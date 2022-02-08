import BotCommandHandler.BotCommand
import IRCCommandWriter.{CapReq, Join, Nick, Pass, Pong}
import akka.actor.{ActorRef, ActorSystem}

import javax.net.ssl.{SSLContext, SSLSocket, SSLSocketFactory}
import java.io.{BufferedReader, InputStreamReader}
import java.net.SocketException

object App {

  implicit val actorSystem: ActorSystem = ActorSystem()

  val socketFactory: SSLSocketFactory = {
    if (Configuration.sslTrustAllCerts) {
      // for debugging purposes only
      val context = SSLContext.getInstance("SSL")
      context.init(null, Array(new TrustAllCerts), new java.security.SecureRandom())
      context.getSocketFactory
    } else {
      SSLSocketFactory.getDefault.asInstanceOf[SSLSocketFactory]
    }
  }

  val socket: SSLSocket = socketFactory
    .createSocket(Configuration.ircServer, Configuration.ircPort)
    .asInstanceOf[SSLSocket]
  socket.startHandshake()

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = {
      actorSystem.terminate()
      socket.close()
      println("Bye!")
    }
  })

  def main(args: Array[String]): Unit = {

    val PINGRegex = """^(PING)( .+)?$""".r

    // badges, username, _, channel, messa
    val PRIVMSGRegex =
      """^(@\S+ )?:(\S+)!(\S+)? PRIVMSG #(\S+) :(.+)$""".r

    val socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream))
    val ircCommandWriter: ActorRef = actorSystem.actorOf(IRCCommandWriter.props(socket))
    val botCommandHandler: ActorRef = actorSystem.actorOf(BotCommandHandler.props(ircCommandWriter))

    Configuration.ircCapabilities.foreach(ircCommandWriter ! CapReq(_))
    ircCommandWriter ! Pass(Configuration.ircToken)
    ircCommandWriter ! Nick(Configuration.ircUsername)
    ircCommandWriter ! Join(Configuration.ircChannel)

    try {
      Iterator
        .iterate(socketReader.readLine())(_ => socketReader.readLine())
        .takeWhile(_ != null)
        .map(_.trim)
        .filter(_.nonEmpty)
        .foreach { line =>
          if (Configuration.debug) println(s"> $line")

          line.trim match {
            case PINGRegex(_*) =>
              ircCommandWriter ! Pong
            case PRIVMSGRegex(_, username, _, channel, message) if message.trim.startsWith("!") =>
              botCommandHandler ! BotCommand(channel, username, message)
            case _ =>
          }
        }
    } catch {
      case e: SocketException =>
        println("Socket exception: " + e.getMessage)
    }

  }

}

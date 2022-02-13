import Client._
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
      println("Bye!")
    }
  })

  def main(args: Array[String]): Unit = {

    val socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream))
    val client: ActorRef = actorSystem.actorOf(Client.props(socket))

    Configuration.ircCapabilities.foreach(capabilities => client ! SendCommand(CapReq(capabilities)))
    client ! SendCommand(Pass(Configuration.ircToken))
    client ! SendCommand(Nick(Configuration.ircUsername))

    try {
      Iterator
        .iterate(socketReader.readLine())(_ => socketReader.readLine())
        .takeWhile(_ != null)
        .map(_.trim)
        .filter(_.nonEmpty)
        .foreach { line =>
          if (Configuration.debug) println(s"> $line")
          client ! Raw(line.trim)
        }
    } catch {
      case e: SocketException =>
        println("Socket exception: " + e.getMessage)
    }

  }

}

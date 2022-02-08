import javax.net.ssl.{SSLContext, SSLSocket, SSLSocketFactory}
import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter, PrintWriter}
import java.net.SocketException

object App {

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

  val reader = new BufferedReader(new InputStreamReader(socket.getInputStream))
  val writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream)))

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = {
      socket.close()
      println("Bye!")
    }
  })

  def main(args: Array[String]): Unit = {

    val PRIVMSGRegex =
      """^(@\S+ )?:(\S+)!(\S+)? PRIVMSG #(\S+) :(.+)$""".r // badges, username, _, channel, message

    capReq(Configuration.ircCapabilities)
    pass(Configuration.ircToken)
    nick(Configuration.ircUsername)
    join(Configuration.ircChannel)

    try {
      Iterator
        .iterate(reader.readLine())(_ => reader.readLine())
        .takeWhile(_ != null)
        .map(_.trim)
        .filter(_.nonEmpty)
        .foreach { line =>
          if (Configuration.debug) println(s"> $line")

          line.trim match {
            case command if command.startsWith("PING") =>
              pong()
            case PRIVMSGRegex(_, username, _, channel, message) if message.trim.equalsIgnoreCase("!hello") =>
              privMsg(channel, s"Hello $username")
            case _ =>
          }
        }
    } catch {
      case e: SocketException =>
        println("Socket exception: " + e.getMessage)
    }

  }

  def pong(): Unit = {
    send("PONG")
  }

  def capReq(capabilities: Option[String]): Unit = {
    capabilities.foreach { cap =>
      send(s"CAP REQ $cap")
    }
  }

  def pass(token: String): Unit = {
    send(s"PASS $token")
  }

  def nick(username: String): Unit = {
    send(s"NICK $username")
  }

  def join(channel: String): Unit = {
    send(s"JOIN #$channel")
  }

  def privMsg(channel: String, message: String): Unit = {
    send(s"PRIVMSG #$channel :$message")
  }

  def send(s: String): Unit = {
    writer.println(s)
    writer.flush()
    if (Configuration.debug) println(s"< $s")
  }
}

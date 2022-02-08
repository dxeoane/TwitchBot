import akka.actor.{Actor, Props}
import java.io.{BufferedWriter, OutputStreamWriter, PrintWriter}
import java.net.Socket

object IRCCommandWriter {
  case object Pong
  case class CapReq(capabilities: String)
  case class Pass(token: String)
  case class Nick(username: String)
  case class Join(channel: String)
  case class PrivateMessage(channel: String, message: String)

  def props(socket: Socket): Props = Props(new IRCCommandWriter(socket))
}

class IRCCommandWriter(val socket: Socket) extends Actor {

  import IRCCommandWriter._

  val writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream)))

  def receive: Receive = {

    case Pong =>
      write("PONG")

    case CapReq(capabilities) =>
      write(s"CAP REQ $capabilities")

    case Pass(token) =>
      write(s"PASS $token")

    case Nick(username) =>
      write(s"NICK $username")

    case Join(channel) =>
      write(s"JOIN #$channel")

    case PrivateMessage(channel, message) =>
      write(s"PRIVMSG #$channel :$message")

    case _ =>
  }

  private def write(message: String): Unit = {
    writer.println(message)
    writer.flush()
    if (Configuration.debug) println(s"< $message")
  }

}

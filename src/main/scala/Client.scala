import CommandHandler.BotCommand
import akka.actor.{Actor, ActorRef, Props}

import java.io.{BufferedWriter, OutputStreamWriter, PrintWriter}
import java.net.Socket

object Client {

  // Client state
  sealed trait ClientState
  case object Disconnected extends ClientState
  case class Connected(username: String) extends ClientState
  case class Joined(channel: String, username: String) extends ClientState

  case object GetClientState

  case class Raw(line: String)

  // Commands
  sealed trait Command
  case object Ping extends Command
  case object Pong extends Command
  case class CapReq(capabilities: String) extends Command
  case class Pass(token: String) extends Command
  case class Nick(username: String) extends Command
  case class Join(channel: String) extends Command
  case class Say(channel: String, message: String) extends Command
  case class Part(channel: String) extends Command

  case class SendCommand(command: Command)

  def props(socket: Socket): Props = Props(new Client(socket))
}

class Client(val socket: Socket) extends Actor {

  import Client._

  private val writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream)))
  private val commandHandler: ActorRef = context.actorOf(CommandHandler.props(self))

  private val PINGRegex = """^(PING)( .+)?$""".r
  // badges, username, _, channel, messa
  private val PRIVMSGRegex =
    """^(@\S+ )?:(\S+)!(\S+)? PRIVMSG #(\S+) :(.+)$""".r
  private val ConnectedRegex =
    """^:tmi.twitch.tv 376 (\S+) :>$""".r
  private val JOINRegex =
    """^(@\S+ )?:(\S+)!(\S+)? JOIN #(\S+)$""".r

  def receive: Receive = behavior(Disconnected)

  def behavior(state: ClientState): Receive = {

    case GetClientState =>
      sender() ! state

    case Raw(PINGRegex(_*)) =>
      sendCommand(Pong)

    case Raw(ConnectedRegex(username)) =>
      println(s"User connected: $username")
      sendCommand(Join(Configuration.ircChannel))
      context.become(behavior(Connected(username)))

    case Raw(PRIVMSGRegex(_, username, _, channel, message)) if message.trim.startsWith("!") =>
      commandHandler ! BotCommand(channel = channel, username = username, message = message)

    case Raw(JOINRegex(_, username, _, channel)) =>
      println(s"User $username joined $channel")
      context.become(behavior(Joined(channel = channel, username = username)))

    case SendCommand(command) =>
      sendCommand(command)

    case _ =>
  }

  private def sendCommand(command: Command): Unit = {
    command match {
      case Ping =>
        write("PING")

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

      case Say(channel, message) =>
        say(channel = channel, message = message)

      case Part(channel) =>
        write(s"PART #$channel")
    }
  }

  private def say(channel: String, message: String): Unit = {
    if (!Configuration.ircReadOnly) {
      write(s"PRIVMSG #$channel :$message")
    } else {
      if (Configuration.debug) println(s"The chat is read only. I can not send: $message")
    }
  }

  private def write(message: String): Unit = {
    writer.println(message)
    writer.flush()
    if (Configuration.debug) println(s"< $message")
  }

}

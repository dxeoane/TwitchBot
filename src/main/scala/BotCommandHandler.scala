import IRCCommandWriter.PrivateMessage
import akka.actor.{Actor, ActorRef, Props}

object BotCommandHandler {
  case class BotCommand(channel: String, username: String, message: String)

  def props(ircCommandWriter: ActorRef): Props = Props(new BotCommandHandler(ircCommandWriter))
}

class BotCommandHandler(ircCommandWriter: ActorRef) extends Actor {

  import BotCommandHandler._

  def receive: Receive = {
    case BotCommand(channel, username, message) if message.equalsIgnoreCase("!Hello") =>
      ircCommandWriter ! PrivateMessage(channel, s"Hello $username")
    case _ =>
  }

}

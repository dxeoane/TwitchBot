import Client.{Say, SendCommand}
import akka.actor.{Actor, ActorRef, Props}

object CommandHandler {
  case class BotCommand(channel: String, username: String, message: String)

  def props(client: ActorRef): Props = Props(new CommandHandler(client))
}

class CommandHandler(client: ActorRef) extends Actor {

  import CommandHandler._

  def receive: Receive = {
    case BotCommand(channel, username, message) if message.equalsIgnoreCase("!Hello") =>
      client ! SendCommand(Say(channel, s"Hello $username"))
    case _ =>
  }

}

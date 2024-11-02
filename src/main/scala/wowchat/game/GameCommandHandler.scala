package wowchat.game

trait GameCommandHandler {

  def sendMessageToWow(tp: Byte, message: String, target: Option[String])
  def sendNotification(message: String)

  def handleWho(arguments: Option[String]): Option[String]
  def handleGmotd(): Option[String]
  def sendGuildInvite(name: String): Unit
  def sendGuildKick(name: String): Unit
  def sendAddIgnore(name: String): Unit
  def sendDelIgnore(name: String): Unit
}

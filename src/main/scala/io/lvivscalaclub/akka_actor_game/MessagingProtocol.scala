package io.lvivscalaclub.akka_actor_game

sealed trait MessagingProtocol

case class NewGame(name: String) extends MessagingProtocol
case class Response(status: Status, error: Option[Error] = None) extends MessagingProtocol
case class Balance(credits: Long) extends MessagingProtocol

sealed trait Status
case object Success extends Status
case object Failure extends Status

sealed trait Error
case object ZeroBalance extends Error
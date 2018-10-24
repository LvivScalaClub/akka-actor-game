package io.lvivscalaclub.akka_actor_game.server

import akka.actor.{Actor, ActorLogging, Props}
import io.lvivscalaclub.akka_actor_game.{Failure, NewGameRequest, NewGameResponse, PlayerAlreadyConnected}

class PlayerSupervisor extends Actor with ActorLogging {

  override def receive = {
    case g@NewGameRequest(name, userId) =>
      log.info(s"New game request: $name, user $userId")
      context.child(userId) match {
        case Some(_) =>
          sender() ! NewGameResponse(Failure, Some(PlayerAlreadyConnected))
        case None =>
          val playerActor = context.actorOf(Props(new Player(userId)), userId)
          playerActor.forward(g)
      }
  }
}

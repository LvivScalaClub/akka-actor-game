package io.lvivscalaclub.akka_actor_game

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}

object Main extends App {

  val actorSystem = ActorSystem("GameActorSystem")
  val props = Props[Supervisor]
  val supervisor = actorSystem.actorOf(props, "Supervisor")
  val propsWebClient = Props(new WebClient(supervisor))
  val webClient = actorSystem.actorOf(propsWebClient, s"web-client-${System.currentTimeMillis()}")

}

class WebClient(supervisor: ActorRef) extends Actor with ActorLogging {
  supervisor ! NewGame("newGame")

  override def receive = {
    case Response(Success, None) => log.info("Response ok!")
    case Response(Failure, Some(err)) => log.info(s"Response fail: $err!")
    case Balance(credits) => log.info(s"Balance $credits")
  }
}

class Supervisor extends Actor with ActorLogging {

  override def receive = {
    case NewGame(name) =>
      log.info(s"New game request: $name")
      val playerActor = context.actorOf(Props[Player], "PlayerActor")
      playerActor.forward(NewGame(name))
  }
}

class Player extends Actor with ActorLogging {

  // TODO: refactor using [[akka.actor.FSM]] when the time will come.
  private var balance: Long = 1

  override def receive = {
    case NewGame(name) =>
      if (balance <= 0) {
        sender ! Response(Failure, Some(ZeroBalance))
        self ! PoisonPill
      }
      else {
        log.info(s"New game request: $name")
        sender ! Response(Success)
        sender ! Balance(balance)
      }
  }
}

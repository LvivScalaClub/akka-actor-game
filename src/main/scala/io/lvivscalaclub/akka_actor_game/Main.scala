package io.lvivscalaclub.akka_actor_game

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}

object Main extends App {

  val actorSystem = ActorSystem("GameActorSystem")
  val props = Props[PlayerSupervisor]
  val supervisor = actorSystem.actorOf(props, "Supervisor")
  createSlotMachine(supervisor, "user1")
  createSlotMachine(supervisor, "user1")

  def createSlotMachine(supervisor: ActorRef, user: String): ActorRef = {
    val propsWebClient = Props(new SlotMachine(supervisor, user))
    actorSystem.actorOf(propsWebClient, s"slot-machine-${System.nanoTime()}")
  }
}

class SlotMachine(supervisor: ActorRef, user: String) extends Actor with ActorLogging {
  supervisor ! NewGameRequest("newGame", user)

  override def receive = {
    case NewGameResponse(Success, None) => log.info("Response ok!")
    case NewGameResponse(Failure, Some(err)) => log.info(s"Response fail: $err!")
    case Balance(credits) => log.info(s"Balance $credits")
  }
}

class PlayerSupervisor extends Actor with ActorLogging {

  override def receive = {
    case g@NewGameRequest(name, userId) =>
      log.info(s"New game request: $name, user $userId")
      context.child(userId) match {
        case Some(_) =>
          sender() ! NewGameResponse(Failure, Some(PlayerAlreadyConnected))
        case None =>
          val playerActor = context.actorOf(Props[Player], userId)
          playerActor.forward(g)
      }

  }
}

class Player extends Actor with ActorLogging {

  // TODO: refactor using [[akka.actor.FSM]] when the time will come.
  private var balance: Long = 1

  val RollState: Receive = {
    case RollRequest(_) =>
      ???
  }

  val InitGameState: Receive = {
    case NewGameRequest(name, _) =>
      if (balance <= 0) {
        sender ! NewGameResponse(Failure, Some(ZeroBalance))
        self ! PoisonPill
      }
      else {
        log.info(s"New game request: $name")
        sender ! NewGameResponse(Success)
        sender ! Balance(balance)
        context.become(RollState)
      }
  }

  override def receive: Receive = InitGameState
}

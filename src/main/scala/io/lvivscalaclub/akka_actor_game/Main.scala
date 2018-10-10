package io.lvivscalaclub.akka_actor_game

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}

import scala.util.Random

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

  private val RollCost = 1

  // TODO: refactor using [[akka.actor.FSM]] when the time will come.
  private var balance: Long = 20

  def takeDoubleRequestState(win: Long, step: Int): Receive = {
    case DoubleRequest(_) =>
      val isWin = Random.nextBoolean()
      if(isWin) {
        sender ! DoubleResponse(win * 2)
        if(step > 1) {
          context.become(takeWinOrGoToDoubleState(win * 2, step - 1))
        } else {
          balance = balance + win * 2
          sender ! Balance(balance)
          context.become(RollState)
        }
      } else {
        sender ! DoubleResponse(0)
        sender ! Balance(balance)
        context.become(RollState)
      }
  }

  def takeWinOrGoToDoubleState(win: Long, step: Int = 5): Receive = {
    case TakeWinRequest =>
      balance = balance + win
      sender ! Balance(balance)
      context.become(RollState)

    case GoToDoubleRequest if step > 0 =>
      context.become(takeDoubleRequestState(win, step))
  }

  val RollState: Receive = {
    case RollRequest(_) =>
      balance = balance - RollCost
      val isWin = Random.nextBoolean()
      val screen = if(isWin) {
        Seq.fill(3){
          val n = Random.nextInt(9)
          Seq.fill(5)(n)
        }
      } else {
        Seq.fill(3)(Seq.fill(5)(Random.nextInt(9)))
      }
      val win = if(isWin) Random.nextInt(20) + 1 else 0
      sender ! RollResponse(screen, win)
      sender ! Balance(balance)

      if(isWin) {
        context.become(takeWinOrGoToDoubleState(win))
      }
  }

  val InitGameState: Receive = {
    case NewGameRequest(name, _) =>
      if (balance <= 0) {
        sender ! NewGameResponse(Failure, Some(ZeroBalance))
        self ! PoisonPill
      } else {
        log.info(s"New game request: $name")
        sender ! NewGameResponse(Success)
        sender ! Balance(balance)
        context.become(RollState)
      }
  }

  override def receive: Receive = InitGameState
}

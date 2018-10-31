package io.lvivscalaclub.akka_actor_game.server

import akka.actor.{Actor, ActorLogging, PoisonPill}
import io.lvivscalaclub.akka_actor_game.{Balance, DoubleRequest, DoubleResponse, Failure, GoToDoubleRequest, IllegalRequest, NewGameRequest, NewGameResponse, RollRequest, RollResponse, Success, TakeWinRequest, ZeroBalance}

import scala.util.Random

class Player(userId: String,
             initBalance: Long = 20,
             randomGenerator: RandomGenerator = DefaultRandomGenerator
            ) extends Actor with ActorLogging {

  private val RollCost = 1

  // TODO: refactor using [[akka.actor.FSM]] when the time will come.
  private var balance: Long = initBalance

  def doubleRequestState(win: Long, step: Int): Receive = {
    case DoubleRequest(_) =>
      val isWin = randomGenerator.win
      if (isWin) {
        sender ! DoubleResponse(win * 2)
        if (step > 1) {
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
      log.info(s"Take win: $win")
      balance = balance + win
      sender ! Balance(balance)
      context.become(RollState)

    case GoToDoubleRequest if step > 0 =>
      context.become(doubleRequestState(win, step))
  }

  val RollState: Receive = {
    case RollRequest =>
      if (balance < RollCost) {
        context.self ! PoisonPill
      } else {
        balance = balance - RollCost
        val isWin = randomGenerator.win
        val screen = if (isWin) {
          Seq.fill(3) {
            val n = Random.nextInt(9)
            Seq.fill(5)(n)
          }
        } else {
          Seq.fill(3)(Seq.fill(5)(Random.nextInt(9)))
        }
        val win = if (isWin) Random.nextInt(20) + 1 else 0
        sender ! RollResponse(screen, win)
        sender ! Balance(balance)

        if (isWin) {
          context.become(takeWinOrGoToDoubleState(win))
        }
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

  override def unhandled(message: Any): Unit = {
    sender() ! IllegalRequest(message.getClass.getSimpleName)
  }
}

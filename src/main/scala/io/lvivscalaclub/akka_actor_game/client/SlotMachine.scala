package io.lvivscalaclub.akka_actor_game.client

import akka.actor.{Actor, ActorLogging, ActorRef}
import io.lvivscalaclub.akka_actor_game.{Balance, DoubleRequest, DoubleResponse, Failure, GoToDoubleRequest, NewGameRequest, NewGameResponse, RollRequest, RollResponse, Success, TakeWinRequest}
import io.lvivscalaclub.akka_actor_game.models.Card

import scala.concurrent.duration._
import scala.util.Random

class SlotMachine(supervisor: ActorRef, user: String) extends Actor with ActorLogging {
  supervisor ! NewGameRequest("newGame", user)

  import scala.concurrent.ExecutionContext.Implicits.global

  override def receive = {
    case NewGameResponse(Success, None) =>
      log.info("Response ok!")

    case RollResponse(screen, win) =>
      log.info(s"Roll response: $win")
      bonusGame(win)

    case NewGameResponse(Failure, Some(err)) =>
      log.info(s"Response fail: $err!")

    case Balance(credits) =>
      log.info(s"Balance $credits")
      context.system.scheduler.scheduleOnce(2.seconds, sender, RollRequest)

    case DoubleResponse(win) =>
      log.info(s"Double Response: $win")
      bonusGame(win)
  }

  private def bonusGame(win: Long) = {
    if (win > 0) {
      val bonusGame = Random.nextBoolean()
      log.info(s"Bonus game: $bonusGame")
      if (bonusGame) {
        sender ! GoToDoubleRequest
        sender ! DoubleRequest(Card.Black)
      } else {
        sender ! TakeWinRequest
      }
    }
  }
}


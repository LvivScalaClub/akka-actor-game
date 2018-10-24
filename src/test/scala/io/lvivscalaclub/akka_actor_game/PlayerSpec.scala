package io.lvivscalaclub.akka_actor_game

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import io.lvivscalaclub.akka_actor_game.server.Player
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike, Matchers}

class PlayerSpec
  extends TestKit(ActorSystem("slot-game"))
    with ImplicitSender
    with FreeSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "PlayerActor" - {
    "Init State" - {
      "init success" in {
        val player = createPlayer()

        player ! NewGameRequest("test-game", "test")

        val expectedInitGameResp = NewGameResponse(status = Success)
        expectMsg(expectedInitGameResp)
        expectMsg(Balance(20))
        expectNoMessage()

        player ! NewGameRequest("test-game", "test")
        expectMsg(IllegalRequest("NewGameRequest"))
        expectNoMessage()
      }

      "init failed" in {
        val player = createPlayer(balance = 0)

        watch(player)

        player ! NewGameRequest("test-game", "test1")

        val expectedInitGameResp = NewGameResponse(status = Failure, error = Some(ZeroBalance))
        expectMsg(expectedInitGameResp)
        expectTerminated(player)
        expectNoMessage()
      }
    }


    "Roll State" - {
      "RollRequest loose" in {

        val gen = new RandomGenerator {
          override def win: Boolean = false
        }

        val player = createPlayer(generator = gen)
        gotoRollState(player)

        player ! RollRequest

        val rollResp = expectMsgType[RollResponse]
        rollResp.win shouldBe 0
        expectMsg(Balance(19))
        expectNoMessage()
      }

      "RollRequest win" in {

        val gen = new RandomGenerator {
          override def win: Boolean = true
        }

        val player = createPlayer(generator = gen)
        gotoRollState(player)

        player ! RollRequest

        val rollResp = expectMsgType[RollResponse]
        (rollResp.win > 0) shouldBe true
        expectMsg(Balance(19))
        expectNoMessage()
        player ! RollRequest
        expectMsgType[IllegalRequest]
        expectNoMessage()
      }
    }

    def createPlayer(balance: Int = 20, generator: RandomGenerator = DefaultRandomGenerator): ActorRef = {
      val playerName = s"test${System.nanoTime()}"
      val props = Props(new Player(playerName, balance, generator))
      system.actorOf(props, playerName)
    }

    def gotoRollState(player: ActorRef): Unit = {
      player ! NewGameRequest("test-game", "test")
      val expectedInitGameResp = NewGameResponse(status = Success)
      expectMsg(expectedInitGameResp)
      expectMsgType[Balance]
      expectNoMessage()
    }
  }
}
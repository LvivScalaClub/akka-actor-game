package io.lvivscalaclub.akka_actor_game

import akka.actor.{ActorRef, ActorSystem, Props}
import io.lvivscalaclub.akka_actor_game.client.SlotMachine
import io.lvivscalaclub.akka_actor_game.server.PlayerSupervisor

import scala.util.Random

object Main extends App {

  val actorSystem = ActorSystem("GameActorSystem")
  val props = Props[PlayerSupervisor]
  val supervisor = actorSystem.actorOf(props, "Supervisor")
  createSlotMachine(supervisor, "user1")
  createSlotMachine(supervisor, "user2")

  def createSlotMachine(supervisor: ActorRef, user: String): ActorRef = {
    val propsWebClient = Props(new SlotMachine(supervisor, user))
    actorSystem.actorOf(propsWebClient, s"slot-machine-${System.nanoTime()}")
  }
}

trait RandomGenerator {
  def win: Boolean
}

object DefaultRandomGenerator extends RandomGenerator {
  override def win: Boolean = Random.nextBoolean()
}

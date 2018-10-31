package io.lvivscalaclub.akka_actor_game.server

import akka.actor.{ActorSystem, Props}

import scala.util.Random

object Server extends App {
  val actorSystem = ActorSystem("GameActorSystem")
  val props = Props[PlayerSupervisor]
  val supervisor = actorSystem.actorOf(props, "Supervisor")
}

trait RandomGenerator {
  def win: Boolean
}

object DefaultRandomGenerator extends RandomGenerator {
  override def win: Boolean = Random.nextBoolean()
}

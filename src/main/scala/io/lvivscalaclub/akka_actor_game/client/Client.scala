package io.lvivscalaclub.akka_actor_game.client

import akka.actor.{ActorRef, ActorSelection, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Client extends App {
  val config = ConfigFactory.load("application_client.conf")

  val actorSystem = ActorSystem("GameActorSystemClient", config)
  val supervisor =
    actorSystem.actorSelection("akka.tcp://GameActorSystem@127.0.0.1:2552/user/Supervisor")

  createSlotMachine(supervisor, "user1")

  def createSlotMachine(supervisor: ActorSelection, user: String): ActorRef = {
    val propsWebClient = Props(new SlotMachine(supervisor, user))
    actorSystem.actorOf(propsWebClient, s"slot-machine-${System.nanoTime()}")
  }
}

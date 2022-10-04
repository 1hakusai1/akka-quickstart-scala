package com.example

import akka.actor.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed

object PrintMyActorRefActor {
    def apply(): Behavior[String] =
        Behaviors.setup(context => new PrintMyActorRefActor(context))
}

class PrintMyActorRefActor(context: ActorContext[String])
    extends AbstractBehavior[String](context) {

    override def onMessage(msg: String): Behavior[String] =
        msg match {
            case "print" =>
                val secondRef =
                    context.spawn(Behaviors.empty[String], "second-actor")
                println(s"Second: $secondRef")
                this
        }

}

object Main {
    def apply(): Behavior[String] =
        Behaviors.setup(context => new Main(context))
}

class Main(context: ActorContext[String])
    extends AbstractBehavior[String](context) {

    override def onMessage(msg: String): Behavior[String] =
        msg match {
            case "start" =>
                val firstRef =
                    context.spawn(PrintMyActorRefActor(), "first-actor")
                println(s"First: $firstRef")
                firstRef ! "print"
                this
        }
}

object ActorHierarchyExperiments extends App {
    val testSystem = typed.ActorSystem(Main(), "testSystem")
    testSystem ! "start"
}

package com.example

import akka.actor.ActorSystem
import akka.actor.TypedActor
import akka.actor.typed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

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
                val supervisingActor =
                    context.spawn(SupervisingActor(), "supervising-actor")
                supervisingActor ! "failChild"
                this
        }
}
object StartStopActor1 {
    def apply(): Behavior[String] =
        Behaviors.setup(context => new StartStopActor1(context))
}

class StartStopActor1(context: ActorContext[String])
    extends AbstractBehavior[String](context) {
    println("first start")
    context.spawn(StartStopActor2(), "second")

    override def onMessage(msg: String): Behavior[String] =
        msg match {
            case "stop" => Behaviors.stopped
        }

    override def onSignal: PartialFunction[typed.Signal, Behavior[String]] = {
        case typed.PostStop =>
            println("first stopped")
            this
    }
}

object StartStopActor2 {
    def apply(): Behavior[String] = Behaviors.setup(new StartStopActor2(_))
}

class StartStopActor2(context: ActorContext[String])
    extends AbstractBehavior[String](context) {
    println("second start")
    override def onMessage(msg: String): Behavior[String] = {
        Behaviors.unhandled
    }

    override def onSignal: PartialFunction[typed.Signal, Behavior[String]] = {
        case typed.PostStop =>
            println("second stopped")
            this
    }

}
object SupervisingActor {
    def apply(): Behavior[String] =
        Behaviors.setup(context => new SupervisingActor(context))
}

class SupervisingActor(context: ActorContext[String])
    extends AbstractBehavior[String](context) {
    private val child = context.spawn(
      Behaviors
          .supervise(SupervisedActor())
          .onFailure(typed.SupervisorStrategy.restart),
      name = "supervised-actor"
    )

    override def onMessage(msg: String): Behavior[String] =
        msg match {
            case "failChild" =>
                child ! "fail"
                this
        }
}

object SupervisedActor {
    def apply(): Behavior[String] =
        Behaviors.setup(context => new SupervisedActor(context))
}
class SupervisedActor(context: ActorContext[String])
    extends AbstractBehavior[String](context) {
    println("supervised actor started")
    override def onMessage(msg: String): Behavior[String] =
        msg match {
            case "fail" =>
                println("supervised actor fails now")
                throw new Exception("I failed!")
        }
    override def onSignal: PartialFunction[typed.Signal, Behavior[String]] = {
        case typed.PreRestart =>
            println("supervised actor will be restarted")
            this
        case typed.PostStop =>
            println("supervised actor stopped")
            this
    }
}

object ActorHierarchyExperiments extends App {
    val testSystem = typed.ActorSystem(Main(), "testSystem")
    testSystem ! "start"
}

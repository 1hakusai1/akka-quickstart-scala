package com.example

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem

object EchoActor {
    final case class EchoRequest(content: String)

    def apply(): Behavior[EchoRequest] = Behaviors.receive {
        (context, message) =>
            println(message.content)
            Behaviors.same
    }
}

object RootActor {
    def apply(): Behavior[String] = Behaviors.setup { context =>
        val echoActor = context.spawn(EchoActor(), "echoActor")
        Behaviors.receiveMessage { message =>
            echoActor ! EchoActor.EchoRequest(message)
            Behaviors.same
        }
    }
}

object AkkaQuickstart extends App {
    val root = ActorSystem(RootActor(), "rootActor")
    root ! "DEBUG"
}

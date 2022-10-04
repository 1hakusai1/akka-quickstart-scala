package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object EchoActor {
    final case class EchoRequest(content: String)

    def apply(wordList: List[String]): Behavior[EchoRequest] =
        Behaviors.receive { (context, message) =>
            val newList = wordList.appended(message.content)
            println(s"current word List: $newList")
            EchoActor(newList)
        }
}

object RootActor {
    def apply(): Behavior[String] = Behaviors.setup { context =>
        val echoActor = context.spawn(EchoActor(List[String]()), "echoActor")
        Behaviors.receiveMessage { message =>
            echoActor ! EchoActor.EchoRequest(message)
            Behaviors.same
        }
    }
}

object AkkaQuickstart extends App {
    val root = ActorSystem(RootActor(), "rootActor")
    root ! "apple"
    root ! "orange"
    root ! "banana"
}

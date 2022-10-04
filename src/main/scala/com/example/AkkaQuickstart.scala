package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object FifoActor {
    trait FifoRequest
    final case class PutRequest(content: String) extends FifoRequest
    final case class PopRequest() extends FifoRequest

    def apply(wordList: List[String]): Behavior[FifoRequest] =
        Behaviors.receive { (context, message) =>
            message match {
                case PutRequest(content) => {
                    val newList = wordList.appended(content)
                    println(s"current word List: $newList")
                    FifoActor(newList)
                }
                case PopRequest() => {
                    if (wordList.isEmpty) {
                        println("EMPTY!!! Can't pop")
                        Behaviors.same
                    } else {
                        val popped :: remain = wordList
                        println(s"popped: $popped")
                        println(s"current word List: $remain")
                        FifoActor(remain)
                    }
                }
                case _ => {
                    println("Not implmented")
                    Behaviors.same
                }
            }
        }
}

object RootActor {
    def apply(): Behavior[FifoActor.FifoRequest] = Behaviors.setup { context =>
        val echoActor = context.spawn(FifoActor(List[String]()), "echoActor")
        Behaviors.receiveMessage { message =>
            echoActor ! message
            Behaviors.same
        }
    }
}

object AkkaQuickstart extends App {
    val root = ActorSystem(RootActor(), "rootActor")
    root ! FifoActor.PutRequest("apple")
    root ! FifoActor.PutRequest("orange")
    root ! FifoActor.PutRequest("banana")
    root ! FifoActor.PopRequest()
    root ! FifoActor.PopRequest()
    root ! FifoActor.PopRequest()
    root ! FifoActor.PopRequest()
    root ! FifoActor.PopRequest()
    root ! FifoActor.PopRequest()
    root ! FifoActor.PutRequest("grape")
}

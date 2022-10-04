package com.example

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.Behavior
import akka.actor.typed.Signal
import akka.actor.typed.PostStop
import akka.actor.typed.scaladsl.Behaviors

object Device {
    def apply(groupId: String, deviceId: String): Behavior[Command] =
        Behaviors.setup(context => new Device(context, groupId, deviceId))

    sealed trait Command
    final case class ReadTemperature(
        requestId: Long,
        replyTo: ActorRef[RespondTemparature]
    ) extends Command
    final case class RespondTemparature(requestId: Long, value: Option[Double])
    final case class RecordTemparature(
        requestId: Long,
        value: Double,
        replyTo: ActorRef[TemparatureRecorded]
    ) extends Command
    final case class TemparatureRecorded(requestId: Long)
}

class Device(
    context: ActorContext[Device.Command],
    groupId: String,
    deviceId: String
) extends AbstractBehavior[Device.Command](context) {
    import Device._

    var lastTemparatureReading: Option[Double] = None
    context.log.info("Device actor {}-{} started", groupId, deviceId)

    override def onMessage(msg: Device.Command): Behavior[Device.Command] = {
        msg match {
            case RecordTemparature(id, value, replyTo) =>
                context.log.info(
                  "Recorded temparature reading {} with {}",
                  value,
                  id
                )
                lastTemparatureReading = Some(value)
                replyTo ! TemparatureRecorded(id)
                this
            case ReadTemperature(id, replyTo) =>
                replyTo ! RespondTemparature(id, lastTemparatureReading)
                this
        }
    }

    override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
        case PostStop =>
            context.log.info("Device actor {}-{} stopped", groupId, deviceId)
            this
    }

}

package com.example

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext

object DeviceManager {
    sealed trait Command

    final case class RequestTrackDevice(
        groupId: String,
        deviceId: String,
        replyTo: ActorRef[DeviceRegisterd]
    ) extends Command
        with DeviceGroup.Command
    final case class DeviceRegisterd(device: ActorRef[Device.Command])
    final case class RequestDeviceList(
        requestId: Long,
        groupId: String,
        replyTo: ActorRef[ReplyDeviceList]
    ) extends Command
        with DeviceGroup.Command
    final case class ReplyDeviceList(requestId: Long, ids: Set[String])
    private final case class DeviceGroupTerminated(groupId: String)
        extends Command

    final case class RequestAllTemparature(
        requestId: Long,
        groupId: String,
        replyTo: ActorRef[RespondAllTemparatures]
    ) extends DeviceGroupQuery.Command
        with DeviceGroup.Command
        with Command
    final case class RespondAllTemparatures(
        requestId: Long,
        temparatures: Map[String, TemparatureReading]
    )
    sealed trait TemparatureReading
    final case class Temparature(value: Double) extends TemparatureReading
    case object TemparatureNotAvailable extends TemparatureReading
    case object DeviceNotAvailable extends TemparatureReading
    case object DeviceTimedOut extends TemparatureReading
}

class DeviceManager(context: ActorContext[DeviceManager.Command])
    extends AbstractBehavior[DeviceManager.Command](context) {
    import DeviceManager._

    var groupIdToActor = Map.empty[String, ActorRef[DeviceGroup.Command]]
    context.log.info("DeviceManager started")

    override def onMessage(
        msg: DeviceManager.Command
    ): Behavior[DeviceManager.Command] =
        msg match {
            case tracMsg @ RequestTrackDevice(groupId, _, replyTo) =>
                groupIdToActor.get(groupId) match {
                    case Some(ref) =>
                        ref ! tracMsg
                    case None =>
                        context.log.info(
                          "Creating device group actor for {}",
                          groupId
                        )
                        val groupActor = context.spawn(
                          DeviceGroup(groupId),
                          s"group-$groupId"
                        )
                        context.watchWith(
                          groupActor,
                          DeviceGroupTerminated(groupId)
                        )
                        groupActor ! tracMsg
                        groupIdToActor += groupId -> groupActor
                }
                this
            case req @ RequestDeviceList(requestId, groupId, replyTo) =>
                groupIdToActor.get(groupId) match {
                    case Some(ref) =>
                        ref ! req
                    case None =>
                        replyTo ! ReplyDeviceList(requestId, Set.empty)
                }
                this
            case DeviceGroupTerminated(groupId) =>
                context.log.info(
                  "Device group actor for {} has been terminated",
                  groupId
                )
                groupIdToActor -= groupId
                this
        }
    override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
        case PostStop =>
            context.log.info("DeviceManager stopped")
            this
    }
}

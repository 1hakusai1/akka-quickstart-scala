package com.example

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.Behavior
import akka.actor.typed.Signal
import akka.actor.typed.PostStop
import akka.actor.typed.scaladsl.Behaviors

object DeviceGroup {

    def apply(groupId: String): Behavior[Command] =
        Behaviors.setup(context => new DeviceGroup(context, groupId))

    trait Command
    private final case class DeviceTerminated(
        device: ActorRef[Device.Command],
        groupId: String,
        deviceId: String
    ) extends Command
}

class DeviceGroup(
    context: ActorContext[DeviceGroup.Command],
    groupId: String
) extends AbstractBehavior[DeviceGroup.Command](context) {
    import DeviceGroup._
    import DeviceManager.{
        DeviceRegisterd,
        ReplyDeviceList,
        RequestDeviceList,
        RequestTrackDevice
    }

    private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]

    context.log.info("Device group {} started", groupId)

    override def onMessage(
        msg: DeviceGroup.Command
    ): Behavior[DeviceGroup.Command] =
        msg match {
            case RequestTrackDevice(`groupId`, deviceId, replyTo) =>
                deviceIdToActor.get(deviceId) match {
                    case Some(deviceActor) =>
                        replyTo ! DeviceRegisterd(deviceActor)
                    case None =>
                        context.log.info(
                          "Creating device actor for {}",
                          deviceId
                        )
                        val deviceActor = context.spawn(
                          Device(groupId, deviceId),
                          s"device-$deviceId"
                        )
                        deviceIdToActor += deviceId -> deviceActor
                        replyTo ! DeviceRegisterd(deviceActor)
                }
                this
            case RequestTrackDevice(gid, _, _) => {
                context.log.warn(
                  "Ignoring TrackDevice request for {}. This actor is resposible for {}",
                  gid,
                  groupId
                )
                this
            }
        }

    override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
        case PostStop =>
            context.log.info("DeviceGroup {} stopped", groupId)
            this
    }
}

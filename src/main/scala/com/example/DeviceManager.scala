package com.example

import akka.actor.typed.ActorRef

object DeviceManager {
    sealed trait Command

    final case class RequestTrackDevice(
        groupId: String,
        deviceId: String,
        replyTo: ActorRef[DeviceRegisterd]
    ) extends Command
        with DeviceGroup.Command
    final case class DeviceRegisterd(device: ActorRef[Device.Command])
    final case class RequestDeviceList()
    final case class ReplyDeviceList()
}

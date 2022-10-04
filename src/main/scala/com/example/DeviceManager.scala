package com.example

import akka.actor.typed.ActorRef

object DeviceManager {
    sealed trait Command

    final case class RequestTrackDevice(
        groupId: String,
        deviceId: String,
        replyTo: ActorRef[DeviceRegisterd]
    ) extends Command
    final case class DeviceRegisterd(device: ActorRef[Device.Command])
}

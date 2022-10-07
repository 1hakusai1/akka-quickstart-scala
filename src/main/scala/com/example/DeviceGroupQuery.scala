package com.example

import akka.actor.typed.ActorRef
import scala.concurrent.duration.FiniteDuration
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.TimerScheduler
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object DeviceGroupQuery {

    def apply(
        deviceIdToActor: Map[String, ActorRef[Device.Command]],
        requestId: Long,
        requester: ActorRef[DeviceManager.RespondAllTemparatures],
        timeout: FiniteDuration
    ): Behavior[Command] = {
        Behaviors.setup { context =>
            Behaviors.withTimers { timers =>
                new DeviceGroupQuery(
                  deviceIdToActor,
                  requestId,
                  requester,
                  timeout,
                  context,
                  timers
                )
            }
        }
    }

    trait Command
    private case object CollectionTimeout extends Command
    final case class WrappedRespondTemparature(
        response: Device.RespondTemparature
    ) extends Command
    private final case class DeviceTerminated(deviceId: String) extends Command
}

class DeviceGroupQuery(
    deviceIdToActor: Map[String, ActorRef[Device.Command]],
    requestId: Long,
    requester: ActorRef[DeviceManager.RespondAllTemparatures],
    timeout: FiniteDuration,
    context: ActorContext[DeviceGroupQuery.Command],
    timers: TimerScheduler[DeviceGroupQuery.Command]
) extends AbstractBehavior[DeviceGroupQuery.Command](context) {
    import DeviceGroupQuery._
    import DeviceManager.DeviceNotAvailable
    import DeviceManager.DeviceTimedOut
    import DeviceManager.RespondAllTemparatures
    import DeviceManager.Temparature
    import DeviceManager.TemparatureNotAvailable
    import DeviceManager.TemparatureReading

    timers.startSingleTimer(CollectionTimeout, CollectionTimeout, timeout)

    private val respondTemparatureAdapter =
        context.messageAdapter(WrappedRespondTemparature.apply)

    deviceIdToActor.foreach { case (deviceId, device) =>
        context.watchWith(device, DeviceTerminated(deviceId))
        device ! Device.ReadTemperature(0, respondTemparatureAdapter)
    }

    private var repliesSoFar = Map.empty[String, TemparatureReading]
    private var stillWating = deviceIdToActor.keySet

    override def onMessage(
        msg: Command
    ): Behavior[Command] = ???

    private def onRespondTemparature(
        response: Device.RespondTemparature
    ): Behavior[Command] = {
        val reading = response.value match {
            case Some(value) => Temparature(value)
            case None        => TemparatureNotAvailable
        }
        val deviceId = response.deviceId
        repliesSoFar += (deviceId -> reading)
        stillWating -= deviceId

        respondWhenAllCollected()
    }

    private def onDeviceTerminated(deviceId: String): Behavior[Command] = {
        if (stillWating(deviceId)) {
            repliesSoFar += (deviceId -> DeviceNotAvailable)
            stillWating -= deviceId
        }
        respondWhenAllCollected()
    }

    private def onCollectionTimeout(): Behavior[Command] = {
        repliesSoFar ++= stillWating.map(deviceId => deviceId -> DeviceTimedOut)
        stillWating = Set.empty
        respondWhenAllCollected()
    }

    private def respondWhenAllCollected(): Behavior[Command] = {
        if (stillWating.isEmpty) {
            requester ! RespondAllTemparatures(requestId, repliesSoFar)
            Behaviors.stopped
        } else {
            this
        }
    }
}

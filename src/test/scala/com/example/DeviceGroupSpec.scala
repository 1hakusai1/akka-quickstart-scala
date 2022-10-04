package com.example

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class DeviceGroupSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
    import DeviceManager._
    import Device._

    "DeviceGroup actor" must {
        "be able to register a device actor" in {
            val probe = createTestProbe[DeviceRegisterd]()
            val groupActor = spawn(DeviceGroup("group"))

            groupActor ! RequestTrackDevice("group", "device1", probe.ref)
            val registered1 = probe.receiveMessage()
            val deviceActor1 = registered1.device

            groupActor ! RequestTrackDevice("group", "device2", probe.ref)
            val registered2 = probe.receiveMessage()
            val deviceActor2 = registered2.device
            deviceActor1 should !==(deviceActor2)

            val recordProbe = createTestProbe[TemparatureRecorded]()
            deviceActor1 ! RecordTemparature(0, 1.0, recordProbe.ref)
            recordProbe.expectMessage(TemparatureRecorded(0))
            deviceActor2 ! RecordTemparature(1, 2.0, recordProbe.ref)
            recordProbe.expectMessage(TemparatureRecorded(1))
        }
        "return sam actor for same deviceId" in {
            val probe = createTestProbe[DeviceRegisterd]()
            val groupActor = spawn(DeviceGroup("group"))
            groupActor ! RequestTrackDevice("group", "device1", probe.ref)
            val deviceActor1 = probe.receiveMessage().device
            groupActor ! RequestTrackDevice("group", "device1", probe.ref)
            val deviceActor2 = probe.receiveMessage().device

            deviceActor1 should ===(deviceActor2)
        }
        "ignore requests for wrong groupId" in {
            val probe = createTestProbe[DeviceRegisterd]()
            val groupActor = spawn(DeviceGroup("group"))
            groupActor ! RequestTrackDevice("wrongGroup", "device1", probe.ref)
            probe.expectNoMessage()
        }
        "be able to list active devices" in {
            val registerdProbe = createTestProbe[DeviceRegisterd]()
            val groupActor = spawn(DeviceGroup("group"))

            groupActor ! RequestTrackDevice(
              "group",
              "device1",
              registerdProbe.ref
            )
            registerdProbe.receiveMessage()
            groupActor ! RequestTrackDevice(
              "group",
              "device2",
              registerdProbe.ref
            )
            registerdProbe.receiveMessage()

            val deviceListProbe = createTestProbe[ReplyDeviceList]()
            groupActor ! RequestDeviceList(0, "group", deviceListProbe.ref)
            deviceListProbe.expectMessage(
              ReplyDeviceList(0, Set("device1", "device2"))
            )
        }
        "be able to list active devices after one shuts down" in {
            val registerdProbe = createTestProbe[DeviceRegisterd]()
            val groupActor = spawn(DeviceGroup("group"))

            groupActor ! RequestTrackDevice(
              "group",
              "device1",
              registerdProbe.ref
            )
            val toShutDown = registerdProbe.receiveMessage().device
            groupActor ! RequestTrackDevice(
              "group",
              "device2",
              registerdProbe.ref
            )
            registerdProbe.receiveMessage()

            val deviceListProbe = createTestProbe[ReplyDeviceList]()
            groupActor ! RequestDeviceList(0, "group", deviceListProbe.ref)
            deviceListProbe.expectMessage(
              ReplyDeviceList(0, Set("device1", "device2"))
            )

            toShutDown ! Passivate
            registerdProbe.expectTerminated(
              toShutDown,
              registerdProbe.remainingOrDefault
            )
            registerdProbe.awaitAssert {
                groupActor ! RequestDeviceList(1, "group", deviceListProbe.ref)
                deviceListProbe.expectMessage(
                  ReplyDeviceList(1, Set("device2"))
                )
            }
        }
    }
}

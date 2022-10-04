package com.example

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class DeviceSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
    import Device._

    "Device actor" must {
        "reply with empty reading if no temparature is known" in {
            val probe = createTestProbe[RespondTemparature]()
            val deviceActor = spawn(Device("group", "device"))

            deviceActor ! ReadTemperature(42, probe.ref)
            val response = probe.receiveMessage()
            response.requestId should ===(42)
            response.value should ===(None)
        }
        "reply with latest temparature reading" in {
            val recordProbe = createTestProbe[TemparatureRecorded]()
            val readProbe = createTestProbe[RespondTemparature]()
            val deviceActor = spawn(Device("group", "device"))

            deviceActor ! RecordTemparature(1, 24.0, recordProbe.ref)
            recordProbe.expectMessage(TemparatureRecorded(1))

            deviceActor ! ReadTemperature(2, readProbe.ref)
            val response1 = readProbe.receiveMessage()
            response1.requestId should ===(2)
            response1.value should ===(Some(24.0))

            deviceActor ! RecordTemparature(3, 55.0, recordProbe.ref)
            recordProbe.expectMessage(TemparatureRecorded(3))

            deviceActor ! ReadTemperature(4, readProbe.ref)
            val response2 = readProbe.receiveMessage()
            response2.requestId should ===(4)
            response2.value should ===(Some(55.0))
        }
    }
}

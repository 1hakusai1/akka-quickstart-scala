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
    }
}

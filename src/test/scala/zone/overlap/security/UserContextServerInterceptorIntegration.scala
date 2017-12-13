// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.security

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import zone.overlap.docker.DockerDexService

class UserContextServerInterceptorIntegration
    extends WordSpec
    with Matchers
    with DockerTestKit
    with DockerKitSpotify
    with DockerDexService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "dex node" when {
    "started" should {
      "be ready with log line checker" in {
        isContainerReady(dexContainer).futureValue shouldBe true
        dexContainer.getPorts().futureValue.get(5556) should not be empty
        dexContainer.getPorts().futureValue.get(5557) should not be empty
        dexContainer.getIpAddresses().futureValue should not be Seq.empty
      }
    }
  }
}

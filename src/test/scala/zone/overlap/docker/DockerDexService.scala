// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.docker

import com.spotify.docker.client.messages.PortBinding
import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import com.whisk.docker.testkit.{ContainerSpec, DockerReadyChecker, ManagedContainers}
import org.scalatest.Suite

import scala.concurrent.duration._

/*
 * Provides a Dex Docker container for integration tests
 */
trait DockerDexService extends DockerTestKitForAll {
  self: Suite =>

  val dexHttpPort = 5556
  val dexGrpcPort = 5557

  lazy val dexContainer = ContainerSpec("asia.gcr.io/zone-overlap/dex-integration:latest")
    .withPortBindings(dexHttpPort -> PortBinding.of("0.0.0.0", dexHttpPort),
                      dexGrpcPort -> PortBinding.of("0.0.0.0", dexGrpcPort))
    .withReadyChecker(
      DockerReadyChecker
        .HttpResponseCode(dexHttpPort, "/dex/.well-known/openid-configuration")
        .within(100 millis)
        .looped(20, 2000 millis)
    )
    .withReadyChecker(DockerReadyChecker.LogLineContains("listening (grpc)"))

  override val managedContainers: ManagedContainers = dexContainer.toManagedContainer
}

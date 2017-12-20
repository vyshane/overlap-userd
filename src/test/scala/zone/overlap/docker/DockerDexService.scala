// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.docker

import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

import scala.concurrent.duration._

/*
 * Provides a Dex Docker container for integration tests
 */
trait DockerDexService extends DockerKit {

  val dexHttpPort = 5556
  val dexGrpcPort = 5557

  val dexContainer = DockerContainer("asia.gcr.io/zone-overlap/dex-integration:latest")
    .withPorts(dexHttpPort -> None, dexGrpcPort -> None)
    .withReadyChecker(
      DockerReadyChecker
        .HttpResponseCode(dexHttpPort, "/dex/.well-known/openid-configuration")
        .within(100 millis)
        .looped(20, 2000 millis)
    )
    .withReadyChecker(DockerReadyChecker.LogLineContains("listening (grpc)"))

  abstract override def dockerContainers = dexContainer :: super.dockerContainers
}
// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.docker

import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

/*
 * Provides a Dex Docker container for integration tests
 */
trait DockerDexService extends DockerKit {

  val dexContainer = DockerContainer("asia.gcr.io/zone-overlap/dex-integration:latest")
    .withPorts(5556 -> None, 5557 -> None)
    .withReadyChecker(DockerReadyChecker.LogLineContains("listening (grpc)"))

  abstract override def dockerContainers = dexContainer :: super.dockerContainers
}
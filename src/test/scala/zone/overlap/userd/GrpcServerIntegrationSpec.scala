// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd

import java.util.UUID

import com.github.javafaker.Faker
import io.getquill._
import io.grpc.{Status, StatusRuntimeException}
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.util.MutableHandlerRegistry
import monix.eval.Task
import org.scalatest._
import zone.overlap.privateapi.{FindUserByIdRequest, UserGrpcMonix => PrivateUserGrpcMonix}
import zone.overlap.userd.endpoints.privateapi.PrivateUserService
import zone.overlap.userd.persistence.{Decoders, Encoders, Quotes, UserRepository}

// Exercise the gRPC server (in-process)
class GrpcServerIntegrationSpec extends AsyncWordSpec with Matchers with RecoverMethods {

  "The userd gRPC server" when {
    "a request is sent to one of its endpoints" should {
      "send a respond back" in {

        val faker = new Faker()
        val serverName = s"userd-test-server-${UUID.randomUUID().toString}"
        val serviceRegistry = new MutableHandlerRegistry()

        lazy val databaseContext = new H2JdbcContext(SnakeCase, "database") with Encoders with Decoders with Quotes
        lazy val userRepository = UserRepository(databaseContext)
        serviceRegistry.addService(
          PrivateUserGrpcMonix.bindService(new PrivateUserService(userRepository), monix.execution.Scheduler.global))

        val server = InProcessServerBuilder
          .forName(serverName)
          .fallbackHandlerRegistry(serviceRegistry)
          .directExecutor()
          .build()
          .start()

        val channel = InProcessChannelBuilder
          .forName(serverName)
          .directExecutor()
          .build()

        import monix.execution.Scheduler.Implicits.global
        val privateUserService = PrivateUserGrpcMonix.stub(channel)

        recoverToExceptionIf[StatusRuntimeException] {
          privateUserService
            .findUserById(FindUserByIdRequest(""))
            .doOnFinish(_ => {
              channel.shutdown()
              server.shutdownNow()
              Task(())
            })
            .runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INVALID_ARGUMENT.getCode
        }
      }
    }
  }
}

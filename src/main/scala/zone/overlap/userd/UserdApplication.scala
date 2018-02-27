// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd

import java.util.concurrent.TimeUnit

import com.coreos.dex.api.ApiGrpcMonix
import com.typesafe.config.ConfigFactory
import io.getquill.{PostgresJdbcContext, SnakeCase}
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.{ManagedChannelBuilder, ServerInterceptors}
import io.prometheus.client.exporter.HTTPServer
import mu.node.grpc.oidc.IdTokenInterceptor
import mu.node.healthttpd.Healthttpd
import org.slf4j.LoggerFactory
import zone.overlap.api.UserGrpcMonix
import zone.overlap.internalapi.EmailDeliveryGrpcMonix
import zone.overlap.privateapi.{UserGrpcMonix => PrivateUserGrpcMonix}
import zone.overlap.userd.events.EventPublisher
import zone.overlap.userd.persistence._
import zone.overlap.userd.authentication.AuthenticationContext
import zone.overlap.userd.endpoints.api.PublicUserService
import zone.overlap.userd.endpoints.privateapi.PrivateUserService

import scala.util.Try

/*
 * Main entrypoint for our application
 */
object UserdApplication {

  private val log = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    // Start status HTTP endpoints
    val healthttpd = Healthttpd(config.getInt("status.port")).startAndIndicateNotReady()

    // Start serving Prometheus metrics via HTTP
    new HTTPServer(config.getInt("metrics.port"))
    log.info(s"Serving metrics via HTTP on port ${config.getString("metrics.port")}")

    // Setup database access
    if (config.getBoolean("autoMigrateDatabaseOnLaunch")) DatabaseMigrator(config).migrate()
    lazy val userRepository = {
      val databaseContext = new PostgresJdbcContext(SnakeCase, "database") with Encoders with Decoders with Quotes
      UserRepository(databaseContext)
    }

    // Kafka
    lazy val eventPublisher = EventPublisher(config)

    // Public user service
    lazy val dexStub = {
      val channel = ManagedChannelBuilder
        .forAddress(config.getString("dex.host"), config.getInt("dex.port"))
        .usePlaintext(true)
        .build()
      ApiGrpcMonix.stub(channel).withDeadlineAfter(5, TimeUnit.SECONDS)
    }
    lazy val emailDeliveryStub = {
      val channel = ManagedChannelBuilder
        .forAddress(config.getString("emaild.host"), config.getInt("emaild.port"))
        .usePlaintext(true)
        .build()
      EmailDeliveryGrpcMonix.stub(channel).withDeadlineAfter(5, TimeUnit.SECONDS)
    }
    lazy val publicUserService = UserGrpcMonix.bindService(
      new PublicUserService(userRepository, dexStub, emailDeliveryStub, eventPublisher),
      monix.execution.Scheduler.global
    )

    // Private user service
    lazy val privateUserService = PrivateUserGrpcMonix.bindService(
      new PrivateUserService(userRepository),
      monix.execution.Scheduler.global
    )

    // Start gRPC server
    val idTokenInterceptor = IdTokenInterceptor(AuthenticationContext(_), config)
    val grpcServer = NettyServerBuilder
      .forPort(config.getInt("grpc.port"))
      .addService(ServerInterceptors.intercept(publicUserService, idTokenInterceptor))
      .addService(ServerInterceptors.intercept(privateUserService, idTokenInterceptor))
      .build()
      .start()

    // userd is up and we are ready to serve requests
    healthttpd
      .setHealthCheck(() => {
        Try(userRepository.canQueryUsers() && eventPublisher.canQueryTopicPartitions())
          .getOrElse(false)
      })
      .indicateReady()

    grpcServer.awaitTermination()

    sys.ShutdownHookThread {
      grpcServer.shutdown()
      healthttpd.stop()
    }
  }
}

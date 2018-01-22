// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd

import com.coreos.dex.api.api.ApiGrpcMonix
import com.typesafe.config.ConfigFactory
import io.getquill.{PostgresJdbcContext, SnakeCase}
import io.grpc.netty.NettyServerBuilder
import io.grpc.{ManagedChannelBuilder, ServerInterceptors}
import io.prometheus.client.exporter.HTTPServer
import zone.overlap.api.user.UserGrpcMonix
import zone.overlap.privateapi.user.{UserGrpcMonix => PrivateUserGrpcMonix}
import zone.overlap.userd.events.EventPublisher
import zone.overlap.userd.persistence._
import zone.overlap.userd.authentication.UserContextServerInterceptor
import zone.overlap.userd.endpoints.api.PublicUserService
import zone.overlap.userd.endpoints.privateapi.PrivateUserService
import zone.overlap.userd.monitoring.StatusServer

/*
 * Main entrypoint for our application
 */
object UserdApplication {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    // Start status HTTP endpoints
    val statusServer = StatusServer(config.getInt("status.port")).startAndIndicateNotReady()

    // Start serving Prometheus metrics via HTTP
    new HTTPServer(config.getInt("metrics.port"))

    // Setup database access
    if (config.getString("autoMigrateDatabaseOnLaunch").toLowerCase() == "yes")
      DatabaseMigrator(config).migrate()
    lazy val databaseContext = new PostgresJdbcContext(SnakeCase, "database") with Encoders with Decoders with Quotes
    lazy val userRepository = UserRepository(databaseContext)

    // Kafka
    lazy val eventPublisher = EventPublisher(config)

    // User authentication
    lazy val userContextServerInterceptor = UserContextServerInterceptor(config)

    // Public user service
    lazy val channel = ManagedChannelBuilder
      .forAddress(config.getString("dex.host"), config.getInt("dex.port"))
      .usePlaintext(true)
      .build()
    lazy val dexStub = ApiGrpcMonix.stub(channel)
    lazy val publicUserService = UserGrpcMonix.bindService(
      PublicUserService(userRepository, dexStub, eventPublisher),
      monix.execution.Scheduler.global
    )

    // Private user service
    lazy val privateUserService = PrivateUserGrpcMonix.bindService(
      PrivateUserService(userRepository),
      monix.execution.Scheduler.global
    )

    // Start gRPC server
    val grpcServer = NettyServerBuilder
      .forPort(config.getInt("grpc.port"))
      .addService(ServerInterceptors.intercept(publicUserService, userContextServerInterceptor))
      .addService(ServerInterceptors.intercept(privateUserService, userContextServerInterceptor))
      .build()
      .start()

    // gRPC server is up and we are ready to serve requests
    statusServer.indicateReady()

    statusServer.healthChecker = () => {
      userRepository.canQueryUsers() && eventPublisher.canQueryTopicPartitions()
    }

    grpcServer.awaitTermination()
  }
}

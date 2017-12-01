// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd

import com.coreos.dex.api.api.ApiGrpcMonix
import com.typesafe.config.ConfigFactory
import io.getquill.{PostgresJdbcContext, SnakeCase}
import io.grpc.netty.NettyServerBuilder
import io.grpc.{ManagedChannelBuilder, ServerInterceptors}
import zone.overlap.api.PublicUserService
import zone.overlap.api.user.UserGrpcMonix
import zone.overlap.privateapi.user.{UserGrpcMonix => PrivateUserGrpcMonix}
import zone.overlap.privateapi.PrivateUserService
import zone.overlap.userd.events.EventPublisher
import zone.overlap.userd.persistence._
import zone.overlap.userd.security.UserContextServerInterceptor

// Main entrypoint for our application
object UserdApplication {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    // Database access
    if (config.getString("autoMigrateDatabaseOnLaunch").toLowerCase() == "yes")
      DatabaseMigrator(config).migrate()
    lazy val context = new PostgresJdbcContext(SnakeCase, "database") with Encoders with Decoders with Quotes
    lazy val userRepository = UserRepository(context)

    // Kafka
    lazy val eventPublisher = EventPublisher(config)

    // User authentication
    lazy val userContextServerInterceptor = new UserContextServerInterceptor(config)

    // Public user service
    lazy val channel = ManagedChannelBuilder
      .forAddress(config.getString("dexHost"), config.getInt("dexPort"))
      .usePlaintext(true)
      .build()
    lazy val dexStub = ApiGrpcMonix.stub(channel)
    lazy val publicUserService = UserGrpcMonix.bindService(
      new PublicUserService(userRepository, dexStub, eventPublisher),
      monix.execution.Scheduler.global
    )

    // Private user service
    lazy val privateUserService = PrivateUserGrpcMonix.bindService(
      new PrivateUserService(userRepository),
      monix.execution.Scheduler.global
    )

    // Start gRPC server and block
    NettyServerBuilder
      .forPort(config.getInt("grpcServer.port"))
      .addService(ServerInterceptors.intercept(publicUserService, userContextServerInterceptor))
      .addService(ServerInterceptors.intercept(privateUserService, userContextServerInterceptor))
      .build()
      .start()
      .awaitTermination()
  }
}

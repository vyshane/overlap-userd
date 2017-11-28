// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd

import api.api.ApiGrpcMonix
import com.typesafe.config.ConfigFactory
import io.getquill.{PostgresJdbcContext, SnakeCase}
import io.grpc.netty.NettyServerBuilder
import io.grpc.ManagedChannelBuilder
import zone.overlap.api.PublicUserService
import zone.overlap.api.user.UserGrpcMonix
import zone.overlap.privateapi.user.{UserGrpcMonix => PrivateUserGrpcMonix}
import zone.overlap.privateapi.PrivateUserService
import zone.overlap.userd.events.EventPublisher
import zone.overlap.userd.persistence._

// Main entrypoint for our application
object UserdApplication {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    if (config.getString("autoMigrateDatabaseOnLaunch").toLowerCase() == "yes")
      DatabaseMigrator(config).migrate()

    lazy val context = new PostgresJdbcContext(SnakeCase, "database") with Encoders with Decoders with Quotes
    lazy val userRepository = UserRepository(context)

    lazy val eventPublisher = EventPublisher(config)

    lazy val channel = ManagedChannelBuilder
      .forAddress(config.getString("dexHost"), config.getInt("dexPort"))
      .usePlaintext(true)
      .build()

    lazy val dexStub = ApiGrpcMonix.stub(channel)

    NettyServerBuilder
      .forPort(config.getInt("grpcServer.port"))
      .addService(
        UserGrpcMonix.bindService(
          new PublicUserService(userRepository, dexStub, eventPublisher),
          monix.execution.Scheduler.global
        )
      )
      .addService(
        PrivateUserGrpcMonix.bindService(
          new PrivateUserService(userRepository),
          monix.execution.Scheduler.global
        )
      )
      .build()
      .start()
      .awaitTermination()
  }
}

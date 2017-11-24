// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd

import com.typesafe.config.ConfigFactory
import io.getquill.{PostgresJdbcContext, SnakeCase}
import io.grpc.ServerBuilder
import zone.overlap.api.private_.{UserService => PrivateUserService}
import zone.overlap.api.private_.user.{UserGrpcMonix => PrivateUserGrpcMonix}
import zone.overlap.userd.persistence._

// Main entrypoint for our application
object UserdApplication {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    if (config.getString("autoMigrateDatabaseOnLaunch").toLowerCase() == "yes")
      DatabaseMigrator(config).migrate()

    lazy val context = new PostgresJdbcContext(SnakeCase, "database") with Encoders with Decoders with Quotes
    lazy val userRepository = UserRepository(context)

    ServerBuilder
      .forPort(config.getInt("grpcServer.port"))
      .addService(
        PrivateUserGrpcMonix.bindService(new PrivateUserService(userRepository), monix.execution.Scheduler.global)
      )
      .build()
      .start()
      .awaitTermination()
  }
}

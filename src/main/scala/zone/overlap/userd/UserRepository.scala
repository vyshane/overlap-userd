// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd

import java.time.Instant

import io.getquill.context.jdbc.JdbcContext
import io.getquill.{H2JdbcContext, PostgresJdbcContext, SnakeCase}

case class UserRecord(
  id: String,
  firstName: String,
  lastName: String,
  email: String,
  passwordHash: String,
  created: Instant
)

case class UserRepository(context: JdbcContext[_, _]) {
  import context._

  val users = quote {
    querySchema[UserRecord]("users")
  }

  def findById(id: String) = ???
}

trait UserModule {
  lazy val postgresUserRepo = UserRepository(new PostgresJdbcContext(SnakeCase, "database"))
  lazy val h2UserRepo = UserRepository(new H2JdbcContext(SnakeCase, "database"))
}

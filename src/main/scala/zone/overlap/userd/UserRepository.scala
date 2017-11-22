// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd

import java.time.Instant

import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill._

case class UserRecord(
  id: String,
  firstName: String,
  lastName: String,
  email: String,
  passwordHash: String,
  created: Instant
)

case class UserRepository[Dialect <: SqlIdiom, Naming <: NamingStrategy](context: JdbcContext[Dialect, Naming]) {
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

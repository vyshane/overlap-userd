// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.persistence

import java.time.Instant

import io.getquill._
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

case class UserRecord(
  id: String,
  firstName: String,
  lastName: String,
  email: String,
  passwordHash: String,
  signedUp: Instant
)

case class UserRepository[Dialect <: SqlIdiom, Naming <: NamingStrategy]
                         (context: JdbcContext[Dialect, Naming] with Encoders with Decoders with Quotes) {
  import context._

  val users = quote {
    querySchema[UserRecord]("users")
  }

  def findUserById(id: String): Option[UserRecord] = {
    val q = quote {
      users.filter(_.id == lift(id))
    }
    context.run(q).headOption
  }
}


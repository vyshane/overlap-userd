// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.persistence

import java.time.Instant
import java.util.UUID

import com.google.protobuf.timestamp.Timestamp
import io.getquill._
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom
import zone.overlap.privateapi.user.{User, UserStatus}
import zone.overlap.api.user.{SignUpRequest, UpdateInfoRequest}

case class UserRecord(id: String,
                      firstName: String,
                      lastName: String,
                      email: String,
                      status: UserStatus,
                      signedUp: Instant) {

  def toProtobuf = User(
    id,
    firstName,
    lastName,
    email,
    status,
    Option(Timestamp(signedUp.getEpochSecond, signedUp.getNano))
  )
}

case class UserRepository[Dialect <: SqlIdiom, Naming <: NamingStrategy](
    context: JdbcContext[Dialect, Naming] with Encoders with Decoders with Quotes) {

  import context._
  implicit val userStatusEncoder = MappedEncoding[UserStatus, String](us => us.name)
  implicit val userStatusDecoder = MappedEncoding[String, UserStatus](s => UserStatus.fromName(s).get)

  private val users = quote {
    querySchema[UserRecord]("users")
  }

  def findUserById(id: String): Option[UserRecord] = {
    val q = quote {
      users.filter(_.id == lift(id))
    }
    context.run(q).headOption
  }

  def findUserByEmail(email: String): Option[UserRecord] = {
    val q = quote {
      users.filter(_.email == lift(email))
    }
    context.run(q).headOption
  }

  def createUser(signUpRequest: SignUpRequest): String = {
    val userId = UUID.randomUUID().toString
    createUser(
      UserRecord(
        userId,
        signUpRequest.firstName,
        signUpRequest.lastName,
        signUpRequest.email,
        UserStatus.PENDING_EMAIL_VERIFICATION,
        Instant.now()
      )
    )
    userId
  }

  def createUser(userRecord: UserRecord): Unit = {
    val q = quote {
      users.insert(lift(userRecord))
    }
    context.run(q)
  }

  def updateUser(updateInfoRequest: UpdateInfoRequest): Unit = {
    // TODO
  }
}

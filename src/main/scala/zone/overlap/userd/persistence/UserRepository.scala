// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.persistence

import java.time.Instant
import java.util.UUID

import com.google.protobuf.timestamp.Timestamp
import io.getquill._
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom
import monix.eval.Task
import zone.overlap.privateapi.user.{User, UserStatus}
import zone.overlap.api.user.{SignUpRequest, UpdateInfoRequest}
import zone.overlap.userd.utils._

import scala.util.{Failure, Success, Try}

case class UserRecord(id: String,
                      firstName: String,
                      lastName: String,
                      email: String,
                      emailVerificationCode: Option[String],
                      passwordHash: String,
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

  def findUserById(id: String): Task[Option[UserRecord]] = {
    val q = quote {
      users.filter(_.id == lift(id))
    }
    Task(context.run(q).headOption)
  }

  def findUserByEmail(email: String): Task[Option[UserRecord]] = {
    val q = quote {
      users.filter(_.email == lift(email))
    }
    Task(context.run(q).headOption)
  }

  def findUserPendingEmailVerification(code: String): Task[Option[UserRecord]] = {
    val q = quote {
      users.filter(u => u.emailVerificationCode == lift(code) && u.status == lift(UserStatus.PENDING_EMAIL_VERIFICATION))
    }
    Task(context.run(q).headOption)
  }

  // Also clears the email verification code
  def activateUser(email: String): Task[Unit] = {
    val q = quote {
      users
        .filter(_.email == lift(email))
        .update(
          _.emailVerificationCode -> lift(Option.empty),
          _.status -> lift(UserStatus.ACTIVE)
        )
    }
    Task(context.run(q))
  }

  def createUser(signUpRequest: SignUpRequest): Task[String] = {
    val userId = UUID.randomUUID().toString
    createUser(
      UserRecord(
        userId,
        signUpRequest.firstName,
        signUpRequest.lastName,
        signUpRequest.email,
        Option(randomUniqueCode()),
        hashPassword(signUpRequest.password), // TODO: Is there a way to store passwords in Dex only?
        UserStatus.PENDING_EMAIL_VERIFICATION,
        Instant.now()
      )
    ).map(_ => userId)
  }

  def createUser(userRecord: UserRecord): Task[Unit] = {
    val q = quote {
      users.insert(lift(userRecord))
    }
    Task(context.run(q))
  }

  def updateUser(currentEmail: String, updateInfoRequest: UpdateInfoRequest): Task[Unit] = {
    val q = quote {
      users
        .filter(_.email == lift(currentEmail))
        .update(
          _.firstName -> lift(updateInfoRequest.firstName),
          _.lastName -> lift(updateInfoRequest.lastName)
        )
    }
    Task(context.run(q))
  }

  def updateUserStatus(email: String, userStatus: UserStatus): Task[Unit] = {
    val q = quote {
      users
        .filter(_.email == lift(email))
        .update(
          _.status -> lift(userStatus)
        )
    }
    Task(context.run(q))
  }

  // Simple blocking health check. Can we query the users database table?
  def canQueryUsers(): Boolean = {
    Try(
      context.run(quote(users.map(_.id))).headOption
    ) match {
      case Success(_) => true
      case Failure(_) => false
    }
  }
}

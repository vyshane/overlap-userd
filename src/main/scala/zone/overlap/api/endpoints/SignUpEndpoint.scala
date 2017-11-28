// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api.endpoints

import java.time.Instant

import api.api.{CreatePasswordReq, CreatePasswordResp}
import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import io.grpc.Status
import monix.eval.Task
import org.slf4j.LoggerFactory
import zone.overlap.internalapi.events.user.UserSignedUp
import zone.overlap.api.user.{SignUpRequest, SignUpResponse}
import zone.overlap.userd.persistence.UserRecord

import scala.collection.mutable.ListBuffer

object SignUpEndpoint {

  private val log = LoggerFactory.getLogger(this.getClass)

  def signUp(findUserByEmail: String => Option[UserRecord],
             createUser: SignUpRequest => String,
             createDexPassword: CreatePasswordReq => Task[CreatePasswordResp],
             notifyUserSignedUp: UserSignedUp => Task[Unit])(request: SignUpRequest): Task[SignUpResponse] = {
    Task(request)
      .flatMap(ensureValidSignUpRequest(_))
      .flatMap(ensureEmailNotTaken(findUserByEmail)(_))
      .map(createUser(_))
      .flatMap(userId => addToDex(createDexPassword)(buildCreatePasswordReq(userId, request.email, request.password)))
      .flatMap(userId => {
        val now = Instant.now()
        notifyUserSignedUp(
          UserSignedUp(
            userId,
            request.firstName,
            request.lastName,
            request.email,
            Option(Timestamp(now.getEpochSecond, now.getNano))
          )
        ).map(_ => SignUpResponse(userId))
      })
  }

  private def ensureValidSignUpRequest(request: SignUpRequest): Task[SignUpRequest] = {
    validationErrorMessage(request)
      .map(errorMessage => Task.raiseError(Status.INVALID_ARGUMENT.augmentDescription(errorMessage).asRuntimeException()))
      .getOrElse(Task(request))
  }

  private def validationErrorMessage(request: SignUpRequest): Option[String] = {
    val errors = new ListBuffer[String]

    def validName(name: String) = name.length > 0 && name.length <= 255
    if (!validName(request.firstName))
      errors += "First name must be between 1 and 255 characters"
    if (!validName(request.lastName))
      errors += "Last name must be between 1 and 255 characters"

    if (request.email.trim.isEmpty)
      errors += "Email address is required"
    else if (!validEmail(request.email))
      errors += "Email address is invalid"

    if (request.password.length < 6)
      errors += "Password must be at least 6 characters"

    if (errors.isEmpty) Option.empty
    else Option(errors.mkString("\n"))
  }

  private def validEmail(email: String) = {
    // Regex source: https://www.w3.org/TR/html5/forms.html#valid-e-mail-address
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
      .findFirstMatchIn(email)
      .isDefined
  }

  private def ensureEmailNotTaken(findUserByEmail: String => Option[UserRecord])(
      signUpRequest: SignUpRequest): Task[SignUpRequest] = {
    findUserByEmail(signUpRequest.email)
      .map(_ =>
        Task.raiseError(Status.ALREADY_EXISTS.augmentDescription("Email address is already taken").asRuntimeException()))
      .getOrElse(Task(signUpRequest))
  }

  private def addToDex(createDexPassword: CreatePasswordReq => Task[CreatePasswordResp])(
      request: CreatePasswordReq): Task[String] = {
    createDexPassword(request)
      .flatMap(resp => {
        if (resp.alreadyExists) {
          log.error("Unable to create password in dex: User already exists")
          Task.raiseError(Status.ABORTED.augmentDescription("User already exists in auth service").asRuntimeException())
        } else {
          Task(request.password.get.userId)
        }
      })
  }

  private def buildCreatePasswordReq(userId: String, email: String, password: String): CreatePasswordReq = {
    import com.github.t3hnar.bcrypt._
    CreatePasswordReq(
      Option(
        api.api.Password(
          email,
          ByteString.copyFromUtf8(password.bcrypt),
          email,
          userId
        )
      )
    )
  }
}

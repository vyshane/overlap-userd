// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api.endpoints

import java.time.Instant

import com.google.protobuf.timestamp.Timestamp
import io.grpc.Status
import monix.eval.Task
import zone.overlap.internalapi.events.user.UserSignedUp
import zone.overlap.api.user.{SignUpRequest, SignUpResponse}

import scala.collection.mutable.ListBuffer

object SignUpEndpoint {

  def signUp(createUser: SignUpRequest => String, notifyUserSignedUp: UserSignedUp => Task[Unit])
            (request: SignUpRequest): Task[SignUpResponse] = {
    Task(request)
      .flatMap(ensureValidSignUpRequest(_))
      .map(createUser(_))
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
}

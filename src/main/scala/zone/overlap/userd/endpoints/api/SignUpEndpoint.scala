// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import java.time.{Clock, Instant}

import cats.data.Validated.{Invalid, Valid}
import com.google.protobuf.timestamp.Timestamp
import io.grpc.Status
import monix.eval.Task
import zone.overlap.api.user.{SignUpRequest, SignUpResponse}
import zone.overlap.internalapi.events.user.UserSignedUp
import zone.overlap.userd.persistence.UserRecord
import zone.overlap.userd.validation.RequestValidator

object SignUpEndpoint {

  def signUp(findUserByEmail: String => Option[UserRecord],
             createUser: SignUpRequest => String,
             notifyUserSignedUp: UserSignedUp => Task[Unit],
             clock: Clock)(request: SignUpRequest): Task[SignUpResponse] = {
    Task(request)
      .flatMap(ensureValidSignUpRequest(findUserByEmail)(_))
      .map(createUser(_))
      .flatMap { userId =>
        val userSignedUp = buildUserSignedUpMessage(clock)(userId, request)
        notifyUserSignedUp(userSignedUp).map(_ => SignUpResponse())
      }
  }

  private def ensureValidSignUpRequest(findUserByEmail: String => Option[UserRecord])(
      request: SignUpRequest): Task[SignUpRequest] = {
    RequestValidator.validateSignUpRequest(findUserByEmail)(request) match {
      case Valid(request) => Task(request)
      case Invalid(nel) => {
        val errorDescription = nel.map(v => v.errorMessage).toList.mkString("\n")
        Task.raiseError(Status.INVALID_ARGUMENT.augmentDescription(errorDescription).asRuntimeException())
      }
    }
  }

  private def buildUserSignedUpMessage(clock: Clock)(userId: String, signUpRequest: SignUpRequest): UserSignedUp = {
    val now = Instant.now(clock)
    UserSignedUp(
      userId,
      signUpRequest.firstName,
      signUpRequest.lastName,
      signUpRequest.email,
      Option(Timestamp(now.getEpochSecond, now.getNano))
    )
  }
}

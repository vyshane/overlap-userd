// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import java.time.{Clock, Instant}

import cats.data.Validated.{Invalid, Valid}
import com.google.protobuf.timestamp.Timestamp
import io.grpc.Status
import monix.eval.Task
import zone.overlap.api.user.{SignUpRequest, SignUpResponse}
import zone.overlap.internalapi.events.user.UserSignedUp
import zone.overlap.userd.endpoints.TaskScheduling
import zone.overlap.userd.persistence.UserRecord
import zone.overlap.userd.validation.RequestValidator

object SignUpEndpoint extends TaskScheduling {

  def signUp(findUserByEmail: String => Task[Option[UserRecord]],
             createUser: SignUpRequest => Task[String],
             notifyUserSignedUp: UserSignedUp => Task[Unit],
             clock: Clock)(request: SignUpRequest): Task[SignUpResponse] = {
    for (existingUser <- findUserByEmail(request.email).executeOn(ioScheduler).asyncBoundary;
         _ <- ensureValidSignUpRequest(_ => existingUser.isDefined)(request);
         userId <- createUser(request).executeOn(ioScheduler).asyncBoundary;
         userSignedUp <- buildUserSignedUpMessage(clock)(userId, request);
         _ <- notifyUserSignedUp(userSignedUp).executeOn(ioScheduler).asyncBoundary) yield (SignUpResponse())
  }

  private def ensureValidSignUpRequest(emailExists: String => Boolean)(request: SignUpRequest): Task[SignUpRequest] = {
    RequestValidator.validateSignUpRequest(emailExists)(request) match {
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

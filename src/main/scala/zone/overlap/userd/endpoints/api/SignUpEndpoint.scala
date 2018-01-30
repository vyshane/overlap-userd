// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import java.time.{Clock, Instant}

import com.google.protobuf.timestamp.Timestamp
import monix.eval.Task
import zone.overlap.api.user.{SignUpRequest, SignUpResponse}
import zone.overlap.internalapi.events.user.UserSignedUp
import zone.overlap.userd.endpoints.TaskScheduling
import zone.overlap.userd.persistence.UserRecord
import zone.overlap.userd.validation.RequestValidator._

object SignUpEndpoint extends TaskScheduling {

  def signUp(findUserByEmail: String => Task[Option[UserRecord]],
             createUser: SignUpRequest => Task[String],
             notifyUserSignedUp: UserSignedUp => Task[Unit],
             clock: Clock)
            (request: SignUpRequest): Task[SignUpResponse] = {
    for {
      existingUser <- findUserByEmail(request.email).executeOn(ioScheduler).asyncBoundary
      _ <- ensureValid(validateSignUpRequest(_ => existingUser.isDefined))(request)
      userId <- createUser(request).executeOn(ioScheduler).asyncBoundary
      userSignedUp <- Task.now(buildUserSignedUpMessage(clock)(userId, request))
      _ <- notifyUserSignedUp(userSignedUp).executeOn(ioScheduler).asyncBoundary
    } yield SignUpResponse()
  }

  private[api] def buildUserSignedUpMessage(clock: Clock)(userId: String, signUpRequest: SignUpRequest): UserSignedUp = {
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

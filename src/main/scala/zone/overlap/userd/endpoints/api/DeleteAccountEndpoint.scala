// Copyright 2018 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import java.time.{Clock, Instant}

import com.coreos.dex.api.{DeletePasswordReq, DeletePasswordResp}
import com.google.protobuf.timestamp.Timestamp
import io.grpc.Status
import monix.eval.Task
import zone.overlap.api.{DeleteAccountRequest, DeleteAccountResponse}
import zone.overlap.internalapi.events.AccountDeleted
import zone.overlap.userd.authentication.UserContext
import zone.overlap.userd.endpoints.TaskScheduling
import zone.overlap.userd.persistence.{Email, UserRecord}
import zone.overlap.userd.validation.RequestValidator._
import zone.overlap.userd.utils._

object DeleteAccountEndpoint extends TaskScheduling {

  def handle(
      ensureAuthenticated: () => Task[UserContext],
      findUserByEmail: Email => Task[Option[UserRecord]],
      deleteUser: Email => Task[Unit],
      unregisterFromDex: DeletePasswordReq => Task[DeletePasswordResp],
      notifyAccountDeleted: AccountDeleted => Task[Unit],
      clock: Clock
  )(request: DeleteAccountRequest): Task[DeleteAccountResponse] = {
    for {
      userContext <- ensureAuthenticated()
      _ <- ensureValid(validateDeleteAccountRequest)(request)
      user <- ensureUserExists(findUserByEmail)(userContext.email)
      _ <- ensureValidCredentials(request, user)
      response <- deleteUser(userContext.email).executeOn(ioScheduler)
      _ <- unregisterFromDex(DeletePasswordReq(request.email))
      event = buildAccountDeletedEvent(clock)(user)
      _ <- notifyAccountDeleted(event).asyncBoundary
    } yield DeleteAccountResponse()
  }

  private[api] def ensureUserExists(findUserByEmail: Email => Task[Option[UserRecord]])(email: Email): Task[UserRecord] = {
    findUserByEmail(email)
      .executeOn(ioScheduler)
      .asyncBoundary
      .flatMap { user =>
        user
          .map(Task.now(_))
          .getOrElse(Task.raiseError(Status.NOT_FOUND.augmentDescription("User account not found").asRuntimeException()))
      }
  }

  private[api] def ensureValidCredentials(request: DeleteAccountRequest, user: UserRecord): Task[UserRecord] = {
    if (request.email == user.email && hashPassword(request.password) == user.passwordHash)
      Task.now(user)
    else
      Task.raiseError(
        Status.PERMISSION_DENIED
          .augmentDescription("Email and password combination not valid for current user")
          .asRuntimeException()
      )
  }

  private[api] def buildAccountDeletedEvent(clock: Clock)(user: UserRecord): AccountDeleted = {
    val now = Instant.now(clock)
    AccountDeleted(
      user.id,
      user.firstName,
      user.lastName,
      user.email,
      Some(Timestamp(now.getEpochSecond, now.getNano))
    )
  }
}

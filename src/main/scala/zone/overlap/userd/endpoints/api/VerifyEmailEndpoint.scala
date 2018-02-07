// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import com.coreos.dex.api.{CreatePasswordReq, CreatePasswordResp, Password => DexPassword}
import com.google.protobuf.ByteString
import io.grpc.Status
import monix.eval.Task
import org.slf4j.LoggerFactory
import zone.overlap.api.{VerifyEmailRequest, VerifyEmailResponse}
import zone.overlap.userd.endpoints.TaskScheduling
import zone.overlap.userd.persistence.UserRecord

object VerifyEmailEndpoint extends TaskScheduling {

  private val log = LoggerFactory.getLogger(this.getClass)

  /*
   * Mark user email as having been verified and register user with Dex
   */
  def handle(
      findUserByEmailVerificationCode: String => Task[Option[UserRecord]],
      registerUserWithDex: CreatePasswordReq => Task[CreatePasswordResp],
      activateUser: (String) => Task[Unit]
  )(request: VerifyEmailRequest): Task[VerifyEmailResponse] = {
    for {
      possibleUser <- findUserByEmailVerificationCode(request.verificationCode).executeOn(ioScheduler).asyncBoundary
      user <- ensureUserExists(possibleUser)
      createPasswordReq = buildCreatePasswordReq(user.id, displayName(user), user.email, user.passwordHash)
      createPasswordResp <- registerUserWithDex(createPasswordReq).executeOn(ioScheduler).asyncBoundary
      _ <- ensureDexUserCreated(createPasswordResp)
      _ <- activateUser(user.email).executeOn(ioScheduler).asyncBoundary
    } yield VerifyEmailResponse()
  }

  private[api] def ensureUserExists(user: Option[UserRecord]): Task[UserRecord] = {
    user
      .map(Task.now(_))
      .getOrElse {
        Task.raiseError(Status.INVALID_ARGUMENT.augmentDescription("Invalid email verification code").asRuntimeException())
      }
  }

  private[api] def ensureDexUserCreated(createPasswordResp: CreatePasswordResp): Task[CreatePasswordResp] = {
    if (createPasswordResp.alreadyExists) {
      log.error("Unable to create password in dex: User already exists")
      Task.raiseError(Status.ABORTED.augmentDescription("User already exists in auth service").asRuntimeException())
    } else {
      Task.now(createPasswordResp)
    }
  }

  def buildCreatePasswordReq(userId: String, displayName: String, email: String, passwordHash: String): CreatePasswordReq = {
    CreatePasswordReq(
      Some(
        DexPassword(
          email,
          ByteString.copyFromUtf8(passwordHash),
          displayName,
          userId
        )
      )
    )
  }

  private[api] def displayName(user: UserRecord): String = s"${user.firstName} ${user.lastName}"
}

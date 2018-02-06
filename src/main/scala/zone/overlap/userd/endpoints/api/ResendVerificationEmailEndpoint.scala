// Copyright 2018 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import io.grpc.Status
import monix.eval.Task
import zone.overlap.api.{ResendVerificationEmailRequest, ResendVerificationEmailResponse}
import zone.overlap.internalapi.{SendWelcomeEmailRequest, SendWelcomeEmailResponse}
import zone.overlap.privateapi.UserStatus
import zone.overlap.userd.endpoints.TaskScheduling
import zone.overlap.userd.persistence.{Email, UserRecord}
import zone.overlap.userd.utils._
import zone.overlap.userd.validation.RequestValidator._

object ResendVerificationEmailEndpoint extends TaskScheduling {

  def handle(findUserByEmail: Email => Task[Option[UserRecord]],
             verificationCodeUpdater: (Email, String) => Task[Unit],
             sendWelcomeEmail: SendWelcomeEmailRequest => Task[SendWelcomeEmailResponse])
            (request: ResendVerificationEmailRequest): Task[ResendVerificationEmailResponse] = {
    for {
      _ <- ensureValid(validateResendVerificationEmailRequest)(request)
      user <- ensureUserExists(findUserByEmail)(request.email)
      _ <- ensurePendingEmailVerification(user)
      currentCode <- assignEmailVerificationCodeIfNecessary(randomUniqueCode, verificationCodeUpdater)(user)
      sendEmailRequest = buildSendWelcomEmailRequest(user, currentCode)
      _ <- sendWelcomeEmail(sendEmailRequest).executeOn(ioScheduler).asyncBoundary
    } yield ResendVerificationEmailResponse()
  }

  private[api] def ensureUserExists(findUserByEmail: Email => Task[Option[UserRecord]])(email: Email): Task[UserRecord] = {
    findUserByEmail(email).executeOn(ioScheduler).asyncBoundary
      .flatMap { user =>
        user
          .map(Task.now(_))
          .getOrElse(Task.raiseError(Status.NOT_FOUND.augmentDescription("Email not found").asRuntimeException()))
      }
  }

  private[api] def ensurePendingEmailVerification(user: UserRecord): Task[UserRecord] = {
    if (user.status != UserStatus.PENDING_EMAIL_VERIFICATION.name)
      Task.raiseError(
        Status.INVALID_ARGUMENT.augmentDescription("Email has already been already verified").asRuntimeException())
    else
      Task.now(user)
  }

  private[api] def assignEmailVerificationCodeIfNecessary(codeGenerator: () => String,
                                                          updateCode: (Email, String) => Task[Unit])
                                                         (user: UserRecord): Task[String] = {
    user.emailVerificationCode
      .map(Task.now(_))
      .getOrElse {
        val newCode = codeGenerator()
        updateCode(user.email, newCode).map(_ => newCode).executeOn(ioScheduler).asyncBoundary
      }
  }

  private[api] def buildSendWelcomEmailRequest(user: UserRecord, verificationCode: String): SendWelcomeEmailRequest = {
    SendWelcomeEmailRequest(user.firstName, user.lastName, user.email, verificationCode)
  }
}

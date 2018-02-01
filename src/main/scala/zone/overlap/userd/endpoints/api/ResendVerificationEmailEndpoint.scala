// Copyright 2018 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import io.grpc.Status
import monix.eval.Task
import zone.overlap.api.{ResendVerificationEmailRequest, ResendVerificationEmailResponse}
import zone.overlap.internalapi.EmailDeliveryGrpcMonix.EmailDeliveryServiceStub
import zone.overlap.internalapi.SendWelcomeEmailRequest
import zone.overlap.privateapi.UserStatus
import zone.overlap.userd.persistence.{Email, UserRecord}
import zone.overlap.userd.utils._

object ResendVerificationEmailEndpoint {

  def resendVerificationEmail(findUserByEmail: String => Task[Option[UserRecord]],
                              verificationCodeUpdater: (Email, String) => Task[Unit],
                              emailDeliveryStub: EmailDeliveryServiceStub)
                             (request: ResendVerificationEmailRequest): Task[ResendVerificationEmailResponse] = {
    for {
      user <- ensureUserExists(findUserByEmail)(request.email)
      _ <- ensurePendingEmailVerification(user)
      currentCode <- assignEmailVerificationCodeIfNecessary(randomUniqueCode, verificationCodeUpdater)(user)
      sendEmailRequest = buildSendWelcomEmailRequest(user, currentCode)
      _ <- emailDeliveryStub.sendWelcomeEmail(sendEmailRequest)
    } yield ResendVerificationEmailResponse()
  }

  private[api] def ensureUserExists(findUserByEmail: Email => Task[Option[UserRecord]])(email: Email): Task[UserRecord] = {
    findUserByEmail(email)
      .flatMap { user =>
        user
          .map(Task.now(_))
          .getOrElse(Task.raiseError(Status.NOT_FOUND.augmentDescription("Email not found").asRuntimeException()))
      }
  }

  private[api] def ensurePendingEmailVerification(user: UserRecord): Task[UserRecord] = {
    if (user.status != UserStatus.PENDING_EMAIL_VERIFICATION)
      Task.raiseError(Status.NOT_FOUND.augmentDescription("Email already verified").asRuntimeException())
    else
      Task.now(user)
  }

  private[api] def assignEmailVerificationCodeIfNecessary(codeGenerator: () => String,
                                                          updateCode: (Email, String) => Task[Unit])
                                                         (user: UserRecord): Task[String] = {
    user.emailVerificationCode
      .map(Task.now(_))
      .getOrElse {
        val newCode = randomUniqueCode()
        updateCode(user.email, newCode).map(_ => newCode)
      }
  }

  private[api] def buildSendWelcomEmailRequest(user: UserRecord, currentCode: String): SendWelcomeEmailRequest = {
    SendWelcomeEmailRequest(user.firstName, user.lastName, user.email, currentCode)
  }
}

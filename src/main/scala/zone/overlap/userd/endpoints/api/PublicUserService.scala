// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import java.time.Clock

import com.coreos.dex.api.ApiGrpcMonix
import monix.eval.Task
import zone.overlap.api._
import zone.overlap.internalapi.EmailDeliveryGrpcMonix.EmailDeliveryServiceStub
import zone.overlap.userd.endpoints._
import zone.overlap.userd.events.EventPublisher
import zone.overlap.userd.persistence.UserRepository

class PublicUserService(userRepository: UserRepository[_, _],
                        dexStub: ApiGrpcMonix.DexStub,
                        emailDeliveryStub: EmailDeliveryServiceStub,
                        eventPublisher: EventPublisher)
    extends UserGrpcMonix.UserService {

  override def signUp(request: SignUpRequest): Task[SignUpResponse] = {
    SignUpEndpoint.handle(
      userRepository.findUserByEmail,
      userRepository.createUser,
      eventPublisher.sendUserSignedUp,
      Clock.systemUTC()
    )(request)
  }

  override def verifyEmail(request: VerifyEmailRequest): Task[VerifyEmailResponse] = {
    VerifyEmailEndpoint.handle(
      userRepository.findUserPendingEmailVerification,
      dexStub.createPassword,
      userRepository.activateUser
    )(request)
  }

  override def resendVerificationEmail(request: ResendVerificationEmailRequest) = {
    ResendVerificationEmailEndpoint.handle(
      userRepository.findUserByEmail,
      userRepository.updateEmailVerificationCode,
      emailDeliveryStub.sendWelcomeEmail
    )(request)
  }

  override def updateInfo(request: UpdateInfoRequest): Task[UpdateInfoResponse] = {
    UpdateInfoEndpoint.handle(
      ensureAuthenticated,
      userRepository.updateUser
    )(request)
  }

  override def updateEmail(request: UpdateEmailRequest): Task[UpdateEmailResponse] = ???

  override def changePassword(request: ChangePasswordRequest): Task[ChangePasswordResponse] = ???

  override def deleteAccount(request: DeleteAccountRequest): Task[DeleteAccountResponse] = {
    DeleteAccountEndpoint.handle(
      ensureAuthenticated,
      userRepository.findUserByEmail,
      userRepository.deleteUser,
      dexStub.deletePassword,
      eventPublisher.sendAccountDeleted,
      Clock.systemUTC()
    )(request)
  }
}

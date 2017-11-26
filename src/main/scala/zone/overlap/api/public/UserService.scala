// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api

import zone.overlap.api.public.endpoints.SignUpEndpoint
import zone.overlap.api.user._
import zone.overlap.userd.events.EventPublisher
import zone.overlap.userd.persistence.UserRepository

class UserService(userRepository: UserRepository[_, _], eventPublisher: EventPublisher) extends UserGrpcMonix.UserService {

  override def signUp(request: SignUpRequest) =
    SignUpEndpoint.signUp(userRepository.createUser, eventPublisher.sendUserSignedUp)(request)

  override def verifyEmail(request: VerifyEmailRequest) = ???

  override def resendVerificationEmail(request: ResendVerificationEmailRequest) = ???

  override def updateInfo(request: UpdateInfoRequest) = ???

  override def changePassword(request: ChangePasswordRequest) = ???

  override def deleteAccount(request: DeleteAccountRequest) = ???
}

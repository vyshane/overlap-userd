// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api

import java.time.Clock

import com.coreos.dex.api.api.ApiGrpcMonix
import monix.eval.Task
import zone.overlap.api.endpoints.{SignUpEndpoint, UpdateInfoEndpoint}
import zone.overlap.api.user._
import zone.overlap.userd.events.EventPublisher
import zone.overlap.userd.persistence.UserRepository

class PublicUserService(userRepository: UserRepository[_, _], dexStub: ApiGrpcMonix.DexStub, eventPublisher: EventPublisher)
    extends UserGrpcMonix.UserService {

  override def signUp(request: SignUpRequest): Task[SignUpResponse] = {
    SignUpEndpoint.signUp(userRepository.findUserByEmail,
                          userRepository.createUser,
                          dexStub.createPassword,
                          eventPublisher.sendUserSignedUp,
                          Clock.systemUTC())(request)
  }

  override def verifyEmail(request: VerifyEmailRequest) = ???

  override def resendVerificationEmail(request: ResendVerificationEmailRequest) = ???

  override def updateInfo(request: UpdateInfoRequest): Task[UpdateInfoResponse] = {
    UpdateInfoEndpoint.updateInfo(userRepository.updateUser)(request)
  }

  override def changePassword(request: ChangePasswordRequest) = ???

  override def deleteAccount(request: DeleteAccountRequest) = ???
}

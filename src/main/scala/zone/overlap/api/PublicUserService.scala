// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api

import java.time.Clock

import com.coreos.dex.api.api.ApiGrpcMonix
import monix.eval.Task
import zone.overlap.api.endpoints.{SignUpEndpoint, UpdateInfoEndpoint, VerifyEmailEndpoint}
import zone.overlap.api.user._
import zone.overlap.userd.authentication.UserContextServerInterceptor
import zone.overlap.userd.events.EventPublisher
import zone.overlap.userd.persistence.UserRepository

class PublicUserService(userRepository: UserRepository[_, _], dexStub: ApiGrpcMonix.DexStub, eventPublisher: EventPublisher)
    extends UserGrpcMonix.UserService {

  override def signUp(request: SignUpRequest): Task[SignUpResponse] = {
    SignUpEndpoint.signUp(userRepository.findUserByEmail,
                          userRepository.createUser,
                          eventPublisher.sendUserSignedUp,
                          Clock.systemUTC())(request)
  }

  override def verifyEmail(request: VerifyEmailRequest): Task[VerifyEmailResponse] = {
    VerifyEmailEndpoint.verifyEmail(userRepository.findUserByEmailVerificationCode,
                                    userRepository.updateUserStatus,
                                    dexStub.createPassword)(request)
  }

  override def resendVerificationEmail(request: ResendVerificationEmailRequest) = ???

  override def updateInfo(request: UpdateInfoRequest): Task[UpdateInfoResponse] = {
    UpdateInfoEndpoint.updateInfo(UserContextServerInterceptor.ensureAuthenticated, userRepository.updateUser)(request)
  }

  override def changePassword(request: ChangePasswordRequest) = ???

  override def deleteAccount(request: DeleteAccountRequest) = ???
}

// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api

import zone.overlap.api.user._

class UserService extends UserGrpcMonix.UserService {

  override def signUp(request: SignUpRequest) = ???

  override def verifyEmail(request: VerifyEmailRequest) = ???

  override def resendVerificationEmail(request: ResendVerificationEmailRequest) = ???

  override def updateInfo(request: UpdateInfoRequest) = ???

  override def changePassword(request: ChangePasswordRequest) = ???

  override def deleteAccount(request: DeleteAccountRequest) = ???
}

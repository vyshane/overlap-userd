// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import com.coreos.dex.api.api.{CreatePasswordReq, CreatePasswordResp, Password => DexPassword}
import com.google.protobuf.ByteString
import io.grpc.Status
import monix.eval.Task
import org.slf4j.LoggerFactory
import zone.overlap.api.user.{VerifyEmailRequest, VerifyEmailResponse}
import zone.overlap.privateapi.user.UserStatus
import zone.overlap.userd.persistence.UserRecord

object VerifyEmailEndpoint {

  private val log = LoggerFactory.getLogger(this.getClass)

  def verifyEmail(findUserByEmailVerificationCode: String => Option[UserRecord],
                  updateUserStatus: (String, UserStatus) => Unit,
                  registerUserWithDex: CreatePasswordReq => Task[CreatePasswordResp])(
      verifyEmailRequest: VerifyEmailRequest): Task[VerifyEmailResponse] = {
    findUserByEmailVerificationCode(verifyEmailRequest.verificationCode)
      .map(Task(_))
      .getOrElse(
        Task.raiseError(Status.INVALID_ARGUMENT.augmentDescription("Invalid email verification code").asRuntimeException()))
      .flatMap(user =>
        registerWithDex(registerUserWithDex)(
          buildCreatePasswordReq(user.id, displayName(user), user.email, user.passwordHash)))
      .map(email => updateUserStatus(email, UserStatus.ACTIVE))
      .map(_ => VerifyEmailResponse())
  }

  private def registerWithDex(createDexPassword: CreatePasswordReq => Task[CreatePasswordResp])(
      request: CreatePasswordReq): Task[String] = {
    createDexPassword(request)
      .flatMap(resp => {
        if (resp.alreadyExists) {
          log.error("Unable to create password in dex: User already exists")
          Task.raiseError(Status.ABORTED.augmentDescription("User already exists in auth service").asRuntimeException())
        } else {
          Task(request.password.get.email)
        }
      })
  }

  def buildCreatePasswordReq(userId: String, displayName: String, email: String, passwordHash: String): CreatePasswordReq = {
    CreatePasswordReq(
      Option(
        DexPassword(
          email,
          ByteString.copyFromUtf8(passwordHash),
          displayName,
          userId
        )
      )
    )
  }

  private def displayName(user: UserRecord): String = s"$user.firstName $user.lastName"
}

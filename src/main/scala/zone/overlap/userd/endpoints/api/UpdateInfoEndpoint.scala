// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import cats.data.Validated.{Invalid, Valid}
import io.grpc.Status
import monix.eval.Task
import zone.overlap.api.user.{UpdateInfoRequest, UpdateInfoResponse}
import zone.overlap.userd.authentication.UserContext
import zone.overlap.userd.validation.RequestValidator

object UpdateInfoEndpoint {

  def updateInfo(ensureAuthenticated: () => Task[UserContext], updateUser: (String, UpdateInfoRequest) => Unit)(
      request: UpdateInfoRequest): Task[UpdateInfoResponse] = {
    for (
      userContext <- ensureAuthenticated();
      validRequest <- ensureValidUpdateInfoRequest(request);
      response <- Task(updateUser(userContext.email, validRequest)).map(_ => UpdateInfoResponse())
    ) yield response
  }

  private def ensureValidUpdateInfoRequest(request: UpdateInfoRequest): Task[UpdateInfoRequest] = {
    RequestValidator.validateUpdateUserInfoRequest(request) match {
      case Valid(request) => Task(request)
      case Invalid(nel) => {
        val errorDescription = nel.map(v => v.errorMessage).toList.mkString("\n")
        Task.raiseError(Status.INVALID_ARGUMENT.augmentDescription(errorDescription).asRuntimeException())
      }
    }
  }
}

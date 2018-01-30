// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import monix.eval.Task
import zone.overlap.api.user.{UpdateInfoRequest, UpdateInfoResponse}
import zone.overlap.userd.authentication.UserContext
import zone.overlap.userd.validation.RequestValidator._

object UpdateInfoEndpoint {

  def updateInfo(ensureAuthenticated: () => Task[UserContext],
                 updateUser: (String, UpdateInfoRequest) => Task[Unit])
                (request: UpdateInfoRequest): Task[UpdateInfoResponse] = {
    for {
      userContext <- ensureAuthenticated()
      validRequest <- ensureValid(validateUpdateUserInfoRequest)(request)
      response <- updateUser(userContext.email, validRequest)
    } yield UpdateInfoResponse()
  }
}

// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import monix.eval.Task
import zone.overlap.api.user.{UpdateInfoRequest, UpdateInfoResponse}
import zone.overlap.userd.authentication.UserContext

object UpdateInfoEndpoint {

  def updateInfo(ensureAuthenticated: () => Task[UserContext], updateUser: (String, UpdateInfoRequest) => Unit)(
      request: UpdateInfoRequest): Task[UpdateInfoResponse] = {
    ensureAuthenticated()
      // TODO: Validate request
      .map(userContext => updateUser(userContext.email, request))
      .map(_ => UpdateInfoResponse())
  }
}

// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api.endpoints

import monix.eval.Task
import zone.overlap.api.user.{UpdateInfoRequest, UpdateInfoResponse}
import zone.overlap.userd.authentication.UserContext

object UpdateInfoEndpoint {

  def updateInfo(ensureAuthenticated: () => Task[UserContext], updateUser: (String, UpdateInfoRequest) => Unit)(
      request: UpdateInfoRequest): Task[UpdateInfoResponse] = {
    ensureAuthenticated()
      .map(userContext => updateUser(userContext.email, request))
      .map(_ => UpdateInfoResponse())
  }
}

// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import monix.eval.Task
import zone.overlap.api.{UpdateInfoRequest, UpdateInfoResponse}
import zone.overlap.userd.authentication.UserContext
import zone.overlap.userd.endpoints.TaskScheduling
import zone.overlap.userd.validation.RequestValidator._

object UpdateInfoEndpoint extends TaskScheduling {

  def handle(
      ensureAuthenticated: () => Task[UserContext],
      updateUser: (String, UpdateInfoRequest) => Task[Unit]
  )(request: UpdateInfoRequest): Task[UpdateInfoResponse] = {
    for {
      userContext <- ensureAuthenticated()
      validRequest <- ensureValid(validateUpdateUserInfoRequest)(request)
      response <- updateUser(userContext.email, validRequest).executeOn(ioScheduler).asyncBoundary
    } yield UpdateInfoResponse()
  }
}

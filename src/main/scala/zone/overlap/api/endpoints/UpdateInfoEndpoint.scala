// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api.endpoints

import io.grpc.Status
import monix.eval.Task
import zone.overlap.api.user.{UpdateInfoRequest, UpdateInfoResponse}
import zone.overlap.userd.security.UserContextServerInterceptor

object UpdateInfoEndpoint {

  def updateInfo(updateUser: UpdateInfoRequest => Unit)(request: UpdateInfoRequest): Task[UpdateInfoResponse] = {
    UserContextServerInterceptor.getUserContext() match {
      case Some(userContext) =>
        // TODO
        Task(UpdateInfoResponse())
      case None =>
        Task.raiseError(Status.UNAUTHENTICATED.asRuntimeException())
    }
  }
}

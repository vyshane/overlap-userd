// Copyright 2018 Vy-Shane Xie Sin Fat

package zone.overlap.userd

import io.grpc.Status
import monix.eval.Task
import mu.node.grpc.oidc.IdTokenInterceptor
import zone.overlap.userd.authentication.AuthenticationContext

package object endpoints {

  def ensureAuthenticated(): Task[AuthenticationContext] = {
    IdTokenInterceptor
      .getIdTokenContext()
      .map(Task.now(_))
      .getOrElse(Task.raiseError(Status.UNAUTHENTICATED.asRuntimeException()))
  }
}

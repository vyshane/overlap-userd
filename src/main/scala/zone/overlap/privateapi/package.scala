// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.privateapi

import io.grpc.Status
import monix.eval.Task
import zone.overlap.privateapi.user.{FindUserByIdRequest, FindUserByIdResponse}
import zone.overlap.userd.persistence.UserRecord

// The actual implementations for the RPC endpoints
// These functions should kept be pure. Side-effectful functions are passed in as dependencies to make testing easy.
package object Endpoints {

  def findUserById(queryDb: String => Option[UserRecord])(request: FindUserByIdRequest): Task[FindUserByIdResponse] = {
    if (request.userId.isEmpty)
      Task.raiseError(Status.INVALID_ARGUMENT.augmentDescription("User ID is required").asRuntimeException())
    else
      Task {
        val user = queryDb(request.userId).map(_.toProtobuf)
        FindUserByIdResponse(user)
      }
  }
}

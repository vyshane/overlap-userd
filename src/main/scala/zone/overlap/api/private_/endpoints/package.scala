// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api.private_

import com.google.protobuf.timestamp.Timestamp
import monix.eval.Task
import zone.overlap.api.private_.user.{FindUserByIdRequest, FindUserByIdResponse, User, UserStatus}
import zone.overlap.userd.persistence.UserRecord

// The actual implementations for the RPC endpoints
// These functions should kept be pure. Side-effectful functions are passed in as dependencies to make testing easy.
package object Endpoints {

  def findUserById(queryDb: String => Option[UserRecord])(request: FindUserByIdRequest): Task[FindUserByIdResponse] = {
    if (request.userId.isEmpty)
      Task.raiseError(new IllegalArgumentException("User ID is required"))
    Task {
      val user = queryDb(request.userId).map(userRecordToProto(_))
      FindUserByIdResponse(user)
    }
  }

  private def userRecordToProto(record: UserRecord): User = {
    User(
      record.id,
      record.firstName,
      record.lastName,
      record.email,
      UserStatus.ACTIVE, // TODO
      Option(Timestamp(record.signedUp.getEpochSecond, record.signedUp.getNano))
    )
  }
}

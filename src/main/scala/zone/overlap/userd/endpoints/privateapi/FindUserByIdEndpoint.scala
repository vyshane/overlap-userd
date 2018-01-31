// Copyright 2018 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.privateapi

import monix.eval.Task
import zone.overlap.privateapi.{FindUserByIdRequest, FindUserByIdResponse}
import zone.overlap.userd.persistence.UserRecord
import zone.overlap.userd.validation.RequestValidator._

object FindUserByIdEndpoint {

  def findUserById(queryDatabase: String => Task[Option[UserRecord]])
                  (request: FindUserByIdRequest): Task[FindUserByIdResponse] = {
    for {
      _ <- ensureValid(validateFindUserByIdRequest)(request)
      userRecord <- queryDatabase(request.userId)
      response = FindUserByIdResponse(userRecord.map(_.toProtobuf))
    } yield response
  }
}

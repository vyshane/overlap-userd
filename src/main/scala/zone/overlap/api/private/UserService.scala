// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api.`private`

import zone.overlap.api.`private`.user._

/**
  * @author shane.xie
  */
class UserService extends UserGrpcMonix.UserService {

  override def findUserById(request: FindUserByIdRequest) = ???

  override def findUsers(request: FindUsersRequest) = ???

  override def streamUsers(request: FindUsersRequest) = ???

  override def activateUser(request: ActivateUserRequest) = ???

  override def suspendUser(request: SuspendUserRequest) = ???

  override def deleteUser(request: DeleteUserRequest) = ???
}

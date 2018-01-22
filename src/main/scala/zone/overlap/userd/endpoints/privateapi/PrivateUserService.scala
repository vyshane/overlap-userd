// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.privateapi

import zone.overlap.privateapi.user._
import zone.overlap.userd.persistence.UserRepository

case class PrivateUserService(userRepository: UserRepository[_, _]) extends UserGrpcMonix.UserService {

  override def findUserById(request: FindUserByIdRequest) = Endpoints.findUserById(userRepository.findUserById)(request)

  override def findUsers(request: FindUsersRequest) = ???

  override def streamUsers(request: FindUsersRequest) = ???

  override def activateUser(request: ActivateUserRequest) = ???

  override def suspendUser(request: SuspendUserRequest) = ???

  override def deleteUser(request: DeleteUserRequest) = ???
}

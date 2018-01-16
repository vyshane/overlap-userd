// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}

class VerifyEmailEndpointSpec extends AsyncWordSpec with AsyncMockFactory with Matchers with RecoverMethods {

//  val createDexPassword = mockFunction[CreatePasswordReq, Task[CreatePasswordResp]]
//  createDexPassword
//    .expects(where { theRequest: CreatePasswordReq =>
//      import com.github.t3hnar.bcrypt._
//      theRequest.password.get.email == signUpRequest.email
//      theRequest.password.get.hash.toStringUtf8 == signUpRequest.password.bcrypt
//      theRequest.password.get.username == signUpRequest.email
//      theRequest.password.get.userId == newUserId
//    })
//    .returning(Task(CreatePasswordResp(false)))

//  "sent a valid request but the dex password creation fails" should {
//    "raise an abort error saying that the dex password could not be created" in {
//      val findUserByEmail = mockFunction[String, Option[UserRecord]]
//      findUserByEmail
//        .expects(*)
//        .returning(Option.empty)
//
//      val createUser = mockFunction[SignUpRequest, String]
//      createUser
//        .expects(*)
//        .returning(UUID.randomUUID().toString)
//
//      val sendNotification = mockFunction[UserSignedUp, Task[Unit]]
//      sendNotification.expects(*).never()
//
//      recoverToExceptionIf[StatusRuntimeException] {
//        SignUpEndpoint
//          .signUp(findUserByEmail, createUser, sendNotification, Clock.systemUTC())(randomSignUpRequest())
//          .runAsync
//      } map { error =>
//        error.getStatus.getCode shouldEqual Status.ABORTED.getCode
//        error.getMessage.contains("User already exists in auth service") shouldBe true
//      }
//    }
//  }
}

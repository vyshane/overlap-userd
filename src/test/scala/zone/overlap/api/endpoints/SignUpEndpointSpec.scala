// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api.endpoints

import java.util.UUID

import api.api.{CreatePasswordReq, CreatePasswordResp}
import com.github.javafaker.Faker
import io.grpc.{Status, StatusRuntimeException}
import monix.eval.Task
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}
import zone.overlap.TestUtils
import zone.overlap.api.user.SignUpRequest
import zone.overlap.internalapi.events.user.UserSignedUp
import zone.overlap.userd.persistence.UserRecord

class SignUpEndpointSpec extends AsyncWordSpec with AsyncMockFactory with Matchers with RecoverMethods {

  import monix.execution.Scheduler.Implicits.global
  private val faker = new Faker()

  "The signUp public endpoint" when {
    "sent an invalid request" should {
      "raise an error containing the validation errors" in {
        val findUserByEmail = mockFunction[String, Option[UserRecord]]

        val createUser = mockFunction[SignUpRequest, String]
        createUser.expects(*).never()

        val createDexPassword = mockFunction[CreatePasswordReq, Task[CreatePasswordResp]]
        createDexPassword.expects(*).never()

        val sendNotification = mockFunction[UserSignedUp, Task[Unit]]
        sendNotification.expects(*).never()

        recoverToExceptionIf[StatusRuntimeException] {
          SignUpEndpoint
            .signUp(findUserByEmail, createUser, createDexPassword, sendNotification)(SignUpRequest("", "", "", "abc"))
            .runAsync
        } map { error =>
          error.getMessage.contains("First name must be between 1 and 255 characters") shouldBe true
          error.getMessage.contains("Last name must be between 1 and 255 characters") shouldBe true
          error.getMessage.contains("Email address is required") shouldBe true
          error.getMessage.contains("Password must be at least 6 characters") shouldBe true
        }
      }
    }
    "sent a request with an invalid email address" should {
      "raise an error saying that a valid email address is required" in {
        val request = randomSignUpRequest().withEmail("invalid email address")
        recoverToExceptionIf[StatusRuntimeException] {
          SignUpEndpoint
            .signUp(
              mockFunction[String, Option[UserRecord]],
              mockFunction[SignUpRequest, String],
              mockFunction[CreatePasswordReq, Task[CreatePasswordResp]],
              mockFunction[UserSignedUp, Task[Unit]]
            )(request)
            .runAsync
        } map { error =>
          error.getMessage.contains("Email address is invalid") shouldBe true
        }
      }
    }
    "sent a request with an email address that is already taken" should {
      "raise an already exists error saying that the email address is taken" in {
        val findUserByEmail = mockFunction[String, Option[UserRecord]]
        findUserByEmail
          .expects(*)
          .returning(Option(TestUtils.randomUserRecord()))

        recoverToExceptionIf[StatusRuntimeException] {
          SignUpEndpoint
            .signUp(
              findUserByEmail,
              mockFunction[SignUpRequest, String],
              mockFunction[CreatePasswordReq, Task[CreatePasswordResp]],
              mockFunction[UserSignedUp, Task[Unit]]
            )(randomSignUpRequest())
            .runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.ALREADY_EXISTS.getCode
          error.getMessage.contains("Email address is already taken") shouldBe true
        }
      }
    }
    "sent a valid request but the dex password creation fails" should {
      "raise an abort error saying that the dex password could not be created" in {
        val findUserByEmail = mockFunction[String, Option[UserRecord]]
        findUserByEmail
          .expects(*)
          .returning(Option.empty)

        val createUser = mockFunction[SignUpRequest, String]
        createUser
          .expects(*)
          .returning(UUID.randomUUID().toString)

        val createDexPassword = mockFunction[CreatePasswordReq, Task[CreatePasswordResp]]
        createDexPassword
          .expects(*)
          .returning(Task(CreatePasswordResp(true)))

        val sendNotification = mockFunction[UserSignedUp, Task[Unit]]
        sendNotification.expects(*).never()

        recoverToExceptionIf[StatusRuntimeException] {
          SignUpEndpoint
            .signUp(findUserByEmail, createUser, createDexPassword, sendNotification)(randomSignUpRequest())
            .runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.ABORTED.getCode
          error.getMessage.contains("User already exists in auth service") shouldBe true
        }
      }
    }
    "sent a valid request" should {
      "create the user and send a user signed up notification" in {
        val signUpRequest = randomSignUpRequest()
        val newUserId = UUID.randomUUID().toString

        val findUserByEmail = mockFunction[String, Option[UserRecord]]
        findUserByEmail
          .expects(*)
          .returning(Option.empty)

        val createUser = mockFunction[SignUpRequest, String]
        createUser
          .expects(where { theRequest: SignUpRequest =>
            theRequest == signUpRequest
          })
          .returning(newUserId)

        val createDexPassword = mockFunction[CreatePasswordReq, Task[CreatePasswordResp]]
        createDexPassword
          .expects(where { theRequest: CreatePasswordReq =>
            import com.github.t3hnar.bcrypt._
            theRequest.password.get.email == signUpRequest.email
            theRequest.password.get.hash.toStringUtf8 == signUpRequest.password.bcrypt
            theRequest.password.get.username == signUpRequest.email
            theRequest.password.get.userId == newUserId
          })
          .returning(Task(CreatePasswordResp(false)))

        val sendNotification = mockFunction[UserSignedUp, Task[Unit]]
        sendNotification
          .expects(where { userSignedUp: UserSignedUp =>
            userSignedUp.userId == newUserId &&
            userSignedUp.firstName == signUpRequest.firstName &&
            userSignedUp.lastName == signUpRequest.lastName &&
            userSignedUp.email == signUpRequest.email &&
            userSignedUp.signedUp.isDefined
          })
          .returning(Task(()))

        SignUpEndpoint
          .signUp(findUserByEmail, createUser, createDexPassword, sendNotification)(signUpRequest)
          .runAsync
          .map { response =>
            response.userId shouldEqual newUserId
          }
      }
    }
  }

  private def randomSignUpRequest(): SignUpRequest = {
    val firstName = faker.name().firstName()
    SignUpRequest(firstName,
                  faker.name().lastName(),
                  faker.internet().emailAddress(firstName.toLowerCase),
                  faker.superhero().name())
  }
}

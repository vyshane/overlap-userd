// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api.endpoints

import java.util.UUID

import com.github.javafaker.Faker
import io.grpc.StatusRuntimeException
import monix.eval.Task
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}
import zone.overlap.api.user.SignUpRequest
import zone.overlap.internalapi.events.user.UserSignedUp

class SignUpEndpointSpec extends AsyncWordSpec with AsyncMockFactory with Matchers with RecoverMethods {

  import monix.execution.Scheduler.Implicits.global
  private val faker = new Faker()

  "The signUp public endpoint" when {
    "sent an invalid request" should {
      "raise an error containing the validation errors" in {
        val createUser = mockFunction[SignUpRequest, String]
        createUser.expects(*).never()
        val sendNotification = mockFunction[UserSignedUp, Task[Unit]]
        sendNotification.expects(*).never()

        recoverToExceptionIf[StatusRuntimeException] {
          SignUpEndpoint.signUp(createUser, sendNotification)(SignUpRequest("", "", "", "abc")).runAsync
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
            .signUp(mockFunction[SignUpRequest, String], mockFunction[UserSignedUp, Task[Unit]])(request)
            .runAsync
        } map { error =>
          error.getMessage.contains("Email address is invalid") shouldBe true
        }
      }
    }
    "sent a valid request" should {
      "create the user and send a user signed up notification" in {
        val signUpRequest = randomSignUpRequest()
        val newUserId = UUID.randomUUID().toString

        val createUser = mockFunction[SignUpRequest, String]
        createUser
          .expects(where { (theRequest: SignUpRequest) =>
            theRequest == signUpRequest
          })
          .returning(newUserId)

        val sendNotification = mockFunction[UserSignedUp, Task[Unit]]
        sendNotification
          .expects(where { (userSignedUp: UserSignedUp) =>
            userSignedUp.userId == newUserId &&
            userSignedUp.firstName == signUpRequest.firstName &&
            userSignedUp.lastName == signUpRequest.lastName &&
            userSignedUp.email == signUpRequest.email &&
            userSignedUp.signedUp.isDefined
          })
          .returning(Task(()))

        SignUpEndpoint
          .signUp(createUser, sendNotification)(signUpRequest)
          .runAsync
          .map { response =>
            response.userId shouldEqual newUserId
          }
      }
    }
  }

  private def randomSignUpRequest(): SignUpRequest = {
    val firstName = faker.name().firstName()
    SignUpRequest(firstName, faker.name().lastName(), faker.internet().emailAddress(firstName), faker.superhero().name())
  }
}

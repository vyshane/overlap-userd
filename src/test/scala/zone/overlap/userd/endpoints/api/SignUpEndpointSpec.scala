// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID

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

        val sendNotification = mockFunction[UserSignedUp, Task[Unit]]
        sendNotification.expects(*).never()

        recoverToExceptionIf[StatusRuntimeException] {
          SignUpEndpoint
            .signUp(findUserByEmail, createUser, sendNotification, Clock.systemUTC())(SignUpRequest("", "", "", "abc"))
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
              mockFunction[UserSignedUp, Task[Unit]],
              Clock.systemUTC()
            )(request)
            .runAsync
        } map { error =>
          error.getMessage.contains("Email address is invalid") shouldBe true
        }
      }
    }
    "sent a request with an email address that is already taken" should {
      "raise an invalid argument error saying that the email address is taken" in {
        val findUserByEmail = mockFunction[String, Option[UserRecord]]
        findUserByEmail
          .expects(*)
          .returning(Option(TestUtils.randomUserRecord()))

        recoverToExceptionIf[StatusRuntimeException] {
          SignUpEndpoint
            .signUp(
              findUserByEmail,
              mockFunction[SignUpRequest, String],
              mockFunction[UserSignedUp, Task[Unit]],
              Clock.systemUTC()
            )(randomSignUpRequest())
            .runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INVALID_ARGUMENT.getCode
          error.getMessage.contains("Email address is already taken") shouldBe true
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

        val instant = Instant.now()
        val clock = Clock.fixed(instant, ZoneId.systemDefault())

        val sendNotification = mockFunction[UserSignedUp, Task[Unit]]
        sendNotification
          .expects(where { userSignedUp: UserSignedUp =>
            userSignedUp.userId == newUserId &&
            userSignedUp.firstName == signUpRequest.firstName &&
            userSignedUp.lastName == signUpRequest.lastName &&
            userSignedUp.email == signUpRequest.email &&
            userSignedUp.signedUp.get.seconds == instant.getEpochSecond
            userSignedUp.signedUp.get.nanos == instant.getNano
          })
          .returning(Task(()))

        SignUpEndpoint
          .signUp(findUserByEmail, createUser, sendNotification, clock)(signUpRequest)
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

// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID

import com.github.javafaker.Faker
import com.google.protobuf.timestamp.Timestamp
import io.grpc.{Status, StatusRuntimeException}
import monix.eval.Task
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}
import zone.overlap.api.user.{SignUpRequest, SignUpResponse}
import zone.overlap.internalapi.events.user.UserSignedUp
import zone.overlap.userd.endpoints.api.SignUpEndpoint._
import zone.overlap.userd.persistence.UserRecord

class SignUpEndpointSpec extends AsyncWordSpec with AsyncMockFactory with Matchers with RecoverMethods {

  import monix.execution.Scheduler.Implicits.global
  private val faker = new Faker()

  def provide = afterWord("provide")

  // Unit tests
  "The signUp public endpoint" should provide {
    "a buildUserSignedUpMessage method" which {
      "creates a UserSignedUp message from the SignUpRequest" in {
        val instant = Instant.now()
        val clock = Clock.fixed(instant, ZoneId.systemDefault())
        val userId = UUID.randomUUID().toString
        val signUpRequest = randomSignUpRequest()

        buildUserSignedUpMessage(clock)(userId, signUpRequest) shouldEqual UserSignedUp(
          userId,
          signUpRequest.firstName,
          signUpRequest.lastName,
          signUpRequest.email,
          Option(Timestamp(instant.getEpochSecond, instant.getNano)))
      }
    }
  }

  // Verify side-effecting function calls
  "The signUp public endpoint" when {
    "sent an invalid request" should {
      "not create the user and should not send a UserSignedUp notification" in {
        val createUser = mockFunction[SignUpRequest, Task[String]]
        createUser.expects(*).never()

        val sendNotification = mockFunction[UserSignedUp, Task[Unit]]
        sendNotification.expects(*).never()

        recoverToExceptionIf[StatusRuntimeException] {
          SignUpEndpoint
            .signUp(_ => Task.now(None), createUser, sendNotification, Clock.systemUTC())(SignUpRequest())
            .runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INVALID_ARGUMENT.getCode
        }
      }
    }
    "sent a valid request" should {
      "create the user and send a user signed up notification" in {
        val signUpRequest = randomSignUpRequest()
        val newUserId = UUID.randomUUID().toString

        val findUserByEmail = mockFunction[String, Task[Option[UserRecord]]]
        findUserByEmail
          .expects(*)
          .returning(Task.now(None))

        val createUser = mockFunction[SignUpRequest, Task[String]]
        createUser
          .expects(where { theRequest: SignUpRequest =>
            theRequest == signUpRequest
          })
          .returning(Task.now(newUserId))

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
            response shouldEqual SignUpResponse()
          }
      }
    }
  }

  private def randomSignUpRequest(): SignUpRequest = {
    val firstName = faker.name().firstName()
    SignUpRequest(firstName,
                  faker.name().lastName(),
                  faker.internet().emailAddress(firstName.toLowerCase),
                  faker.lorem().characters(6).toLowerCase)
  }
}

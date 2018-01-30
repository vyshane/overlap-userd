// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import com.github.javafaker.Faker
import io.grpc.{Status, StatusRuntimeException}
import monix.eval.Task
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}
import zone.overlap.api.user.{UpdateInfoRequest, UpdateInfoResponse}
import zone.overlap.userd.authentication.UserContext

class UpdateInfoEndpointSpec extends AsyncWordSpec with AsyncMockFactory with Matchers with RecoverMethods {

  import monix.execution.Scheduler.Implicits.global
  private val faker = new Faker()

  // Verify side-effecting function calls
  "The updateInfo public endpoint" when {
    "sent an unauthenticated request" should {
      "raise an unauthenticated error and not update the user info" in {
        val ensureAuthenticated = () => Task.raiseError(Status.UNAUTHENTICATED.asRuntimeException())
        val updateUser = mockFunction[String, UpdateInfoRequest, Task[Unit]]
        updateUser.expects(*, *).never()

        recoverToExceptionIf[StatusRuntimeException] {
          UpdateInfoEndpoint
            .updateInfo(ensureAuthenticated, updateUser)(UpdateInfoRequest())
            .runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.UNAUTHENTICATED.getCode
        }
      }
    }
    "sent an invalid request" should {
      "raise an error containing the validation errors and not update the user info" in {
        val ensureAuthenticated = mockFunction[Task[UserContext]]
        ensureAuthenticated.expects().returning(Task.now(randomUserContext()))
        val updateUser = mockFunction[String, UpdateInfoRequest, Task[Unit]]
        updateUser.expects(*, *).never()

        recoverToExceptionIf[StatusRuntimeException] {
          UpdateInfoEndpoint
            .updateInfo(ensureAuthenticated, updateUser)(UpdateInfoRequest())
            .runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INVALID_ARGUMENT.getCode
          error.getMessage.contains("First name must be between 1 and 255 characters") shouldBe true
          error.getMessage.contains("Last name must be between 1 and 255 characters") shouldBe true
        }
      }
    }
    "sent a valid request" should {
      "update the user info and return an UpdateInfoResponse" in {
        val userContext = randomUserContext()
        val ensureAuthenticated = () => Task.now(userContext)
        val updateInfoRequest = UpdateInfoRequest(faker.name().firstName(), faker.name().lastName())

        val updateUser = mockFunction[String, UpdateInfoRequest, Task[Unit]]
        updateUser
          .expects(where { (email: String, request: UpdateInfoRequest) =>
            email == userContext.email && request == updateInfoRequest
          })
          .once()
          .returning(Task.now(()))

        UpdateInfoEndpoint
          .updateInfo(ensureAuthenticated, updateUser)(updateInfoRequest)
          .runAsync
          .map { response =>
            response shouldEqual UpdateInfoResponse()
          }
      }
    }
  }

  private def randomUserContext(): UserContext = {
    val firstName = faker.name().firstName()
    val lastName = faker.name().firstName()
    val email = faker.internet().emailAddress(firstName)
    UserContext(email, s"$firstName $lastName")
  }
}

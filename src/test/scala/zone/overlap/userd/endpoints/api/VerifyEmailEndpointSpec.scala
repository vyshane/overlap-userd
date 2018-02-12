// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import java.util.UUID

import com.coreos.dex.api.{CreatePasswordReq, CreatePasswordResp, Password}
import com.github.javafaker.Faker
import com.google.protobuf.ByteString
import io.grpc.{Status, StatusRuntimeException}
import monix.eval.Task
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}
import zone.overlap.TestUtils._
import zone.overlap.api.{VerifyEmailRequest, VerifyEmailResponse}
import zone.overlap.userd.endpoints.api.VerifyEmailEndpoint._
import zone.overlap.userd.persistence.UserRecord

class VerifyEmailEndpointSpec extends AsyncWordSpec with AsyncMockFactory with Matchers with RecoverMethods {

  import monix.execution.Scheduler.Implicits.global
  private val faker = new Faker()

  def provide = afterWord("provide")

  // Unit tests
  "The verifyEmail public endpoint" should provide {
    "an ensureUserExists method" which {
      "raises an error if the user does not exist" in {
        recoverToExceptionIf[StatusRuntimeException] {
          ensureUserFound(None).runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INVALID_ARGUMENT.getCode
          error.getMessage.contains("Invalid email verification code") shouldBe true
        }
      }
    }
    "passes the UserRecord through if the user exists " in {
      val user = randomPendingUserRecord()
      ensureUserFound(Option(user)).runAsync map { u =>
        u shouldEqual user
      }
    }
    "a buildCreatePasswordReq method" which {
      "builds a CreatePasswordReq" in {
        val userId = UUID.randomUUID().toString
        val username = faker.name().firstName()
        val email = faker.internet().emailAddress(username)
        val passwordHash = faker.lorem().word().toLowerCase()

        buildCreatePasswordReq(userId, username, email, passwordHash) shouldEqual CreatePasswordReq(
          Some(
            Password(email, ByteString.copyFromUtf8(passwordHash), username, userId)
          )
        )
      }
    }
    "an ensureDexUserCreated method" which {
      "raises an error if the user already exists in Dex" in {
        recoverToExceptionIf[StatusRuntimeException] {
          ensureDexUserCreated(CreatePasswordResp(true)).runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.ABORTED.getCode
          error.getMessage.contains("User already exists in auth service") shouldBe true
        }
      }
      "passes the CreatePasswordResp through if the Dex user was successfully created" in {
        ensureDexUserCreated(CreatePasswordResp(false)).runAsync map { resp =>
          resp shouldEqual CreatePasswordResp(false)
        }
      }
    }
    "a displayName method" which {
      "returns a formatted String made up of the user's first and last names" in {
        val user = randomPendingUserRecord()
        displayName(user) shouldEqual s"${user.firstName} ${user.lastName}"
      }
    }
  }

  // Verify side-effecting function calls
  "The verifyEmail public endpoint" when {
    "sent an unknown verification code" should {
      "not attempt to register the user with Dex and should not activate the user" in {
        val findUserByEmailVerificationCode = mockFunction[String, Task[Option[UserRecord]]]
        findUserByEmailVerificationCode.expects(*).returning(Task.now(None))

        val registerUserWithDex = mockFunction[CreatePasswordReq, Task[CreatePasswordResp]]
        registerUserWithDex.expects(*).never()

        val activateUser = mockFunction[String, Task[Unit]]
        activateUser.expects(*).never()

        recoverToExceptionIf[StatusRuntimeException] {
          handle(findUserByEmailVerificationCode, registerUserWithDex, activateUser)(VerifyEmailRequest()) runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INVALID_ARGUMENT.getCode
        }
      }
    }
    "sent a valid verification code but Dex registration fails because the user already exists in Dex" should {
      "not activate the user" in {
        val findUserByEmailVerificationCode = mockFunction[String, Task[Option[UserRecord]]]
        findUserByEmailVerificationCode.expects(*).returning(Task.now(Some(randomPendingUserRecord())))

        val registerUserWithDex = mockFunction[CreatePasswordReq, Task[CreatePasswordResp]]
        registerUserWithDex
          .expects(*)
          .once()
          .returning(Task.now(CreatePasswordResp(true)))

        val activateUser = mockFunction[String, Task[Unit]]
        activateUser.expects(*).never()

        recoverToExceptionIf[StatusRuntimeException] {
          handle(findUserByEmailVerificationCode, registerUserWithDex, activateUser)(VerifyEmailRequest()).runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.ABORTED.getCode
        }
      }
    }
    "sent a valid verification code and Dex registration is successful" should {
      "activate the user" in {
        val userRecord = randomPendingUserRecord()
        val findUserByEmailVerificationCode = mockFunction[String, Task[Option[UserRecord]]]
        findUserByEmailVerificationCode
          .expects(where { verificationCode: String =>
            verificationCode == userRecord.emailVerificationCode.get
          })
          .returning(Task.now(Some(userRecord)))

        val registerUserWithDex = mockFunction[CreatePasswordReq, Task[CreatePasswordResp]]
        registerUserWithDex
          .expects(where { createPasswordReq: CreatePasswordReq =>
            createPasswordReq.password.get.email == userRecord.email
          })
          .once()
          .returning(Task.now(CreatePasswordResp(false)))

        val activateUser = mockFunction[String, Task[Unit]]
        activateUser
          .expects(where { email: String =>
            email == userRecord.email
          })
          .once()
          .returning(Task.now(()))

        val request = VerifyEmailRequest(userRecord.emailVerificationCode.get)
        handle(findUserByEmailVerificationCode, registerUserWithDex, activateUser)(request).runAsync
          .map(_ shouldEqual VerifyEmailResponse())
      }
    }
  }
}

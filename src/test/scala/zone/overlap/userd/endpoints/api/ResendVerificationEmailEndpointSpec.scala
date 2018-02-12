// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import com.github.javafaker.Faker
import io.grpc.{Status, StatusRuntimeException}
import monix.eval.Task
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}
import zone.overlap.userd.endpoints.api.ResendVerificationEmailEndpoint._
import zone.overlap.TestUtils._
import zone.overlap.api.{ResendVerificationEmailRequest, ResendVerificationEmailResponse}
import zone.overlap.internalapi.{SendWelcomeEmailRequest, SendWelcomeEmailResponse}
import zone.overlap.userd.persistence.{Email, UserRecord}
import zone.overlap.userd.utils._

class ResendVerificationEmailEndpointSpec extends AsyncWordSpec with AsyncMockFactory with Matchers with RecoverMethods {

  import monix.execution.Scheduler.Implicits.global
  private val faker = new Faker()

  def provide = afterWord("provide")

  // Unit tests
  "The resendVerificationEmail public endpoint" should provide {
    "an ensurePendingEmailVerification method" which {
      "raises an error if the user is not pending email verification" in {
        recoverToExceptionIf[StatusRuntimeException] {
          ensurePendingEmailVerification(randomVerifiedUserRecord()).runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INVALID_ARGUMENT.getCode
        }
      }
    }
    "an ensurePendingEmailVerification method" which {
      "passes the user record through if the user is pending email verification" in {
        val user = randomPendingUserRecord()
        ensurePendingEmailVerification(user).runAsync map { u =>
          u shouldEqual user
        }
      }
    }
    "an assignEmailVerificationCodeIfNecessary method" which {
      "returns the existing email verification code if the user has one" in {
        val user = randomPendingUserRecord()
        val codeGenerator = mockFunction[String]
        codeGenerator.expects().never()
        val codeUpdater = mockFunction[Email, String, Task[Unit]]
        codeUpdater.expects(*, *).never()

        assignEmailVerificationCodeIfNecessary(codeGenerator, codeUpdater)(user).runAsync map { code =>
          code shouldEqual user.emailVerificationCode.get
        }
      }
    }
    "an assignEmailVerificationCodeIfNecessary method" which {
      "sets a new email verification code and returns it if the user has none" in {
        val user = randomVerifiedUserRecord()
        val newCode = randomUniqueCode()
        val codeGenerator = () => newCode

        val codeUpdater = mockFunction[Email, String, Task[Unit]]
        codeUpdater
          .expects(where { (email, code) =>
            email == user.email && code == newCode
          })
          .returning(Task.now(()))

        assignEmailVerificationCodeIfNecessary(codeGenerator, codeUpdater)(user).runAsync map { code =>
          code shouldEqual newCode
        }
      }
    }
    "a buildSendWelcomEmailRequest method" which {
      "returns an appropriate SendWelcomEmailRequest" in {
        val user = randomVerifiedUserRecord()
        val verificationCode = randomUniqueCode()
        val request = buildSendWelcomEmailRequest(user, verificationCode)

        request.firstName shouldEqual user.firstName
        request.lastName shouldEqual user.lastName
        request.email shouldEqual user.email
        // Request should use supplied verification code instead of any existing one
        request.verificationCode shouldEqual verificationCode
        request.verificationCode == user.emailVerificationCode shouldBe false
      }
    }
  }

  // Verify side-effecting function calls
  "The resendVerificationEmail public endpoint" when {
    "sent a valid request" should {
      "send a new verification email" in {
        val email = faker.internet().emailAddress()
        val request = ResendVerificationEmailRequest(email)
        val user = randomPendingUserRecord().copy(email = email)

        val findUserByEmail = mockFunction[Email, Task[Option[UserRecord]]]
        findUserByEmail
          .expects(where { e: Email =>
            e == email
          })
          .returning(Task.now(Some(user)))

        val codeUpdater = mockFunction[Email, String, Task[Unit]]
        codeUpdater.expects(*, *).never()

        val emailSender = mockFunction[SendWelcomeEmailRequest, Task[SendWelcomeEmailResponse]]
        emailSender
          .expects(where { request: SendWelcomeEmailRequest =>
            request == SendWelcomeEmailRequest(user.firstName, user.lastName, email, user.emailVerificationCode.get)
          })
          .returning(Task.now(SendWelcomeEmailResponse()))

        handle(findUserByEmail, codeUpdater, emailSender)(ResendVerificationEmailRequest(email)).runAsync
          .map(_ shouldEqual ResendVerificationEmailResponse())
      }
    }
  }
}

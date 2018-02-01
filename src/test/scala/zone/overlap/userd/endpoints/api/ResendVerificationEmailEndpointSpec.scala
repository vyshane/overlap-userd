// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import com.github.javafaker.Faker
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, FunSuite, Matchers, RecoverMethods}

class ResendVerificationEmailEndpointSpec extends AsyncWordSpec with AsyncMockFactory with Matchers with RecoverMethods {

  import monix.execution.Scheduler.Implicits.global
  private val faker = new Faker()

  def provide = afterWord("provide")

  // Unit tests
  "The resendVerificationEmail public endpoint" should provide {
    "an ensureUserExists method" which {
      "raises a Task error if the user does not exist" in (pending)
    }
    "an ensureUserExists method" which {
      "returns the user record wrapped in a Task if the user exists" in (pending)
    }
    "an ensurePendingEmailVerification method" which {
      "raises a Task error if the user is not pending email verification" in (pending)
    }
    "an ensurePendingEmailVerification method" which {
      "returns the user wrapped in a Task if the user is pending email verification" in (pending)
    }
    "an assignEmailVerificationCodeIfNecessary method" which {
      "returns the existing email verification code wrapped in a Task if the user has one" in (pending)
    }
    "an assignEmailVerificationCodeIfNecessary method" which {
      "sets a new email verification code and returns the code wrapped in a Task if the user has none" in (pending)
    }
    "a buildSendWelcomEmailRequest method" which {
      "returns an appropriate SendWelcomEmailRequest" in (pending)
    }
  }

  // Verify side-effecting function calls
  "The resendVerificationEmail public endpoint" when {
    "sent an invalid request" should {
      "not send a verification email" in (pending)
    }
    "sent a valid request" should {
      "send a new verification email" in (pending)
    }
  }
}

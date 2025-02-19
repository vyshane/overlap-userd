// Copyright 2018 Vy-Shane Xie Sin Fat

package zone.overlap.userd.validation

import java.util.UUID

import cats.data.Validated.Invalid
import cats.implicits._
import com.github.javafaker.Faker
import io.grpc.{Status, StatusRuntimeException}
import monix.eval.Task
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}
import zone.overlap.api.{ResendVerificationEmailRequest, SignUpRequest, UpdateInfoRequest}
import zone.overlap.privateapi.FindUserByIdRequest
import zone.overlap.userd.validation.RequestValidator._
import zone.overlap.TestUtils._
import zone.overlap.userd.persistence.UserRecord

class RequestValidatorSpec extends AsyncWordSpec with Matchers with RecoverMethods {

  import monix.execution.Scheduler.Implicits.global
  private val faker = new Faker()

  def provide = afterWord("provide")

  // Unit tests
  "RequestValidator" should provide {
    "an ensureExists function" which {
      "raises a Task error if none exists" in {
        recoverToExceptionIf[StatusRuntimeException] {
          ensureExists[String, UserRecord](_ => Task(None))("").runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.NOT_FOUND.getCode
          error.getMessage.contains("User record not found") shouldBe true
        }
      }
      "returns the value wrapped in a Task if it exists" in {
        val user = randomVerifiedUserRecord()
        ensureExists[String, UserRecord](_ => Task(Some(user)))("").runAsync map { u =>
          u shouldEqual user
        }
      }
    }
    "an ensureValid function" which {
      "raises a Task error if validation fails" in {
        recoverToExceptionIf[StatusRuntimeException] {
          ensureValid(validateFindUserByIdRequest)(FindUserByIdRequest()).runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INVALID_ARGUMENT.getCode
        }
      }
      "returns the value wrapped in a Task if validation succeeds" in {
        val request = FindUserByIdRequest(UUID.randomUUID().toString)
        ensureValid(validateFindUserByIdRequest)(request).runAsync map { r =>
          r shouldEqual request
        }
      }
    }
    "a validateFindUserByIdRequest function" which {
      "returns a validation error if validation fails" in {
        validateFindUserByIdRequest(FindUserByIdRequest()) shouldEqual UserIdIsRequired.invalidNel
      }
      "returns the validated value if validation succeeds" in {
        val request = FindUserByIdRequest(UUID.randomUUID().toString)
        validateFindUserByIdRequest(request) shouldEqual request.valid
      }
    }
    "a validateResendVerificationEmailRequest function" which {
      "returns a validation error if validation fails" in {
        validateResendVerificationEmailRequest(ResendVerificationEmailRequest()) should matchPattern {
          case Invalid(_) =>
        }
      }
      "returns the validated value if validation succeeds" in {
        val request = ResendVerificationEmailRequest(faker.internet().emailAddress())
        validateResendVerificationEmailRequest(request) shouldEqual request.valid
      }
    }
    "a validateSignUpRequest function" which {
      "returns the validation errors if validation fails" in {
        validateSignUpRequest(_ => true)(SignUpRequest()) should matchPattern {
          case Invalid(_) =>
        }
      }
      "returns the validated value if validation succeeds" in {
        val firstName = faker.name().firstName()
        val request = SignUpRequest(
          firstName,
          faker.name().lastName(),
          faker.internet().emailAddress(),
          faker.lorem().characters(8)
        )
        validateSignUpRequest(_ => false)(request) shouldEqual request.valid
      }
    }
    "a validateUpdateUserInfoRequest function" which {
      "returns a validation error if validation fails" in {
        validateUpdateUserInfoRequest(UpdateInfoRequest()) should matchPattern {
          case Invalid(_) =>
        }
      }
      "returns the validated value if validation succeeds" in {
        val request = UpdateInfoRequest(faker.name().firstName(), faker.name().lastName())
        validateUpdateUserInfoRequest(request) shouldEqual request.valid
      }
    }
    "a validateFirstName function" which {
      "returns a validation error if first name is empty" in {
        validateFirstName("") should matchPattern {
          case Invalid(_) =>
        }
      }
      "returns a validation error if first name is too long" in {
        validateFirstName(faker.lorem().characters(256)) should matchPattern {
          case Invalid(_) =>
        }
      }
      "returns the validated value if validation succeeds" in {
        val firstName = faker.name().firstName()
        validateFirstName(firstName) shouldEqual firstName.valid
      }
    }
    "a validateLastName function" which {
      "returns a validation error if validation fails" in {
        validateLastName("") should matchPattern {
          case Invalid(_) =>
        }
      }
      "returns a validation error if last name is too long" in {
        validateLastName(faker.lorem().characters(256)) should matchPattern {
          case Invalid(_) =>
        }
      }
      "returns the validated value if validation succeeds" in {
        val lastName = faker.name().lastName()
        validateFirstName(lastName) shouldEqual lastName.valid
      }
    }
    "a validateUniqueEmail function" which {
      "returns a validation error if the email is empty" in {
        validateUniqueEmail(_ => false)("") should matchPattern {
          case Invalid(_) =>
        }
      }
      "returns a validation error if the email format is invalid" in {
        validateUniqueEmail(_ => false)("bad@") should matchPattern {
          case Invalid(_) =>
        }
      }
      "returns a validation error if the email is already taken" in {
        validateUniqueEmail(_ => true)(faker.internet().emailAddress()) should matchPattern {
          case Invalid(_) =>
        }
      }
      "returns the validated value if validation succeeds" in {
        val email = faker.internet().emailAddress()
        validateUniqueEmail(_ => false)(email) shouldEqual email.valid
      }
    }
    "a validatePassword function" which {
      "returns a validation error if validation fails" in {
        validatePassword(faker.lorem().characters(5)) should matchPattern {
          case Invalid(_) =>
        }
      }
      "returns the validated value if validation succeeds" in {
        val password = faker.lorem().characters(6)
        validatePassword(password) shouldEqual password.valid
      }
    }
  }
}

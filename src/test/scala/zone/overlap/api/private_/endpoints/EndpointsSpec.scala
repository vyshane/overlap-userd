// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api.private_.endpoints

import java.time.Instant
import java.util.UUID

import com.github.javafaker.Faker
import monix.eval.Task
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}
import zone.overlap.api.private_.Endpoints._
import zone.overlap.api.private_.user.{FindUserByIdRequest, User}
import zone.overlap.userd.persistence.UserRecord

class EndpointsSpec extends AsyncWordSpec with Matchers with RecoverMethods {

  import monix.execution.Scheduler.Implicits.global

  private val faker = new Faker()

  "The findById private endpoint" when {
    "sent a request with no user id" should {
      "raise an error" in {
        recoverToExceptionIf[IllegalArgumentException] {
          findUserById(_ => Option.empty)(FindUserByIdRequest.defaultInstance).runAsync
        } map { error =>
          error.getMessage shouldEqual "User ID is required"
        }
      }
    }
    "sent a request with a user id that doesn't exist" should {
      "return a response with no user set" in {
        findUserById(_ => Option.empty)(FindUserByIdRequest(UUID.randomUUID().toString)).runAsync map { response =>
          response.user.isEmpty shouldBe true
        }
      }
    }
    "sent a request with a user id that exists" should {
      "return a response containing a user message that is correctly generated from the user record" in {
        val userRecord = randomUserRecord()
        def assertCorrectUserRecordConversion(record: UserRecord, proto: User) = {
          record.firstName shouldEqual proto.firstName
          record.lastName shouldEqual proto.lastName
          record.email shouldEqual proto.email
          record.signedUp.getEpochSecond shouldEqual proto.signedUp.get.seconds
          record.signedUp.getNano shouldEqual proto.signedUp.get.nanos
        }
        findUserById(_ => Option(userRecord))(FindUserByIdRequest(UUID.randomUUID().toString)).runAsync map { response =>
          assertCorrectUserRecordConversion(userRecord, response.user.get)
        }
      }
    }
  }

  private def randomUserRecord() = {
    val firstName = faker.name().firstName()
    UserRecord(UUID.randomUUID().toString,
               firstName,
               faker.name().lastName(),
               faker.internet().emailAddress(firstName),
               faker.hashCode().toString,
               Instant.now())
  }
}

// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api.private_.endpoints

import java.time.Instant
import java.util.UUID

import com.github.javafaker.Faker
import com.trueaccord.scalapb.{GeneratedEnum, GeneratedEnumCompanion}
import io.grpc.StatusRuntimeException
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}
import zone.overlap.api.private_.Endpoints._
import zone.overlap.api.private_.user.{FindUserByIdRequest, User, UserStatus}
import zone.overlap.userd.persistence.UserRecord

import scala.util.Random

class FindUserByIdSpec extends AsyncWordSpec with Matchers with RecoverMethods {

  import monix.execution.Scheduler.Implicits.global

  private val faker = new Faker()

  "The findUserById private endpoint" when {
    "sent a request with no user id" should {
      "raise an error" in {
        recoverToExceptionIf[StatusRuntimeException] {
          findUserById(_ => Option.empty)(FindUserByIdRequest.defaultInstance).runAsync
        } map { error =>
          error.getMessage.contains("User ID is required") shouldBe true
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
        val userId = UUID.randomUUID().toString
        val userRecord = randomUserRecord(userId)
        def findUser(userId: String): Option[UserRecord] = {
          userId shouldEqual userRecord.id
          Option(userRecord)
        }
        findUserById(findUser)(FindUserByIdRequest(userId)).runAsync map { response =>
          assertCorrectUserRecordConversion(userRecord, response.user.get)
        }
      }
    }
  }

  private def assertCorrectUserRecordConversion(record: UserRecord, protobuf: User) = {
    record.id shouldEqual protobuf.userId
    record.firstName shouldEqual protobuf.firstName
    record.lastName shouldEqual protobuf.lastName
    record.email shouldEqual protobuf.email
    record.signedUp.getEpochSecond shouldEqual protobuf.signedUp.get.seconds
    record.signedUp.getNano shouldEqual protobuf.signedUp.get.nanos
  }

  private def randomUserRecord(userId: String) = {
    val firstName = faker.name().firstName()
    UserRecord(
      userId,
      firstName,
      faker.name().lastName(),
      faker.internet().emailAddress(firstName),
      faker.hashCode().toString,
      randomEnum(UserStatus.enumCompanion),
      Instant.now()
    )
  }

  private def randomEnum[T <: GeneratedEnum](enumCompanion: GeneratedEnumCompanion[T]) = {
    enumCompanion.values.iterator.drop(Random.nextInt(enumCompanion.values.size)).next()
  }
}

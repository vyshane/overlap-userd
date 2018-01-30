// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.privateapi

import java.util.UUID

import com.github.javafaker.Faker
import io.grpc.StatusRuntimeException
import monix.eval.Task
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}
import zone.overlap.privateapi.user.{FindUserByIdRequest, User}
import zone.overlap.userd.persistence.UserRecord
import zone.overlap.TestUtils._
import zone.overlap.userd.endpoints.privateapi.FindUserByIdEndpoint._

class FindUserByIdSpec extends AsyncWordSpec with AsyncMockFactory with Matchers with RecoverMethods {

  import monix.execution.Scheduler.Implicits.global
  private val faker = new Faker()

  // Verify side-effecting function calls
  "The findUserById private endpoint" when {
    "sent a request with no user id" should {
      "raise an illegal argument error" in {
        val queryDatabase = mockFunction[String, Task[Option[UserRecord]]]
        queryDatabase.expects(*).never()

        recoverToExceptionIf[StatusRuntimeException] {
          findUserById(queryDatabase)(FindUserByIdRequest.defaultInstance).runAsync
        } map { error =>
          error.getMessage.contains("User ID is required") shouldBe true
        }
      }
    }
    "sent a request with a user id that doesn't exist" should {
      "return a response with no user set" in {
        val queryDatabase = mockFunction[String, Task[Option[UserRecord]]]
        queryDatabase
          .expects(*)
          .once()
          .returning(Task.now(None))

        findUserById(queryDatabase)(FindUserByIdRequest(UUID.randomUUID().toString)).runAsync map { response =>
          response.user.isEmpty shouldBe true
        }
      }
    }
    "sent a request with a user id that exists" should {
      "return a response containing a user message that is correctly generated from the user record" in {
        val userId = UUID.randomUUID().toString
        val userRecord = randomVerifiedUserRecord().copy(id = userId)
        val queryDatabase = mockFunction[String, Task[Option[UserRecord]]]
        queryDatabase
          .expects(*)
          .once()
          .returning(Task.now(Option(userRecord)))

        findUserById(queryDatabase)(FindUserByIdRequest(userId)).runAsync map { response =>
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
}

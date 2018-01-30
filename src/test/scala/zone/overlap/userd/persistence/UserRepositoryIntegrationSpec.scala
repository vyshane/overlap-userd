// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.persistence

import java.time.Instant
import java.util.UUID

import com.github.javafaker.Faker
import com.typesafe.config.ConfigFactory
import io.getquill.{H2JdbcContext, SnakeCase}
import org.scalatest._
import zone.overlap.privateapi.user.UserStatus
import zone.overlap.TestUtils._
import zone.overlap.api.user.{SignUpRequest, UpdateInfoRequest}
import zone.overlap.userd.utils._

/*
 * Integration tests for UserRepository
 * Uses H2 in-memory database
 */
class UserRepositoryIntegrationSpec
    extends WordSpec
    with Matchers
    with RecoverMethods
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  val config = ConfigFactory.load()
  val databaseMigrator = DatabaseMigrator(config)
  val databaseContext = new H2JdbcContext(SnakeCase, "database") with Encoders with Decoders with Quotes
  val userRepository = UserRepository(databaseContext)
  val faker = new Faker

  override def beforeEach(): Unit = {
    databaseMigrator.migrate()
  }

  override def afterEach(): Unit = {
    databaseMigrator.reset()
  }

  def provide = afterWord("provide")

  "The UserRepository" should provide {
    "a findUserById method" which {
      "returns an empty Option if the user does not exist" in {
        awaitResult(userRepository.findUserById(UUID.randomUUID().toString)) shouldEqual None
      }
      "returns the user if the user exists" in {
        val user = randomVerifiedUserRecord()
        awaitResult(userRepository.createUser(user))
        awaitResult(userRepository.findUserById(user.id)) shouldEqual Option(user)
      }
    }
    "a findUserByEmail method" which {
      "returns an empty Option if the user does not exist" in {
        awaitResult(userRepository.findUserByEmail(faker.internet().emailAddress())) shouldEqual None
      }
      "returns the user if the user exists" in {
        val user = randomVerifiedUserRecord()
        awaitResult(userRepository.createUser(user))
        awaitResult(userRepository.findUserByEmail(user.email)) shouldEqual Option(user)
      }
    }
    "a findUserPendingEmailVerification method" which {
      "returns an empty Option if the email verification code could not be found" in {
        val unknownEmail = faker.internet().emailAddress()
        awaitResult(userRepository.findUserPendingEmailVerification(unknownEmail)) shouldEqual None
      }
      "returns an empty Option if the user exists but is not pending email verification" in {
        val user = randomVerifiedUserRecord().copy(emailVerificationCode = Option(randomUniqueCode()))
        awaitResult(userRepository.createUser(user))
        awaitResult(userRepository.findUserPendingEmailVerification(user.emailVerificationCode.get)) shouldEqual None
      }
      "returns the user if a user with the email verification code was found and the user is pending email verification" in {
        val user = randomPendingUserRecord()
        awaitResult(userRepository.createUser(user))
        awaitResult(userRepository.findUserPendingEmailVerification(user.emailVerificationCode.get)) shouldEqual Option(user)
      }
    }
    "an activateUser method" which {
      "clears the email verification code and sets the user status to active" in {
        val user = randomPendingUserRecord()
        awaitResult(userRepository.createUser(user))
        awaitResult(userRepository.activateUser(user.email))
        val activatedUser = awaitResult(userRepository.findUserByEmail(user.email))
        activatedUser.get.status shouldEqual UserStatus.ACTIVE.name
        activatedUser.get.emailVerificationCode shouldEqual None
      }
    }
    "a createUser method" which {
      "creates a new user with pending email verification status" in {
        val firstName = faker.name().firstName()
        val request = SignUpRequest(
          firstName,
          faker.name().lastName(),
          faker.internet().emailAddress(firstName.toLowerCase()),
          faker.superhero().name()
        )
        val userId = awaitResult(userRepository.createUser(request))
        val userRecord = awaitResult(userRepository.findUserById(userId)).get
        userRecord.firstName shouldEqual request.firstName
        userRecord.lastName shouldEqual request.lastName
        userRecord.email shouldEqual request.email
        userRecord.emailVerificationCode.isEmpty shouldBe false
        userRecord.status shouldEqual UserStatus.PENDING_EMAIL_VERIFICATION.name
        userRecord.signedUp.compareTo(Instant.now) < 0 shouldBe true
      }
    }
    "an updateUser method" which {
      "updates an existing user" in {
        val user = randomVerifiedUserRecord()
        awaitResult(userRepository.createUser(user))
        // Update user
        val firstName = faker.name().firstName()
        val updateInfoRequest = UpdateInfoRequest(firstName, faker.name.lastName())
        awaitResult(userRepository.updateUser(user.email, updateInfoRequest))
        // Check that record was updated as expected
        val updatedUser = awaitResult(userRepository.findUserById(user.id))
        updatedUser.get.firstName shouldEqual updateInfoRequest.firstName
        updatedUser.get.lastName shouldEqual updateInfoRequest.lastName
      }
    }
    "an updateUserStatus method" which {
      "updates the user status" in {
        val user = randomPendingUserRecord()
        awaitResult(userRepository.createUser(user))
        awaitResult(userRepository.updateUserStatus(user.email, UserStatus.SUSPENDED))
        awaitResult(userRepository.findUserByEmail(user.email)).get.status shouldEqual UserStatus.SUSPENDED.name
      }
    }
    "a canQueryDatabase method" which {
      "returns true if we can query the users database table" in {
        userRepository.canQueryUsers() shouldBe true
      }
      "returns false if we cannot query the users database table" in {
        databaseMigrator.reset()
        userRepository.canQueryUsers() shouldBe false
      }
    }
  }
}

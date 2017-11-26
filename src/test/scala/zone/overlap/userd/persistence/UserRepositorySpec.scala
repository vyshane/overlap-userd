// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.persistence

import java.time.Instant
import java.util.UUID

import com.github.javafaker.Faker
import com.typesafe.config.ConfigFactory
import io.getquill.{H2JdbcContext, SnakeCase}
import org.scalatest._
import zone.overlap.api.private_.user.UserStatus
import zone.overlap.TestUtils._
import zone.overlap.api.user.SignUpRequest

class UserRepositorySpec extends WordSpec with Matchers with RecoverMethods with BeforeAndAfterAll with BeforeAndAfterEach {

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
        userRepository.findUserById(UUID.randomUUID().toString) shouldEqual Option.empty
      }
      "returns the user if the user exists" in {
        val user = randomUser()
        userRepository.saveUser(user)
        userRepository.findUserById(user.id) shouldEqual Option(user)
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
        val userId = userRepository.createUser(request)
        val userRecord = userRepository.findUserById(userId).get
        userRecord.firstName shouldEqual request.firstName
        userRecord.lastName shouldEqual request.lastName
        userRecord.email shouldEqual request.email
        import com.github.t3hnar.bcrypt._
        request.password.isBcrypted(userRecord.passwordHash) shouldBe true
        userRecord.status shouldEqual UserStatus.PENDING_EMAIL_VERIFICATION
        userRecord.signedUp.compareTo(Instant.now) < 0 shouldBe true
      }
    }
  }

  private def randomUser(): UserRecord = {
    randomUser(faker.gameOfThrones().house())
  }

  private def randomUser(password: String): UserRecord = {
    import com.github.t3hnar.bcrypt._
    val firstName = faker.name().firstName()
    UserRecord(
      UUID.randomUUID().toString,
      firstName,
      faker.name().lastName(),
      faker.internet().emailAddress(firstName.toLowerCase),
      password.bcrypt,
      randomEnum(UserStatus.enumCompanion),
      Instant.now()
    )
  }
}

// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.persistence

import java.util.UUID

import com.typesafe.config.ConfigFactory
import io.getquill.{H2JdbcContext, SnakeCase}
import org.scalatest._

class UserRepositorySpec extends WordSpec with Matchers with RecoverMethods with BeforeAndAfterAll with BeforeAndAfterEach {

  val config = ConfigFactory.load()
  val databaseMigrator = DatabaseMigrator(config)
  val databaseContext = new H2JdbcContext(SnakeCase, "database") with Encoders with Decoders with Quotes
  val userRepository = UserRepository(databaseContext)

  override def beforeAll(): Unit = {
    databaseMigrator.migrate()
  }

  override def afterAll(): Unit = {
    databaseMigrator.reset()
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
      "returns the user if the user exists" is (pending)
    }
  }
}

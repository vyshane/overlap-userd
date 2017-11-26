// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.api.endpoints

import com.github.javafaker.Faker
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}

class SignUpEndpointSpec extends AsyncWordSpec with Matchers with RecoverMethods {

  import monix.execution.Scheduler.Implicits.global

  private val faker = new Faker()

  "The signUp public endpoint" when {
    "sent a request containing an invalid first name" should {
      "raise an error" is (pending)
    }
    "sent a request containing an invalid last name" should {
      "raise an error" is (pending)
    }
    "sent a request containing an invalid email address" should {
      "raise an error" is (pending)
    }
    "sent a request containing an invalid password" should {
      "raise an error" is (pending)
    }
  }
}

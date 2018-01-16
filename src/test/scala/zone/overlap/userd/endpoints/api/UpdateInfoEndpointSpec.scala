// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints.api

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers, RecoverMethods}

class UpdateInfoEndpointSpec extends AsyncWordSpec with AsyncMockFactory with Matchers with RecoverMethods {

  "The updateInfo public endpoint" when {
    "sent an unauthenticated request" should {
      "raise an error with status unauthenticated" in (pending)
    }
    "sent an invalid request" should {
      "raise an error containing the validation errors" in (pending)
    }
    "sent a valid request" should {
      "update the user's info and return an update info response" in (pending)
    }
  }
}

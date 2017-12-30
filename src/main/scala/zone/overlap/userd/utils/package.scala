// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd

import java.util.UUID

package object utils {

  // TODO: Do we need to handle max String length for bycrypt encryption?
  def hashPassword(password: String): String = {
    import com.github.t3hnar.bcrypt._
    password.bcrypt
  }

  def randomVerificationCode(): String = UUID.randomUUID().toString.replace("-", "")
}

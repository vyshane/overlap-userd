// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd

import java.nio.ByteBuffer
import java.util.{Base64, UUID}

package object utils {

  // TODO: Do we need to handle max String length for bycrypt encryption?
  def hashPassword(password: String): String = {
    import com.github.t3hnar.bcrypt._
    password.bcrypt
  }

  // Generates a random base64 encoded UUID with the trailing == characters removed
  def randomUniqueCode(): String = {
    val uuid = UUID.randomUUID()
    val uuidBytes = ByteBuffer
      .wrap(new Array[Byte](16))
      .putLong(uuid.getMostSignificantBits)
      .putLong(uuid.getLeastSignificantBits)
      .array
    val encoder = Base64.getUrlEncoder()
    encoder.encodeToString(uuidBytes).substring(0, 22)
  }
}

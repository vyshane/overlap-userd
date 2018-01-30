// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.persistence

import java.time.Instant
import java.util.Date

import io.getquill.MappedEncoding
import zone.overlap.privateapi.user.UserStatus

// Common decoders
trait Decoders {
  implicit val instantDecoder = MappedEncoding[Date, Instant](date => date.toInstant)
}

// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.persistence

import java.time.Instant
import java.util.Date

import io.getquill.MappedEncoding

trait Encoders {
  implicit val instantEncoder = MappedEncoding[Instant, Date](instant => Date.from(instant))
}

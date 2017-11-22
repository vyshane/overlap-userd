// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.persistence

import java.time.Instant

import io.getquill.context.sql.SqlContext

trait Quotes {
  this: SqlContext[_, _] =>
  implicit class TimestampQuotes(left: Instant) {
    def >(right: Instant) = quote(infix"$left > $right".as[Boolean])
    def <(right: Instant) = quote(infix"$left < $right".as[Boolean])
    def >=(right: Instant) = quote(infix"$left >= $right".as[Boolean])
    def <=(right: Instant) = quote(infix"$left <= $right".as[Boolean])
  }
}

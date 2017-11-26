// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap

import com.trueaccord.scalapb.{GeneratedEnum, GeneratedEnumCompanion}

import scala.util.Random

object TestUtils {

  def randomEnum[T <: GeneratedEnum](enumCompanion: GeneratedEnumCompanion[T]) = {
    enumCompanion.values.iterator.drop(Random.nextInt(enumCompanion.values.size)).next()
  }
}

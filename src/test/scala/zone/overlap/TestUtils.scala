// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap

import java.time.Instant
import java.util.UUID

import com.github.javafaker.Faker
import com.trueaccord.scalapb.{GeneratedEnum, GeneratedEnumCompanion}
import zone.overlap.privateapi.user.UserStatus
import zone.overlap.userd.persistence.UserRecord

import scala.util.Random

object TestUtils {

  private val faker = new Faker

  def randomEnum[T <: GeneratedEnum](enumCompanion: GeneratedEnumCompanion[T]) = {
    enumCompanion.values.iterator.drop(Random.nextInt(enumCompanion.values.size)).next()
  }

  def randomUserRecord(): UserRecord = {
    randomUserRecord(faker.gameOfThrones().house())
  }

  def randomUserRecord(password: String): UserRecord = {
    val firstName = faker.name().firstName()
    UserRecord(
      UUID.randomUUID().toString,
      firstName,
      faker.name().lastName(),
      faker.internet().emailAddress(firstName.toLowerCase),
      randomEnum(UserStatus.enumCompanion),
      Instant.now()
    )
  }
}

// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap

import java.time.Instant
import java.util.UUID

import com.github.javafaker.Faker
import com.trueaccord.scalapb.{GeneratedEnum, GeneratedEnumCompanion}
import monix.eval.Task
import zone.overlap.privateapi.UserStatus
import zone.overlap.userd.persistence.UserRecord
import zone.overlap.userd.utils._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

object TestUtils {

  private val faker = new Faker

  import monix.execution.Scheduler.Implicits.global
  def awaitResult[T](task: Task[T]): T = Await.result(task.runAsync, 5 seconds)

  def randomEnum[T <: GeneratedEnum](enumCompanion: GeneratedEnumCompanion[T]): T = {
    enumCompanion.values.iterator.drop(Random.nextInt(enumCompanion.values.size)).next()
  }

  def randomEnumExcept[T <: GeneratedEnum](enumCompanion: GeneratedEnumCompanion[T])(except: T): Option[T] = {
    if (enumCompanion.values.size == 1) {
      // There is no enum value other than the one the caller doesn't want
      None
    } else {
      var enum: T = randomEnum(enumCompanion)
      while (enum == except) enum = randomEnum(enumCompanion)
      Option(enum)
    }
  }

  def randomVerifiedUserRecord(): UserRecord = {
    val firstName = faker.name().firstName()
    UserRecord(
      UUID.randomUUID().toString,
      firstName,
      faker.name().lastName(),
      faker.internet().emailAddress(firstName.toLowerCase),
      None,
      hashPassword(faker.gameOfThrones().quote().toLowerCase().replace(" ", "")),
      randomEnumExcept(UserStatus.enumCompanion)(UserStatus.PENDING_EMAIL_VERIFICATION).get.name,
      Instant.now()
    )
  }

  def randomPendingUserRecord(): UserRecord = {
    randomVerifiedUserRecord()
      .copy(status = UserStatus.PENDING_EMAIL_VERIFICATION.name, emailVerificationCode = Option(randomUniqueCode()))
  }
}

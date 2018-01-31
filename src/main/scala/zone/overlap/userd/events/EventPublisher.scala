// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.events

import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import cakesolutions.kafka.KafkaProducer.Conf
import com.typesafe.config.Config
import monix.eval.Task
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import org.slf4j.LoggerFactory
import zone.overlap.internalapi.events.UserSignedUp

import scala.util.{Failure, Success, Try}

case class EventPublisher(config: Config) {

  private val log = LoggerFactory.getLogger(this.getClass)

  val producer = KafkaProducer(
    Conf(new StringSerializer(), new ByteArraySerializer(), bootstrapServers = config.getString("kafka.bootstrapServers"))
  )

  def sendUserSignedUp(userSignedUp: UserSignedUp): Task[Unit] = {
    val record =
      KafkaProducerRecord(config.getString("kafka.topic.events.UserSignedUp"),
                          Some(userSignedUp.userId),
                          userSignedUp.toByteArray)
    Task(producer.sendWithCallback(record)(recordMetadata =>
      recordMetadata match {
        case Success(_)     => ()
        case Failure(error) => log.error("Error publishing UserSignedUp event", error)
    }))
  }

  def canQueryTopicPartitions(): Boolean = {
    Try(producer.partitionsFor(config.getString("kafka.topic.events.UserSignedUp"))).isSuccess
  }
}

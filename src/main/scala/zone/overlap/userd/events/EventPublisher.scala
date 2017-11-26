// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.events

import com.typesafe.config.Config
import monix.eval.Task
import org.slf4j.LoggerFactory
import zone.overlap.internalapi.events.user.UserSignedUp

case class EventPublisher(config: Config) {

  val log = LoggerFactory.getLogger(this.getClass)
  val bootstrapServers = config.getString("kafka.bootstrapServers")

  // TODO
  def sendUserSignedUp(userSignedUp: UserSignedUp): Task[Unit] = {
    // If there's an error while sending to topic, log it
    ???
  }
}

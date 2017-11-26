// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.events

import com.typesafe.config.Config
import monix.eval.Task
import zone.overlap.api.internal.events.user.UserSignedUp

case class EventPublisher(config: Config) {

  val bootstrapServers = config.getString("kafka.bootstrapServers")

  // TODO
  def sendUserSignedUp(userSignedUp: UserSignedUp): Task[Unit] = ???
}

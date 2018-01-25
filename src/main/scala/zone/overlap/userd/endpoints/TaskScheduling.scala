// Copyright 2018 Vy-Shane Xie Sin Fat

package zone.overlap.userd.endpoints

import monix.execution.Scheduler

trait TaskScheduling {
  lazy val ioScheduler = Scheduler.io(name = "userd-io")
}

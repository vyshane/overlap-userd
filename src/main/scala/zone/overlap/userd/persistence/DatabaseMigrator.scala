// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.persistence

import com.typesafe.config.Config
import org.flywaydb.core.Flyway

case class DatabaseMigrator(config: Config) {

  val flyway = new Flyway

  flyway.setDataSource(
    config.getString("database.dataSource.url"),
    config.getString("database.dataSource.user"),
    config.getString("database.dataSource.password")
  )

  def migrate(): Unit = {
    flyway.migrate()
  }

  def reset(): Unit = {
    flyway.clean()
  }
}

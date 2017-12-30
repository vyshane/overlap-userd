// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.monitoring

import org.jsoup.{Connection, HttpStatusException, Jsoup}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class StatusServerIntegration extends WordSpec with Matchers with BeforeAndAfterAll {

  val port = 3041
  val healthEndpoint = s"http://127.0.0.1:$port/health"
  val readinessEndpoint = s"http://127.0.0.1:$port/readiness"
  val statusServer = StatusServer(port).startAndIndicateNotReady()

  override def afterAll() = {
    statusServer.stop()
  }

  "The StatusServer" when {
    "it is started" should {
      "indicate healthy and not ready" in {
        Jsoup.connect(healthEndpoint).method(Connection.Method.GET).execute().statusCode() shouldEqual 200
        val error = intercept[HttpStatusException] {
          Jsoup.connect(readinessEndpoint).method(Connection.Method.GET).execute()
        }
        error.getStatusCode shouldEqual 503
      }
    }
    "it is asked to indicate ready" should {
      "indicate ready" in {
        statusServer.indicateReady()
        Jsoup.connect(readinessEndpoint).method(Connection.Method.GET).execute().statusCode() shouldEqual 200
      }
    }
    "the health checker returns false" should {
      "indicate unhealthy" in {
        statusServer.healthChecker = () => false
        val error = intercept[HttpStatusException] {
          Jsoup.connect(healthEndpoint).method(Connection.Method.GET).execute()
        }
        error.getStatusCode shouldEqual 500
      }
    }
    "it is asked for an inexistant endpoint" should {
      "return a 404 Not Found response" in {
        val error = intercept[HttpStatusException] {
          Jsoup.connect(s"${readinessEndpoint}-does-not-exist").method(Connection.Method.GET).execute()
        }
        error.getStatusCode shouldEqual 404
      }
    }
  }
}

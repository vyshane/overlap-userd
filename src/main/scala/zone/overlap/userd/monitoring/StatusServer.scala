// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.monitoring

import java.util.List
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.{ExecutorService, Executors}

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import org.slf4j.LoggerFactory
import zone.overlap.userd.monitoring.StatusServer.HealthChecker

/*
 * Serves application health and readiness status over HTTP
 */
class StatusServer(val port: Int, var healthChecker: HealthChecker = () => true) extends NanoHTTPD(port) {

  private val log = LoggerFactory.getLogger(this.getClass)
  var isReady = false

  def startAndIndicateNotReady() = {
    setAsyncRunner(BoundRunner(Executors.newFixedThreadPool(2)))
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
    log.info(s"Serving health and metrics on port $port")
    this
  }

  def indicateReady() = isReady = true

  override def serve(session: NanoHTTPD.IHTTPSession): Response = {
    session.getUri match {
      case "/health"    => serveHealth()
      case "/readiness" => serveReadiness()
      case _            => serveNotFound()
    }
  }

  private def serveHealth(): Response = {
    if (healthChecker.apply()) respond(Response.Status.OK, "Healthy")
    else respond(Response.Status.INTERNAL_ERROR, "Unhealthy")
  }

  private def serveReadiness(): Response = {
    if (isReady) respond(Response.Status.OK, "Ready")
    else respond(Response.Status.SERVICE_UNAVAILABLE, "Not Ready")
  }

  private def serveNotFound(): Response = {
    respond(Response.Status.NOT_FOUND, "404 Not Found")
  }

  private def respond(status: Response.Status, message: String): Response = {
    NanoHTTPD.newFixedLengthResponse(status, "text/html; charset=utf-8", message)
  }
}

object StatusServer {
  type HealthChecker = () => Boolean

  def apply(port: Int) = new StatusServer(port)
}

/*
 * The default threading strategy for NanoHTTPD launches a new thread every time. We override
 * that here so we can put an upper limit on the number of active threads using a thread pool.
 *
 * Adapted from Java implementation found at
 * https://github.com/NanoHttpd/nanohttpd/wiki/Example:-Using-a-ThreadPool
 */
case class BoundRunner(val executorService: ExecutorService) extends NanoHTTPD.AsyncRunner {

  private val running: List[NanoHTTPD#ClientHandler] = Collections.synchronizedList(new ArrayList)

  override def closeAll(): Unit = {
    import scala.collection.JavaConverters._
    // Use a copy of the list for concurrency
    for (clientHandler <- new ArrayList[NanoHTTPD#ClientHandler](running).asScala) {
      clientHandler.close()
    }
  }

  override def closed(clientHandler: NanoHTTPD#ClientHandler): Unit = {
    running.remove(clientHandler)
  }

  override def exec(clientHandler: NanoHTTPD#ClientHandler): Unit = {
    executorService.submit(clientHandler)
    running.add(clientHandler)
  }
}

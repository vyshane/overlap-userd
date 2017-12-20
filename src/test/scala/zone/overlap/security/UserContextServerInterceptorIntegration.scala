// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.security

import java.util.UUID

import com.coreos.dex.api.api.ApiGrpcMonix
import com.github.javafaker.Faker
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import io.grpc.ManagedChannelBuilder
import org.jsoup.Jsoup
import org.scalatest.{Matchers, WordSpec}
import zone.overlap.api.endpoints.SignUpEndpoint
import zone.overlap.docker.DockerDexService

import scala.concurrent.Await
import scala.concurrent.duration._

class UserContextServerInterceptorIntegration
    extends WordSpec
    with Matchers
    with DockerDexService {

  val faker = new Faker()
  import monix.execution.Scheduler.Implicits.global

  "UserContextServerInterceptor" when {
    "intercepting an unauthenticated call" should {
      "not provide a UserContext for the call" in {
        // TODO
      }
    }
    "intercepting an authenticated call" should {
      "set up the UserContext for the call" in {

        // Start a Dex Docker container and setup gRPC stub
        val dexHost = "localhost"
        val dexChannel = ManagedChannelBuilder.forAddress(dexHost, dexGrpcPort).usePlaintext(true).build()
        val dexStub = ApiGrpcMonix.stub(dexChannel)

        // Register a user account with Dex
        val email = faker.internet().emailAddress()
        val password = faker.lorem().word()
        val request = SignUpEndpoint.buildCreatePasswordReq(
          UUID.randomUUID().toString,
          password,
          email
        )
        Await.result(dexStub.createPassword(request).runAsync, 5 seconds)

        // Start a local HTTP server to provide the redirect URI callback endpoint
        // We'll use this endpoint to obtain the code to be exchanged for the access token
        val callbackPort = 5555
        val httpServer = new NanoHTTPD(callbackPort) {
          override def serve(session: NanoHTTPD.IHTTPSession): Response = {
            // TODO
            NanoHTTPD.newFixedLengthResponse("TODO")
          }
        } start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)

        // Start the OAuth2 flow
        // Access the Dex authorization endpoint and submit the login form
        val dexAuthorizationEndpoint = s"http://$dexHost:$dexHttpPort/dex/auth?" +
          "client_id=integration-test-app&" +
          s"redirect_uri=http://127.0.0.1:$callbackPort/callback&" +
          "response_type=code&" +
          "scope=openid&"

        Jsoup
          .connect(dexAuthorizationEndpoint)
          .data("login", email)
          .data("password", password)
          .post()

        // TODO

        // Exchange code for access token

        // Use the access token to make an RPC call

        // Check that the UserContext was set up correctly
      }
    }
  }
}

// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.authentication

import java.net.URI
import java.util.UUID

import com.coreos.dex.api.api.ApiGrpcMonix
import com.github.javafaker.Faker
import com.nimbusds.oauth2.sdk.auth.{ClientSecretBasic, Secret}
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk._
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import io.grpc.{ManagedChannelBuilder, ServerInterceptors, Status, StatusRuntimeException}
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.util.MutableHandlerRegistry
import monix.eval.Task
import org.jsoup.{Connection, Jsoup}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import zone.overlap.api.endpoints.SignUpEndpoint
import zone.overlap.docker.DockerDexService
import zone.overlap.api.user.{
  ChangePasswordRequest,
  DeleteAccountRequest,
  ResendVerificationEmailRequest,
  SignUpRequest,
  SignUpResponse,
  UpdateInfoRequest,
  UpdateInfoResponse,
  VerifyEmailRequest,
  UserGrpcMonix => PublicUserGrpcMonix
}
import zone.overlap.userd.authentication.{IdTokenCallCredentials, UserContextServerInterceptor}

import scala.concurrent.Await
import scala.concurrent.duration._

/*
 * This test launches a Dex Docker container and runs a full OAuth2 authorization code flow.
 * We use the id token obtained from Dex to perform a gRPC call that exercises adding the
 * credentials metadata using IdTokenCallCredentials and then extracting and validating
 * the id token using UserContextServerInterceptor. The end-to-end test also ensures that
 * we can fetch the JSON Web Key Set from Dex.
 */
class UserContextServerInterceptorIntegration extends WordSpec with Matchers with BeforeAndAfterAll with DockerDexService {

  val faker = new Faker()
  val serverName = s"userd-test-server-${UUID.randomUUID().toString}"
  val email = faker.internet().emailAddress()
  val clientId = "integration-test-app"
  val clientSecret = "ZXhhbXBsZS1hcHAtc2VjcmV0"
  val dexHost = "127.0.0.1"

  lazy val mockUserServiceChannel = InProcessChannelBuilder.forName(serverName).directExecutor().build()

  lazy val mockUserServiceStub: PublicUserGrpcMonix.UserServiceStub = {
    // Configure UserContextServerInterceptor to connect to Dex Docker container
    val config = ConfigFactory
      .defaultApplication()
      .withValue("oidc.issuer", ConfigValueFactory.fromAnyRef(s"http://$dexHost:$dexHttpPort/dex"))
      .withValue("oidc.jwksUrl", ConfigValueFactory.fromAnyRef(s"http://$dexHost:$dexHttpPort/dex/keys"))
      .withValue("oidc.clientId", ConfigValueFactory.fromAnyRef(clientId))
    val userContextServerInterceptor = new UserContextServerInterceptor(config)

    // Start gRPC server and set up a service to perform integration test
    val serviceRegistry = new MutableHandlerRegistry()
    val mockUserService = PublicUserGrpcMonix.bindService(MockUserService(), monix.execution.Scheduler.global)
    serviceRegistry.addService(ServerInterceptors.intercept(mockUserService, userContextServerInterceptor))
    val server = InProcessServerBuilder
      .forName(serverName)
      .fallbackHandlerRegistry(serviceRegistry)
      .directExecutor()
      .build()
      .start()
    val channel = InProcessChannelBuilder
      .forName(serverName)
      .directExecutor()
      .build()
    PublicUserGrpcMonix.stub(channel)
  }

  import monix.execution.Scheduler.Implicits.global

  override def afterAll() = {
    mockUserServiceChannel.shutdown()
  }

  "UserContextServerInterceptor" when {
    "intercepting an unauthenticated call" should {
      "not provide a UserContext for the call" in {
        val error = intercept[StatusRuntimeException] {
          // The updateInfo endpoint requires an authenticated call
          val updateInfo = mockUserServiceStub.updateInfo(UpdateInfoRequest()).runAsync
          Await.result(updateInfo, 5 seconds)
        }
        error.getStatus shouldEqual Status.UNAUTHENTICATED
      }
    }
    "intercepting an authenticated call" should {
      "set up a UserContext for the call" in {
        // Setup Dex gRPC stub
        val dexChannel = ManagedChannelBuilder.forAddress(dexHost, dexGrpcPort).usePlaintext(true).build()
        val dexStub = ApiGrpcMonix.stub(dexChannel)
        val userId = UUID.randomUUID().toString
        val password = faker.lorem().word()

        // Register a user account with Dex
        val request = SignUpEndpoint.buildCreatePasswordReq(userId, email, password)
        Await.result(dexStub.createPassword(request).runAsync, 5 seconds)

        // Start a local HTTP server to provide the callback endpoint
        val callbackPort = 5555
        val callbackHttpServer = CallbackHttpServer(callbackPort, dexHost, dexHttpPort)
        callbackHttpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)

        // Start of the OAuth2 flow
        val dexAuthorizationEndpoint = s"http://$dexHost:$dexHttpPort/dex/auth?" +
          s"client_id=$clientId&" +
          s"client_secret=$clientSecret&" +
          s"redirect_uri=http://127.0.0.1:$callbackPort/callback&" +
          "response_type=code&" +
          "scope=openid%20email%20profile%20groups"

        // Access the Dex authorization endpoint. Dex will redirect us to a login page.
        val loginUrl = Jsoup
          .connect(dexAuthorizationEndpoint)
          .method(Connection.Method.GET)
          .followRedirects(true)
          .execute()
          .url()

        // Submit the login form. Dex will redirect us to the grant access page.
        val grantAccessPage = Jsoup
          .connect(loginUrl.toString)
          .followRedirects(true)
          .data("login", email)
          .data("password", password)
          .post()

        // Submit grant access form
        // Dex will redirect us to our callback, which exchanges the authorization code for an access token
        // The callback endpoint prints the id token and we parse it from the page
        val req = grantAccessPage.select("input[name=req]").first().attr("value")
        val idToken = Jsoup
          .connect(grantAccessPage.location())
          .followRedirects(true)
          .method(Connection.Method.POST)
          .data("req", req)
          .data("approval", "approve")
          .post()
          .select("body")
          .first()
          .text()

        // Use the id token to make an authenticated gRPC call to the user service
        val updateInfo = mockUserServiceStub
          .withCallCredentials(new IdTokenCallCredentials(idToken))
          .updateInfo(UpdateInfoRequest())
          .runAsync

        // We do not expect a StatusRuntimeException with code Status.UNAUTHENTICATED
        noException should be thrownBy Await.result(updateInfo, 5 seconds)

        // Cleanup
        callbackHttpServer.stop()
        dexChannel.shutdown()
      }
    }
  }

  /*
   * HTTP server that implements the OAuth authorization code callback endpoint
   * and exchanges the authorization code for an access token using the Dex token endpoint
   */
  case class CallbackHttpServer(callbackPort: Int, dexHost: String, dexHttpPort: Int) extends NanoHTTPD(callbackPort) {

    // Callback endpoint prints the id token in the body of the HTML page
    override def serve(session: NanoHTTPD.IHTTPSession): Response = {
      val code = session.getParameters.get("code").get(0)
      val codeGrant =
        new AuthorizationCodeGrant(new AuthorizationCode(code), new URI(s"http://127.0.0.1:$callbackPort/callback"))
      val clientAuth =
        new ClientSecretBasic(new ClientID(clientId), new Secret("ZXhhbXBsZS1hcHAtc2VjcmV0"))
      val tokenRequest = new TokenRequest(new URI(s"http://$dexHost:$dexHttpPort/dex/token"), clientAuth, codeGrant)
      val tokenResponse = TokenResponse.parse(tokenRequest.toHTTPRequest().send())
      val idToken = tokenResponse
        .asInstanceOf[AccessTokenResponse]
        .getCustomParameters()
        .get("id_token")
        .asInstanceOf[String]
      NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html; charset=utf-8", idToken)
    }
  }

  /*
   * A mock user service implementation to verify that authenticated RPC calls have the
   * UserContext correctly set up
   */
  case class MockUserService() extends PublicUserGrpcMonix.UserService {

    override def updateInfo(request: UpdateInfoRequest): Task[UpdateInfoResponse] = {
      // Raise an error if the RPC call doesn't have access to the correct UserContext
      UserContextServerInterceptor
        .getUserContext()
        .filter(_.email == email)
        .map(_ => Task(UpdateInfoResponse()))
        .getOrElse(Task.raiseError(Status.UNAUTHENTICATED.asRuntimeException()))
    }

    override def signUp(request: SignUpRequest): Task[SignUpResponse] = ???
    override def verifyEmail(request: VerifyEmailRequest) = ???
    override def resendVerificationEmail(request: ResendVerificationEmailRequest) = ???
    override def changePassword(request: ChangePasswordRequest) = ???
    override def deleteAccount(request: DeleteAccountRequest) = ???
  }
}

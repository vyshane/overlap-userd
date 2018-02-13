// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.authentication

import java.net.URL
import java.time.Instant

import com.google.common.collect.Iterables
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jwt.JWTParser
import com.nimbusds.oauth2.sdk.id.{ClientID, Issuer}
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator
import com.typesafe.config.Config
import io.grpc._
import monix.eval.Task
import zone.overlap.userd.authentication.UserContextServerInterceptor.UserContextDecoder

import scala.util.Try

/*
 * Used to authenticate the user and, if valid, inject the UserContext into the gRPC call.
 * The UserContext then becomes available in gRPC service implementations that are added
 * to the server using the UserContextServerInterceptor.
 *
 * User sessions are propagated as JSON Web Tokens through the Authorization HTTP header
 * using the Bearer schema. JWTs are signed and verified using RS256.
 *
 * The public key used to verify the JWT is provided by a JWKS endpoint configured at
 * oicd.jwkstUrl. The key is downloaded and cached.
 */
case class UserContextServerInterceptor[A](config: Config, decoder: IDTokenClaimsSet => A) extends ServerInterceptor {

  private val validator = {
    val issuer = new Issuer(config.getString("oidc.issuer"))
    val clientID = new ClientID(config.getString("oidc.clientId"))
    val jwsAlgorithm = JWSAlgorithm.RS256
    val jwkSetUrl = new URL(config.getString("oidc.jwksUrl"))
    new IDTokenValidator(issuer, clientID, jwsAlgorithm, jwkSetUrl)
  }

  override def interceptCall[ReqT, RespT](call: ServerCall[ReqT, RespT],
                                          headers: Metadata,
                                          next: ServerCallHandler[ReqT, RespT]): ServerCall.Listener[ReqT] = {
    readBearerToken(headers) flatMap {
      decodeUserContext(_)
    } map { userContext =>
      val withUserContext = Context
        .current()
        .withValue(UserContextServerInterceptor.userContextKey, userContext)
      Contexts.interceptCall(withUserContext, call, headers, next)
    } getOrElse {
      next.startCall(call, headers)
    }
  }

  private def readBearerToken(headers: Metadata): Option[String] = {
    val authorizationHeaderKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
    try {
      Iterables
        .toArray(headers.getAll(authorizationHeaderKey), classOf[String])
        .find(header => header.startsWith("Bearer "))
        .map(header => header.replaceFirst("Bearer ", ""))
    } catch {
      case _: Exception => None
    }
  }

  private def decodeUserContext(jwt: String): Option[A] = {
    Try(validator.validate(JWTParser.parse(jwt), null))
      .map(decoder(_))
      .toOption
  }

  private def notExpired(expiryMillis: Long): Boolean = expiryMillis > Instant.now().toEpochMilli
}

object UserContextServerInterceptor {

  val userContextKey: Context.Key[Any] = Context.key("user_context")
  def getUserContext[A](): Option[A] = Option[A](userContextKey.get.asInstanceOf[A])

  def ensureAuthenticated[A](): Task[A] = {
    getUserContext[A]()
      .map(Task(_))
      .getOrElse(Task.raiseError(Status.UNAUTHENTICATED.asRuntimeException()))
  }
}

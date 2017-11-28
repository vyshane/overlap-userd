// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.security

import java.security.PublicKey
import java.time.Instant

import com.google.common.collect.Iterables
import io.grpc._
import pdi.jwt.Jwt
import pdi.jwt.JwtAlgorithm.RS256
import play.api.libs.json.Json

import scala.util.Try

class UserContextServerInterceptor(jwtVerificationKey: PublicKey) extends ServerInterceptor {

  override def interceptCall[ReqT, RespT](call: ServerCall[ReqT, RespT],
                                          headers: Metadata,
                                          next: ServerCallHandler[ReqT, RespT]): ServerCall.Listener[ReqT] = {
    readBearerToken(headers) flatMap {
      decodeUserContext(_, jwtVerificationKey)
    } map { userContext =>
      val withUserContext = Context
        .current()
        .withValue[UserContext](UserContextServerInterceptor.userContextKey, userContext)
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
      case _: Exception => Option.empty
    }
  }

  private def decodeUserContext(jwt: String, jwtVerificationKey: PublicKey): Option[UserContext] = {
    Jwt
      .decode(jwt, jwtVerificationKey, Seq(RS256))
      .flatMap(payload => Try(Json.parse(payload)))
      .toOption
      .filter(json => (json \ "exp").asOpt[Long].exists(notExpired))
      .flatMap(json => (json \ "sub").asOpt[String].map(UserContext(_)))
  }

  private def notExpired(expiryMillis: Long): Boolean = expiryMillis > Instant.now().toEpochMilli
}

object UserContextServerInterceptor {
  val userContextKey: Context.Key[UserContext] = Context.key("user_context")
}

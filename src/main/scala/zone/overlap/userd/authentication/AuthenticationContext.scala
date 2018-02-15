// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.authentication

import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet

case class AuthenticationContext(email: String, displayName: String)

object AuthenticationContext {

  def apply(claims: IDTokenClaimsSet): AuthenticationContext = {
    AuthenticationContext(claims.getStringClaim("email"), claims.getStringClaim("name"))
  }
}

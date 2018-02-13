// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.authentication

import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet

case class UserContext(email: String, displayName: String)

object UserContext {

  def apply(claims: IDTokenClaimsSet): UserContext = {
    UserContext(claims.getStringClaim("email"), claims.getStringClaim("name"))
  }
}

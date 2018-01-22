// Copyright 2017 Vy-Shane Xie Sin Fat

package zone.overlap.userd.authentication

import java.util.concurrent.Executor

import io.grpc.{Attributes, CallCredentials, Metadata, MethodDescriptor}

case class IdTokenCallCredentials(idToken: String) extends CallCredentials {

  override def applyRequestMetadata(method: MethodDescriptor[_, _],
                                    attributes: Attributes,
                                    appExecutor: Executor,
                                    applier: CallCredentials.MetadataApplier): Unit = {
    appExecutor.execute(() => {
      val headers = new Metadata()
      val authorizationHeaderKey =
        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
      headers.put(authorizationHeaderKey, "Bearer " + idToken)
      applier.apply(headers)
    })
  }

  override def thisUsesUnstableApi() = ()
}

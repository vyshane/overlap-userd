addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.2")
addSbtPlugin("com.lucidchart"   % "sbt-scalafmt"        % "1.11")

// gRPC and Protocol Buffers
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.9")
resolvers += Resolver.bintrayRepo("beyondthelines", "maven")
libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.0",
  "beyondthelines" %% "grpcmonixgenerator" % "0.0.1"
)
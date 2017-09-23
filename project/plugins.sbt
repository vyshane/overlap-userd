addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.1")
addSbtPlugin("com.lucidchart"   % "sbt-scalafmt"        % "1.11")

// gRPC and Protocol Buffers
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.11")
libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.3"

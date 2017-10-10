addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M10" cross CrossVersion.full)

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.2")
addSbtPlugin("com.lucidchart"   % "sbt-scalafmt"        % "1.11")

// gRPC and Protocol Buffers
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.12")
libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.6"

// Copyright 2017 Vy-Shane Xie Sin Fat

name := "userd"
version := sys.env.get("VERSION").getOrElse("1.0-SNAPSHOT")
description := "Overlap User Service"
organization := "zone.overlap"

// Docker image build
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)
import com.typesafe.sbt.packager.docker._
dockerBaseImage := "openjdk:8-jre-alpine"
dockerRepository := Some("asia.gcr.io/zone-overlap")
dockerUpdateLatest := true

// Compiler
scalaVersion := "2.12.4"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

// Dependencies
resolvers += Resolver.bintrayRepo("beyondthelines", "maven")
resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

libraryDependencies ++= Seq(
  // Configuration
  "com.typesafe" % "config" % "1.3.2",

  // Concurrency
  "io.monix" %% "monix" % "2.3.0",

  // gRPC and Protocol Buffers
  "io.grpc" % "grpc-netty" % "1.8.0",
  "io.grpc" % "grpc-stub" % "1.8.0",
  "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % "0.6.7",
  "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.6.7" % "protobuf",
  "beyondthelines" %% "grpcmonixruntime" % "0.0.6",
  "org.javassist" % "javassist" % "3.22.0-GA", // Improves Netty performance

  // Database
  "org.postgresql" % "postgresql" % "9.4.1212",
  "io.getquill" %% "quill-jdbc" % "2.3.1",
  "org.flywaydb" % "flyway-core" % "4.2.0", // Migrations

  // Kafka
  "net.cakesolutions" %% "scala-kafka-client" % "0.11.0.0",

  // Hashing
  "com.github.t3hnar" %% "scala-bcrypt" % "3.0",

  // OpenID Connect
  "com.nimbusds" % "oauth2-oidc-sdk" % "5.41.1",

  // Metrics
  "io.prometheus" % "simpleclient" % "0.1.0",
//  "io.prometheus" % "simpleclient_pushgateway" % "0.1.0",

  // Logging
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  // Testing
  "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test,
//  "com.whisk" %% "docker-testkit-scalatest" % "0.9.5" % Test,
  "com.whisk" %% "docker-testkit-scalatest" % "0.10.0-beta4" % Test,
  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.6" % Test,
  "org.nanohttpd" % "nanohttpd" % "2.2.0" % Test,
  "org.jsoup" % "jsoup" % "1.11.2" % Test,
  "com.github.javafaker" % "javafaker" % "0.14" % Test,

  // Testing - Database Integration
  "com.h2database" % "h2" % "1.4.196" % Test,

  // Testing - Kafka Integration
//  "net.cakesolutions" %% "scala-kafka-client-testkit" % "0.11.0.0" % Test,
//  "org.slf4j" % "log4j-over-slf4j" % "1.7.21" % Test,

// Misc
  "com.github.dwickern" %% "scala-nameof" % "1.0.3" % "provided"
)

// Protobuf/gRPC code generation
PB.targets in Compile := Seq(
  scalapb.gen(grpc = true, flatPackage = false) -> (sourceManaged in Compile).value,
  grpcmonix.generators.GrpcMonixGenerator() -> (sourceManaged in Compile).value
)

// Code formatting
scalafmtConfig := file(".scalafmt.conf")

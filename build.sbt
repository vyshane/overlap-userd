/*
 * Project metadata
 */
name := "userd"
version := sys.env.get("VERSION").getOrElse("1.0-SNAPSHOT")
description := "Overlap User Service"
organization := "zone.overlap"

/*
 * Docker image build
 */
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)
import com.typesafe.sbt.packager.docker._
dockerBaseImage := "openjdk:8-jre-alpine"
dockerRepository := Some("asia.gcr.io/zone-overlap")
dockerUpdateLatest := true

/*
 * Compiler
 */
scalaVersion := "2.12.3"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

/*
 * Dependencies
 */
libraryDependencies ++= Seq(
  "io.frees" %% "freestyle" % "0.3.1",
  "io.frees" %% "freestyle-async-monix" % "0.3.1",
  "io.frees" %% "freestyle-config" % "0.3.1",
  "io.frees" %% "freestyle-logging" % "0.3.1",
  // Configuration
  "com.typesafe" % "config" % "1.3.1",
  // Database
  "org.postgresql" % "postgresql" % "9.4.1208",
  "io.getquill" %% "quill-jdbc" % "2.0.0",
  // Concurrency
  "io.monix" %% "monix" % "2.3.0",
  // gRPC and Protocol Buffers
  "io.grpc" % "grpc-netty" % "1.6.1",
  "io.grpc" % "grpc-stub" % "1.6.1",
  "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % "0.6.6",
  "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.6.6" % "protobuf",
  "org.javassist" % "javassist" % "3.21.0-GA", // Improves Netty performance
  // Logging
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "ch.qos.logback" % "logback-core" % "1.1.8",
  "ch.qos.logback" % "logback-classic" % "1.1.8",
  // Metrics
  "io.prometheus" % "simpleclient" % "0.0.26",
//  "io.prometheus" % "simpleclient_pushgateway" % "0.0.26",
  "org.nanohttpd" % "nanohttpd" % "2.3.1",
  // Testing
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test,
  "com.github.javafaker" % "javafaker" % "0.13" % Test,
  "com.h2database" % "h2" % "1.4.192" % Test
)

/*
 * Protobuf/gRPC code generation
 */
PB.targets in Compile := Seq(
  scalapb.gen(grpc = true, flatPackage = true) -> (sourceManaged in Compile).value
)

/*
 * Code formatting
 */
scalafmtConfig := file(".scalafmt.conf")


import Configurations.{CompilerPlugin, Test}

name := "stubby"
description := "Barebones Stubbing Framework for Scala"
homepage := Some(url("https://github.com/scala-stubby/stubby"))
licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

organization := "org.scala_stubby"
organizationName := "The Stubby Contributors"
organizationHomepage := Some(url("https://github.com/scala-stubby"))
bintrayOrganization := Some("scala-stubby")

scalaVersion := "2.11.8"
crossScalaVersions := Seq("2.11.8", "2.12.0-M5")

scalacOptions += "-language:_"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scalamacros" % "paradise" % "2.1.0" % CompilerPlugin cross CrossVersion.full,
  "org.scalatest" %% "scalatest" % "3.0.0" % Test
)

publishMavenStyle := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
scmInfo := Some(
  ScmInfo(
    browseUrl = url("https://github.com/scala_stubby/stubby"),
    connection = "scm:git:https://github.com/scala_stubby/stubby.git",
    devConnection = Some("scm:git:https://github.com/scala_stubby/stubby.git")
  )
)
developers := List(
  Developer(
    id = "clhodapp",
    email = "clhodapp1@gmail.com",
    name = "Chris Hodapp",
    url = url("https://github.com/clhodapp")
  )
)

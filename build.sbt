
import Configurations.{CompilerPlugin, Test}

organization := "net.clhodapp"
name := "stubby"

scalaVersion := "2.11.8"

scalacOptions += "-language:_"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scalamacros" % "paradise" % "2.1.0" % CompilerPlugin cross CrossVersion.full,
  "org.scalatest" %% "scalatest" % "3.0.0" % Test
)

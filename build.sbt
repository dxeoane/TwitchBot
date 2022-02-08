ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "TwitchBot",
    assembly / mainClass := Some("App")
  )

libraryDependencies += "com.typesafe" % "config" % "1.4.2"

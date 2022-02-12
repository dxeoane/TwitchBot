ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "TwitchBot",
    assembly / mainClass := Some("App")
  )

libraryDependencies += "com.typesafe" % "config" % "1.4.2"

val akkaVersion = "2.6.18"
libraryDependencies ++= Seq(
  ("com.typesafe.akka" %% "akka-actor" % akkaVersion),
  ("com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test)
)

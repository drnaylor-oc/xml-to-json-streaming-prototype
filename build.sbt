ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
    name := "xml-to-json-stream"
  )

val AkkaVersion = "2.6.19"
libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-xml" % "3.0.4",
  "com.typesafe.akka"  %% "akka-stream"             % AkkaVersion,
  "com.typesafe.play"  %% "play-json"               % "2.9.2"     % Test,
  "com.typesafe.akka"  %% "akka-stream-testkit"     % AkkaVersion % Test,
  "org.scalatest"      %% "scalatest"               % "3.2.12"    % Test
)
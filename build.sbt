name := """simple-rest-scala"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.6"

lazy val root = project.in(file(".")).enablePlugins(PlayScala)

routesGenerator := InjectedRoutesGenerator

libraryDependencies ++= Seq(
  ws,
  "com.typesafe.akka" % "akka-actor_2.10" % "2.3.15"
)


fork in run := true
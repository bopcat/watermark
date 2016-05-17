name := "Watermark App"

val commonSettings = Seq(
  organization := "Hoerner & Hufe GmbH",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.10.6",
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")
)

lazy val playDependencies = Seq (
  ws,
  "com.typesafe.akka" % "akka-actor_2.10" % "2.3.15",
  "org.mongodb" % "casbah_2.10" % "3.1.1" pomOnly()
)

lazy val testDependencies = Seq (
  "org.scalatest" % "scalatest_2.10" % "2.2.6" % "test"
)


lazy val http = project.in(file("."))
  .settings(commonSettings:_*)
  .settings(libraryDependencies ++= testDependencies ++ playDependencies)
  .enablePlugins(PlayScala)

routesGenerator := InjectedRoutesGenerator



fork in run := true




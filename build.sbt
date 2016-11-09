name := """tangerine-clinic"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.apache.commons" % "commons-lang3" % "3.5",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)
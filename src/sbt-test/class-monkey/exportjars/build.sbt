scalaVersion := "2.10.6"
version := "1.0.0"

libraryDependencies ++= Seq(
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")

exportJars := true
fork := true


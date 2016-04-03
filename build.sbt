organization := "com.fommil"
name := "class-monkey"

version := "1.7.0-SNAPSHOT"

autoScalaLibrary := false
crossPaths := false

SonatypeSupport.sonatype("fommil", "class-monkey", SonatypeSupport.GPL2ce)

libraryDependencies ++= Seq(
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "junit" % "junit" % "4.12" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")

javacOptions ++= Seq(
  "-source", "1.7",
  "-target", "1.7",
  "-Xlint:all",
  "-Werror",
  "-Xlint:-options",
  "-Xlint:-path",
  "-Xlint:-processing"
)

cancelable in Global := true
fork := true

javaOptions in (Test, test) += s"-Dtest.resources.dir=${(resourceDirectory in Test).value}"

unmanagedBase in Test := baseDirectory.value / "lib-test"

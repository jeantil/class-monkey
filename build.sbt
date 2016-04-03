organization := "com.fommil"
name := "class-monkey"

version := "1.7.0-SNAPSHOT"

autoScalaLibrary := false
crossPaths := false

SonatypeSupport.sonatype("fommil", "class-monkey", SonatypeSupport.GPL3)

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

javacOptions ++= Seq(
  "-source", "1.7",
  "-target", "1.7",
  "-Xlint:all",
  //"-Werror",
  "-Xlint:-options",
  "-Xlint:-path",
  "-Xlint:-processing"
)

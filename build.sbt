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

javaOptions in Test += s"-Dtest.resources.dir=${(resourceDirectory in Test).value}"

javaOptions in Test <++= (packageBin in Compile) map { jar =>
  // needs timestamp to force recompile
  Seq("-javaagent:" + jar.getAbsolutePath, "-Ddummy=" + jar.lastModified)
}

packageOptions := Seq(
  Package.ManifestAttributes(
    "Premain-Class" -> "fommil.ClassMonkey",
    /*
     Boot-Class-Path must match the exact filename of the agent
     artefact. When doing a publishLocal, this is agent-assembly, when
     downloaded from Nexus, this is agent-{version}-assembly.jar. This
     logic sort-of catches this (but won't work if you do a
     publishLocal of a non-snapshot release). Presumably, this is a
     difference between ivy and maven style publishing.
     */
    "Boot-Class-Path" -> {
      if (version.value.contains("SNAP"))
        (artifactPath in (Compile, packageBin)).value.getName
      else
        s"agent-${version.value}-assembly.jar"
    },
    "Can-Redefine-Classes" -> "true",
    "Can-Retransform-Classes" -> "true",
    "Main-Class" -> "NotSuitableAsMain"
  )
)

cancelable in Global := true
fork := true

unmanagedBase in Test := baseDirectory.value / "lib-test"

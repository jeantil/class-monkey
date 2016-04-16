organization := "com.fommil"
name := "class-monkey"

version := "1.7.0-SNAPSHOT"

autoScalaLibrary := false
crossPaths := false

SonatypeSupport.sonatype("fommil", "class-monkey", SonatypeSupport.GPL2ce)

libraryDependencies ++= Seq(
  "org.ow2.asm" % "asm" % "5.1",
  "org.ow2.asm" % "asm-commons" % "5.1",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "junit" % "junit" % "4.12" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")

test in assembly := {}

javacOptions ++= Seq(
  "-source", "1.7",
  "-target", "1.7",
  "-Xlint:all",
  //"-Werror",
  "-Xlint:-options",
  "-Xlint:-path",
  "-Xlint:-processing",
  "-XDignore.symbol.file"
)

javaOptions in Test += s"-Dtest.resources.dir=${(resourceDirectory in Test).value}"

javaOptions in Test <++= (assembly) map { jar =>
  // needs timestamp to force recompile
  Seq("-javaagent:" + jar.getAbsolutePath, "-Ddummy=" + jar.lastModified)
}

packageOptions := Seq(
  Package.ManifestAttributes(
    "Premain-Class" -> "fommil.ClassMonkey",
    /* Boot-Class-Path must match the *exact* filename */
    "Boot-Class-Path" -> (assemblyJarName in assembly).value,
    "Can-Retransform-Classes" -> "true",
    "Main-Class" -> "NotSuitableAsMain"
  )
)

cancelable in Global := true
fork := true

unmanagedBase in Test := baseDirectory.value / "lib-test"

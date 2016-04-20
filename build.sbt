organization := "com.fommil"
name := "class-monkey"

version := "1.7.0-SNAPSHOT"

autoScalaLibrary := false
crossPaths := false

SonatypeSupport.sonatype("fommil", "class-monkey", SonatypeSupport.GPL3ce)

libraryDependencies ++= Seq(
  "org.ow2.asm" % "asm" % "5.1",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "junit" % "junit" % "4.12" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.6" % "test",
  "org.slf4j" % "jul-to-slf4j" % "1.7.19" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")

test in assembly := {}

javacOptions in compile ++= Seq(
  "-source", "1.7",
  "-target", "1.7",
  "-Xlint:all",
  //"-Werror",
  "-Xlint:-options",
  "-Xlint:-path",
  "-Xlint:-processing",
  "-XDignore.symbol.file"
)

javaOptions in Test ++= Seq(
  s"-Dtest.resources.dir=${(resourceDirectory in Test).value}",
  "-Dlogback.configurationFile=logback-test.xml"
)

// match what sonatype wants
assemblyJarName in assembly := { name.value + "-" + version.value + "-assembly.jar" }

javaOptions in Test ++= Seq(
  "-javaagent:" + assembly.value.getAbsolutePath
)

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

artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.copy(`classifier` = Some("assembly"))
}

addArtifact(artifact in (Compile, assembly), assembly)

// no point, it doesn't work because of the Manifest name
publishArtifact in Compile := false

scriptedSettings

scriptedLaunchOpts := Seq(
  "-XX:MaxPermSize=256m", "-Xss2m", "-Xmx256m",
  // WORKAROUND https://github.com/sbt/sbt/issues/2568
  "-javaagent:" + target.value.getAbsolutePath + "/" + name.value + "-" + version.value + "-assembly.jar"
)
scriptedBufferLog := false

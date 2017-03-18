organization := "com.fommil"

autoScalaLibrary := false
crossPaths := false

sonatypeGithub := ("fommil", "class-monkey")
licenses := Seq(("GPL 3.0 Classpath Exception" -> url("http://www.gnu.org/software/classpath/license.html")))

libraryDependencies ++= Seq(
  "org.ow2.asm" % "asm" % "5.2",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.slf4j" % "jul-to-slf4j" % "1.7.25" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")

test in assembly := {}

javacOptions in compile ++= Seq(
  "-source", "1.7",
  "-target", "1.7",
  "-XDignore.symbol.file"
)
javacOptions -= "-Werror" // must use internal APIs
javacOptions in doc ~= (_.filterNot(_.startsWith("-Xlint")))

javaOptions in Test ++= Seq(
  s"-Dtest.resources.dir=${(resourceDirectory in Test).value}"
)

// match what sonatype wants
assemblyJarName in assembly := { name.value + "-" + version.value + "-assembly.jar" }

assemblyShadeRules in assembly := Seq(
  ShadeRule.rename("org.objectweb.**" -> "fommil.asm.@1").inAll
)

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

unmanagedBase in Test := baseDirectory.value / "lib-test"

artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.copy(`classifier` = Some("assembly"))
}

addArtifact(artifact in (Compile, assembly), assembly)

// needed for scripted
resolvers += Resolver.typesafeIvyRepo("releases")
scriptedSettings
scriptedLaunchOpts := Seq(
  "-XX:MaxPermSize=256m", "-Xss2m", "-Xmx256m",
  // WORKAROUND https://github.com/sbt/sbt/issues/2568
  "-javaagent:" + target.value.getAbsolutePath + "/" + name.value + "-" + version.value + "-assembly.jar"
)
scriptedBufferLog := false

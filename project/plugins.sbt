// no more "Resolving..." messages
ivyLoggingLevel := UpdateLogging.Quiet

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

// BLOCKED https://github.com/alexarchambault/coursier/issues/236
//addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M11")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value

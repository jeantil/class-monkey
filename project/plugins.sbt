scalacOptions ++= Seq("-unchecked", "-deprecation")
ivyLoggingLevel := UpdateLogging.Quiet
addSbtPlugin("com.fommil" % "sbt-sensible" % "1.1.10")

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15-5")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value

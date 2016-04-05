There is a (reasonably) well-known bug in the JVM, that the URL classloader does not release its jars

- [Classloaders Keeping JarFiles Open](http://management-platform.blogspot.co.uk/2009/01/classloaders-keeping-jar-files-open.html)
- [apache-cxf/JDKBugHacks.java](https://github.com/ancoron/apache-cxf/blob/master/common/common/src/main/java/org/apache/cxf/common/logging/JDKBugHacks.java)

the various attempts to workaround the bug are limited.

This project provides a monkey patch to `java.net.URLClassLoader` which fixes the problem at its heart: by avoiding the use of persistent `JarFile`s which hold onto file handles.

## Scala

I personally encounter this bug in the context of Scala development:

- https://issues.scala-lang.org/browse/SI-9682
- https://github.com/sbt/sbt/issues/2496

and there is a somewhat related resource management bug in `scalac`:

- https://issues.scala-lang.org/browse/SI-9632

The classloader bug manifests as a memory leak in `sbt` on all platforms, but on Windows it is particularly bad because the OS uses read-write locks on file handles and the file cannot be deleted, moved or changed.

**A catastrophic consequence is that compilations silently fail on Windows when using `exportJars`.**

For some inexplicable reason, the references to the `JarFile` are *lost* when scanning classes for tests to run, and the GC can never reclaim the file handles. The only way to recover from this is to restart the `sbt` process.

## Licence

This is a derivative work of OpenJDK, and therefore must use the [GPLv2 with classpath exception](http://openjdk.java.net/legal/gplv2+ce.html). This is unfortunate because the [GPLv2 is incompatible with the GPLv3](http://www.gnu.org/licenses/old-licenses/gpl-2.0.html) (my preferred licence) and OpenJDK have not used the "or any later version" variant of the GPL which would have allowed it to be distributed under the GPLv3 or later.

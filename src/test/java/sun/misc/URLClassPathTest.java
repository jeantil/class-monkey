// Copyright 1999 Oracle and/or its affiliates
// Copyright 2016 Sam Halliday
// License: http://openjdk.java.net/legal/gplv2+ce.html
package sun.misc;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.*;
import com.sun.net.httpserver.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class URLClassPathTest {

    private static final Logger log = LoggerFactory.getLogger(URLClassPathTest.class);
    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    // NOTE: OpenJDK have tests under URLClassPath for bugs 4957669_and 5017871
    //       but neither bug report has anything to do with URLClassPath, nor
    //       does the test exercise any code paths involving URLClassPath, so
    //       it has been omitted.

    /**
     * URLConnection fails to find resources when given file:/dir/./subdir/ URL
     */
    @Test
    public void b4291009() throws Exception {
        File tempFile = File.createTempFile("foo", ".txt");
        tempFile.deleteOnExit();
        String basestr = tempFile.toURI().toString();
        basestr = basestr.substring(0, basestr.lastIndexOf("/")+1);
        URL url = new URL(basestr+"."+"/");

        ClassLoader cl = new URLClassLoader (new URL[] { url });
        Assert.assertNotNull(cl.getResource (tempFile.getName()));
    }

    /**
     * URLClassLoader fails to close handles to Jar files opened during getResource()
     */
    @Test
    public void b7183373() throws Exception {
        File dir = Files.createTempDirectory("URLClassPath").toFile();
        File f = new File("urlcl" + 1 + ".jar");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(f));

        zos.putNextEntry(new ZipEntry("TestResource"));
        byte[] b = "This is a test resource".getBytes();
        zos.write(b, 0, b.length);
        zos.close();

        // Load the file using cl.getResource()
        URLClassLoader cl = new URLClassLoader(new URL[] { new URL("jar:" +
                                                                   f.toURI().toURL() + "!/")}, null);
        cl.getResource("TestResource");

        // Close the class loader - this should free up all of its Closeables,
        // including the JAR file
        cl.close();

        // Try to delete the JAR file
        f.delete();

        Assert.assertFalse(f.exists());
    }
}

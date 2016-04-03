// Copyright 1999 Oracle and/or its affiliates
// Copyright 2016 Sam Halliday
// Licence: http://openjdk.java.net/legal/gplv2+ce.html
package javat.net;

import com.sun.net.httpserver.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.jar.*;
import java.util.zip.*;
import java.util.*;
import java.io.*;
import java.net.*;
import jdk.testlibrary.FileUtils;
import org.junit.*;

import static java.nio.file.StandardCopyOption.*;

public class URLClassLoaderTest {

    @Test
    public void b5077773() throws Exception {
        URLClassLoader loader =  new URLClassLoader (new URL[] {new URL("file:foo.jar")});
        /* This test will fail if the file below is removed from rt.jar */
        InputStream is = loader.getResourceAsStream ("javax/swing/text/rtf/charsets/mac.txt");
        Assert.assertNotNull(is);
        int c=0;
        while ((is.read()) != -1) {
            c++;
        }
        if (c == 26) /* size of bad file */  {
            throw new RuntimeException ("Wrong mac.txt file was loaded");
        }
    }

    @Test
    public void b6827999() throws Exception {
        URL[] urls = new URL[] {new URL("http://foobar.jar") };
        MyURLClassLoader ucl = new MyURLClassLoader(urls);

        ucl.addURL(new URL("http://foo/bar.jar"));
        urls = ucl.getURLs();

        if (urls.length != 2)
            throw new RuntimeException("Failed:(1)");
        ucl.close();

        ucl.addURL(new URL("http://foo.bar/bar.jar"));

        if (ucl.getURLs().length != 2) {
            throw new RuntimeException("Failed:(2)");
        }
    }

    static class MyURLClassLoader extends URLClassLoader {
        public MyURLClassLoader(URL[] urls) {
            super(urls);
        }
        public void addURL(URL url) {
            super.addURL(url);
        }
    }

    /**
     * URLClassLoader.close() apparently not working for JAR URLs on Windows
     */
    @Test
    public void b6896088() throws Exception {
        File tmp = Files.createTempDirectory("URLClassLoader").toFile();
        File orig = new File(System.getProperty("test.resources.dir") + "/foo.jar");
        File jarf = new File (tmp, "foo.jar");

        Files.copy(orig.toPath(), jarf.toPath());

        String jarName = (jarf.toURI()).toString();
        URL url = new URL("jar", "", jarName + "!/");

        URLClassLoader loader = new URLClassLoader(new URL[]{url});
        Class<?> c = loader.loadClass("Foo");

        loader.close();
        jarf.delete();

        Assert.assertFalse(jarf.exists());
    }

    /**
     * ISE "zip file closed" from JarURLConnection.getInputStream on JDK 7 when !useCaches.
     */
    @Test
    public void b7050028() throws Exception {
        URLConnection conn = getClass().getResource("/javat/net/URLClassLoaderTest.class").openConnection();
        int len = conn.getContentLength();

        byte[] data = new byte[len];
        InputStream is = conn.getInputStream();
        is.read(data);
        is.close();
        conn.setDefaultUseCaches(false);
        File jar = File.createTempFile("URLClassLoaderTest", ".jar");
        jar.deleteOnExit();
        OutputStream os = new FileOutputStream(jar);
        ZipOutputStream zos = new ZipOutputStream(os);
        ZipEntry ze = new ZipEntry("javat/net/URLClassLoaderTest.class");
        ze.setMethod(ZipEntry.STORED);
        ze.setSize(len);
        CRC32 crc = new CRC32();
        crc.update(data);
        ze.setCrc(crc.getValue());
        zos.putNextEntry(ze);
        zos.write(data, 0, len);
        zos.closeEntry();
        zos.finish();
        zos.close();
        os.close();
        URLClassLoader loader = new URLClassLoader(new URL[] {new URL("jar:" + jar.toURI() + "!/")}, ClassLoader.getSystemClassLoader().getParent());
        loader.loadClass("javat.net.URLClassLoaderTest");
    }

    /**
     * 6460701 : URLClassLoader:addURL RI behavior inconsistent with a spec in case duplicate URLs
     * 6431651 : No description for addURL(URL url) method of URLClassLoader class in case null url
     */
    @Test
    public void b6460701_b6431651() throws Exception {
        URL[] urls = new URL[] {new URL("http://foobar.jar") };
        MyURLClassLoader ucl = new MyURLClassLoader(urls);

        ucl.addURL(null);
        ucl.addURL(new URL("http://foobar.jar"));
        ucl.addURL(null);
        ucl.addURL(new URL("http://foobar.jar"));
        ucl.addURL(null);
        ucl.addURL(new URL("http://foobar.jar"));

        urls = ucl.getURLs();

        if (urls.length != 1)
            throw new RuntimeException(
                                       "Failed: There should only be 1 url in the list of search URLs");

        URL url;
        for (int i=0; i<urls.length; i++) {
            url = urls[i];
            if (url == null || !url.equals(new URL("http://foobar.jar")))
                throw new RuntimeException(
                                           "Failed: The url should not be null and should be http://foobar.jar");

        }
    }

    /**
     * FileNotFoundException when loading bogus class
     */
    @Test
    public void b4151665() throws Exception {
        boolean error = true;

        // Start a dummy server to return 404
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        HttpHandler handler = new HttpHandler() {
                public void handle(HttpExchange t) throws IOException {
                    InputStream is = t.getRequestBody();
                    while (is.read() != -1);
                    t.sendResponseHeaders (404, -1);
                    t.close();
                }
            };
        server.createContext("/", handler);
        server.start();

        // Client request
        try {
            URL url = new URL("http://localhost:" + server.getAddress().getPort());
            String name = "foo.bar.Baz";
            ClassLoader loader = new URLClassLoader(new URL[] { url });
            Class<?> c = loader.loadClass(name);
        } catch (ClassNotFoundException ex) {
            error = false;
        } finally {
            server.stop(0);
        }
        if (error)
            throw new RuntimeException("No ClassNotFoundException generated");
    }

    private static String nameToClassName(String key) {
        String key2 = key.replace('/', File.separatorChar);
        int li = key2.lastIndexOf(".class");
        key2 = key2.substring(0, li);
        return key2;
    }

    private static URL getUrl(File file) throws Exception {
        String name;
        try {
            name = file.getCanonicalPath();
        } catch (IOException e) {
            name = file.getAbsolutePath();
        }
        name = name.replace(File.separatorChar, '/');
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        return new URL( "file:" + name);
    }

    /**
     * @author Benjamin Renaud
     * @summary check that URLClassLoader correctly interprets Class-Path
     *
     * This test ensures that a manually constructed URLClassLoader is
     * able to:
     *
     * 1. load a class
     * 2. resolve Class-Path dependencies
     * 3. have that class use a dependent class in a different JAR file
     */
    @Test
    public void b4110602() throws Exception {
        File jar = new File(System.getProperty("test.resources.dir") + "/class_path_test.jar");
        Assert.assertTrue(jar.length() > 0);

        JarFile jarFile = new JarFile(jar);

        URL url = getUrl(jar);

        URLClassLoader ucl = new URLClassLoader(new URL[] { url });

        Manifest manifest = jarFile.getManifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        Map<?, ?> map = manifest.getEntries();

        Iterator<?> it = map.entrySet().iterator();
        Class<?> clazz = null;

        while (it.hasNext()) {
            Map.Entry<?, ?> e = (Map.Entry)it.next();
            Attributes a = (Attributes)e.getValue();

            Attributes.Name an = new Attributes.Name("Class-Path");
            if (a.containsKey(an)) {
                String val = a.getValue(an);
            }

            if (a.containsKey(new Attributes.Name("Java-Bean"))) {
                String beanClassName = nameToClassName((String)e.getKey());
                clazz = ucl.loadClass(beanClassName);
                if (clazz != null) {
                    clazz.newInstance();
                }
            }
        }
    }

    /**
     * Regression test for URLClassLoader getURLs() and addURL() methods.
     * See RFE 4102580: Need URLClassLoader.getURLs() method
     */
    @Test
    public void b4102580() throws Exception {
        MyURLClassLoader ucl =
            new MyURLClassLoader(new URL[] { new File(".").toURI().toURL() });
        URL u = ucl.getResource("/javat/net/URLClassLoaderTest.java");
        File file = new File(System.getProperty("test.resources.dir") + "/class_path_test.jar");
        ucl.addURL(file.toURI().toURL());
        Class<?> c = ucl.loadClass("Foo");
    }

    /**
     * Check that URLClassLoader doesn't create excessive http connections
     */
    @Test
    public void b4636331() throws Exception {
        boolean failed = false;

        MyHttpServer svr = MyHttpServer.create();

        URL urls[] =
            { new URL("http://localhost:" + svr.port() + "/dir1/"),
              new URL("http://localhost:" + svr.port() + "/dir2/") };
        URLClassLoader cl = new URLClassLoader(urls);

        // Test 1 - check that getResource does single HEAD request
        svr.counters().reset();
        URL url = cl.getResource("foo.gif");

        if (svr.counters().getCount() > 0 ||
            svr.counters().headCount() > 1) {
            failed = true;
        }

        // Test 2 - check that getResourceAsStream does at most
        //          one GET request
        svr.counters().reset();
        InputStream in = cl.getResourceAsStream("foo2.gif");
        in.close();
        if (svr.counters().getCount() > 1) {
            failed = true;
        }

        // Test 3 - check that getResources only does HEAD requests
        svr.counters().reset();
        Enumeration<?> e = cl.getResources("foos.gif");
        try {
            for (;;) {
                e.nextElement();
            }
        } catch (NoSuchElementException exc) { }
        if (svr.counters().getCount() > 1) {
            failed = true;
        }

        if (failed) {
            throw new Exception("Excessive http connections established - Test failed");
        }
    }

    /**
     * Test that URLClassLoader public constructors and factory
     * methods throw NullPointerException when appropriate.
     *
     * Tests whether URLClassLoader public constructors and factory
     * methods throw appropriate NullPointerExceptions for 1) a null
     * URL array parameter, and 2) a non-null URL array containing a
     * null element.
     */
    @Test
    public void b7179567() throws Exception {
        String path = "jar:file:"
            + System.getProperty("test.resources.dir") + "/class_path_test.jar"
            + "!/Foo.class";

        URL validURL = new URL(path);
        URL[] validURLArray = new URL[] { validURL, validURL };
        URL[] invalidURLArray = new URL[] { validURL, null };

        int failures = 0;
        URLClassLoader loader;

        try {
            loader = new URLClassLoader(validURLArray);
        } catch (Throwable t) {
            failures++;
        }
        try {
            loader = new URLClassLoader(null);
            failures++;
        } catch (NullPointerException e) {
            // expected
        }
        // This section should be uncommented if 8026517 is fixed.
        //        try {
        //            loader = new URLClassLoader(invalidURLArray);
        //            failures++;
        //        } catch (NullPointerException e) {
        //            // expected
        //        }

        try {
            loader = new URLClassLoader(validURLArray, null);
        } catch (Throwable t) {
            failures++;
        }
        try {
            loader = new URLClassLoader(null, null);
            failures++;
        } catch (NullPointerException e) {
            // expected
        }
        // This section should be uncommented if 8026517 is fixed.
        //        try {
        //            loader = new URLClassLoader(invalidURLArray, null);
        //            failures++;
        //        } catch (NullPointerException e) {
        //            // expected
        //        }

        try {
            loader = new URLClassLoader(validURLArray, null, null);
        } catch (Throwable t) {
            failures++;
        }
        try {
            loader = new URLClassLoader(null, null, null);
            failures++;
        } catch (NullPointerException e) {
            // expected
        }
        // This section should be uncommented if 8026517 is fixed.
        //        try {
        //            loader = new URLClassLoader(invalidURLArray, null, null);
        //            failures++;
        //        } catch (NullPointerException e) {
        //            // expected
        //        }

        try {
            loader = URLClassLoader.newInstance(validURLArray);
        } catch (Throwable t) {
            failures++;
        }
        try {
            loader = URLClassLoader.newInstance(null);
            failures++;
        } catch (NullPointerException e) {
            // expected
        }
        // This section should be uncommented if 8026517 is fixed.
        //        try {
        //            loader = URLClassLoader.newInstance(invalidURLArray);
        //            failures++;
        //        } catch (NullPointerException e) {
        //            // expected
        //        }

        try {
            loader = URLClassLoader.newInstance(validURLArray, null);
        } catch (Throwable t) {
            failures++;
        }
        try {
            loader = URLClassLoader.newInstance(null, null);
            failures++;
        } catch (NullPointerException e) {
            // expected
        }
        // This section should be uncommented if 8026517 is fixed.
        //        try {
        //            loader = URLClassLoader.newInstance(invalidURLArray, null);
        //            failures++;
        //        } catch (NullPointerException e) {
        //            // expected
        //        }

        if (failures != 0) {
            throw new Exception("URLClassLoader NullURLTest had "+failures+" failures!");
        }
    }

    @Test
    public void b4128326_b4127567() throws Exception {
        String path = "jar:file:"
            + System.getProperty("test.resources.dir") + "/class_path_test.jar"
            + "!/Foo.java";

        URL aURL = new URL(path);
        URL testURL = new URL(aURL, "foo/../Foo.java");

        InputStream in = testURL.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String firstLine = reader.readLine();
        if (!firstLine.startsWith("public class Foo {"))
            throw new RuntimeException("Jar or File parsing failure.");
    }

    /**
     * URL-downloaded jar files can consume all available file descriptors
     *
     * needs two jar files test1.jar and test2.jar with following structure
     *
     * com/foo/TestClass
     * com/foo/TestClass1
     * com/foo/Resource1
     * com/foo/Resource2
     *
     * and a directory hierarchy with the same structure/contents
     */
    @Test
    public void b4167874() throws Exception {
        String orig = System.getProperty("test.resources.dir") + "/b4167874";

        File tmp = Files.createTempDirectory("URLClassLoaderTest").toFile();
        copyDir(orig, tmp.getPath() + "/work");

        String workdir = tmp.getPath() + "/work/";

        HttpServer httpServer = HttpServer.create (new InetSocketAddress(0), 10);
        try {
            FileServerHandler handler = new FileServerHandler(workdir+"serverRoot/");
            HttpContext ctx = httpServer.createContext ("/", handler);
            httpServer.start();

            int port = httpServer.getAddress().getPort();
            String s = "http://127.0.0.1:"+port+"/";
            URL url = new URL(s);

            String testjar = workdir + "test.jar";
            copyFile (workdir+"test1.jar", testjar);
            test (testjar, 1, url);

            // repeat test with different implementation
            // of test.jar (whose TestClass.getValue() returns 2

            copyFile (workdir+"test2.jar", testjar);
            test (testjar, 2, url);

            // repeat test using a directory of files
            String testdir=workdir+"testdir/";
            rm_minus_rf (new File(testdir));
            copyDir (workdir+"test1/", testdir);
            test (testdir, 1, url);

            testdir=workdir+"testdir/";
            rm_minus_rf (new File(testdir));
            copyDir (workdir+"test2/", testdir);
            test (testdir, 2, url);
        } finally {
            httpServer.stop(3);
        }
    }

    // create a loader on jarfile (or directory), plus a http loader
    // load a class , then look for a resource
    // also load a class from http loader
    // then close the loader
    // check further new classes/resources cannot be loaded
    // check jar (or dir) can be deleted
    // check existing classes can be loaded
    // check boot classes can be loaded
    private static void test (String name, int expectedValue, URL url2) throws Exception {
        URL url = new URL ("file", null, name);
        URL[] urls = new URL[2];
        urls[0] =  url;
        urls[1] =  url2;
        URLClassLoader loader = new URLClassLoader (urls);
        Class<?> testclass = loadClass ("com.foo.TestClass", loader, true);
        Class<?> class2 = loadClass ("Test", loader, true); // from http
        class2.newInstance();
        Object test = testclass.newInstance();
        Method method = testclass.getDeclaredMethods()[0]; // int getValue();
        int res = (Integer) method.invoke (test);

        if (res != expectedValue) {
            throw new RuntimeException ("wrong value from getValue() ["+res+
                                        "/"+expectedValue+"]");
        }

        // should find /resource1
        URL u1 = loader.findResource ("com/foo/Resource1");
        if (u1 == null) {
            throw new RuntimeException ("can't find com/foo/Resource1 in test1.jar");
        }
        loader.close ();

        // should NOT find /resource2 even though it is in jar
        URL u2 = loader.findResource ("com/foo/Resource2");
        if (u2 != null) {
            throw new RuntimeException ("com/foo/Resource2 unexpected in test1.jar");
        }

        // load tests
        loadClass ("com.foo.TestClass1", loader, false);
        loadClass ("com.foo.TestClass", loader, true);
        loadClass ("java.sql.Array", loader, true);

        // now check we can delete the path
        rm_minus_rf (new File(name));
    }

    /**
     * make sure files can be deleted after closing the loader.
     * Therefore, the test will only really be verified on Windows. It
     * will still run correctly on other platforms
     */
    @Test
    public void b6899919() throws Exception {
        File orig = new File(System.getProperty("test.resources.dir") + "/b6899919");
        File tmp = Files.createTempDirectory("URLClassLoaderTest").toFile();
        File workdir = new File(tmp, "work");
        copyDir(orig, workdir);

        /* the jar we copy for each test */
        File srcfile = new File (workdir, "foo.jar");

        /* the jar we use for the test */
        File testfile = new File (workdir, "test.jar");

        copyFile (srcfile, testfile);
        test (testfile, false, false);

        copyFile (srcfile, testfile);
        test (testfile, true, false);

        copyFile (srcfile, testfile);
        test (testfile, true, true);

        // repeat test using a directory of files

        File testdir= new File (workdir, "testdir");
        File srcdir= new File (workdir, "test3");

        copyDir (srcdir, testdir);
        test (testdir, true, false);

    }

    // create a loader on jarfile (or directory)
    // load a class , then look for a resource
    // then close the loader
    // check further new classes/resources cannot be loaded
    // check jar (or dir) can be deleted
    static void test (File file, boolean loadclass, boolean readall)
        throws Exception
    {
        URL[] urls = new URL[] {file.toURI().toURL()};
        URLClassLoader loader = new URLClassLoader (urls);
        if (loadclass) {
            loadClass ("com.foo.TestClass", loader, true);
        }
        InputStream s = loader.getResourceAsStream ("hello.txt");
        s.read();
        if (readall) {
            while (s.read() != -1) ;
            s.close();
        }

        loader.close ();

        // shouuld not find bye.txt now
        InputStream s1 = loader.getResourceAsStream("bye.txt");
        Assert.assertNull(s1);

        // now check we can delete the path
        rm_minus_rf (file);
    }

    @Test
    public void resourcesAsStream() throws Exception {
        URLClassLoader cl = new URLClassLoader (new URL[] {
            new URL ("file:" + System.getProperty("test.resources.dir") + "/test.jar")
        });
        Class<?> clazz = Class.forName ("Test\u00a3", true, cl);
        InputStream is = clazz.getResourceAsStream ("Test\u00a3.class");
        is.read();
        is = clazz.getResourceAsStream ("Rest\u00a3.class");
        is.read();
    }

    @Test(expected = java.lang.SecurityException.class)
    public void sealed1() throws Exception {
        boolean fail = true;
        p.A.hello();
        p.B.hello();
    }

    @Test(expected = java.lang.SecurityException.class)
    public void sealed2() throws Exception {
        boolean fail = true;
        p.B.hello();
        p.A.hello();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // UTILITIES --- a lot of this should be rewritten to use NIO Files
    static void copyFile (String src, String dst) {
        copyFile (new File(src), new File(dst));
    }

    static void copyDir (String src, String dst) {
        copyDir (new File(src), new File(dst));
    }

    static void copyFile (File src, File dst) {
        try {
            if (!src.isFile()) {
                throw new RuntimeException ("File not found: " + src.toString());
            }
            Files.copy(src.toPath(), dst.toPath(), REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException (e);
        }
    }

    static void rm_minus_rf (File path) throws IOException, InterruptedException {
        if (!path.exists())
            return;
        FileUtils.deleteFileTreeWithRetry(path.toPath());
    }

    static void copyDir (File src, File dst) {
        if (!src.isDirectory()) {
            throw new RuntimeException ("Dir not found: " + src.toString());
        }
        if (dst.exists()) {
            throw new RuntimeException ("Dir exists: " + dst.toString());
        }
        dst.mkdir();
        String[] names = src.list();
        File[] files = src.listFiles();
        for (int i=0; i<files.length; i++) {
            String f = names[i];
            if (files[i].isDirectory()) {
                copyDir (files[i], new File (dst, f));
            } else {
                copyFile (new File (src, f), new File (dst, f));
            }
        }
    }

    /* expect is true if you expect to find it, false if you expect not to */
    static Class<?> loadClass (String name, URLClassLoader loader, boolean expect){
        try {
            Class<?> clazz = Class.forName (name, true, loader);
            if (!expect) {
                throw new RuntimeException ("loadClass: "+name+" unexpected");
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            if (expect) {
                throw new RuntimeException ("loadClass: " +name + " not found");
            }
        }
        return null;
    }
}

// Copyright (C) 2016 Sam Halliday
// License: http://www.gnu.org/software/classpath/license.html
package fommil;

import java.net.URI;
import java.net.URL;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static fommil.ClassMonkeyUtils.*;

public class ClassMonkeyUtilsTest {

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    @Test
    public void parseWindowsURL() throws Exception {
        URL url = new URL("jar:file:C:\\a\\b/d.jar!/E.class");
        URI got = toURI(url);
        URI expected = URI.create("jar:file:///C:/a/b/d.jar!/E.class");
        Assert.assertEquals(expected, got);
    }
}

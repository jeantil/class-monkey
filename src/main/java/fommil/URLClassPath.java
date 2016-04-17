// Copyright (C) 2016 Sam Halliday
// License: http://www.gnu.org/software/classpath/license.html
package fommil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;
import java.util.List;

/**
 * Clean-room reimplementation (not subject to the Oracle licences) of
 * sun.misc.URLClassPath designed to be stateless, simple and not leak
 * any resources.
 */
final public class URLClassPath extends sun.misc.URLClassPath {
    public URLClassPath(URL[] urls, URLStreamHandlerFactory factory) {
        super(new URL[] {}, null); // disable the super
    }

    public URLClassPath(URL[] urls) {
        this(urls, null);
    }

    @Override
    public synchronized List<IOException> closeLoaders() {
        return null;
    }

    @Override
    public synchronized void addURL(URL url) {
    }

    @Override
    public URL[] getURLs() {
        return null;
    }

    @Override
    public URL findResource(String name, boolean check) {
        return null;
    }

    @Override
    public sun.misc.Resource getResource(String name) {
        return getResource(name, true);
    }

    @Override
    public sun.misc.Resource getResource(String name, boolean check) {
        return null;
    }

    @Override
    public Enumeration<URL> findResources(String name, boolean check) {
        return null;
    }

    @Override
    public Enumeration<sun.misc.Resource> getResources(String name) {
        return getResources(name, true);
    }

    @Override
    public Enumeration<sun.misc.Resource> getResources(String name, boolean check) {
        return null;
    }

}

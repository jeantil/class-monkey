// Copyright (C) 2016 Sam Halliday
// License: http://www.gnu.org/software/classpath/license.html
package fommil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import static fommil.ClassMonkeyUtils.*;

/**
 * Clean-room reimplementation (not subject to the Oracle licences) of
 * {@link sun.misc.URLClassPath} designed to be stateless (insofar as
 * the API allows), simple and not leak any resources.
 *
 * Recall that {@link java.net.URL#equals(Object)} will use a DNS, so
 * it should be avoided.
 */
final public class URLClassPath extends sun.misc.URLClassPath {

    private final CopyOnWriteArraySet<URI> uris = new CopyOnWriteArraySet<>();

    public URLClassPath(URL[] urls, URLStreamHandlerFactory factory) {
        // disables the super
        super(new URL[] {}, null);

        for (URL url: urls) {
            addURL(url);
        }
    }

    public URLClassPath(URL[] urls) {
        this(urls, null);
    }

    @Override
    public List<IOException> closeLoaders() {
        return Collections.emptyList();
    }

    @Override
    public void addURL(URL url) {
        if (url == null) return;

        URI uri = toURI(url);
        if (uris.add(uri)) {
            System.out.println("Added " + uri + " with scheme " + uri.getScheme());
            // valid possibilities are:
            //
            // 1. file (points to directory or archive)
            // 2. jar/zip (an entry / directory within an archive)
            // 3. some crazy network server location

            // TODO atomic operation to pre-scan this URI
        }
    }

    @Override
    public URL[] getURLs() {
        List<URL> urls = new ArrayList<>();
        for (URI uri : uris) {
            urls.add(toURL(uri));
        }
        return urls.toArray(new URL[]{ });
    }

    @Override
    public URL findResource(String name, boolean check) {
        return null;
    }

    @Override
    public Enumeration<URL> findResources(String name, boolean check) {
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
    public Enumeration<sun.misc.Resource> getResources(String name) {
        return getResources(name, true);
    }

    @Override
    public Enumeration<sun.misc.Resource> getResources(String name, boolean check) {
        return null;
    }

}

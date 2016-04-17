// Copyright (C) 2016 Sam Halliday
// License: http://www.gnu.org/software/classpath/license.html
package fommil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import static fommil.ClassMonkeyUtils.*;

/**
 * Clean-room reimplementation (not subject to the Oracle licences) of
 * {@link sun.misc.URLClassPath} designed to be stateless (insofar as
 * the API allows), simple and not leak or maintain file handles.
 *
 * Pre-loading is used to optimise jar / zip archive loading (at the
 * cost of an initial overhead). Archives are assumed to be immutable
 * for the lifetime of an instance.
 *
 * Directory based URLs are lazily fetched with no caching.
 *
 * Performance may be degraded for network protocols and no effort is
 * spent trying to cache such a ridiculous concept (let's be honest,
 * you shouldn't be using network classloaders in this day and age).
 *
 *
 * No security checking is performed: all URIs and user requests are
 * trusted and whatever powers the JVM provides are used.
 */
final public class URLClassPath extends sun.misc.URLClassPath {
    private static final Logger log = Logger.getLogger(URLClassPath.class.getName());

    // recall that java.net.URL#equals will use a DNS, so avoid URL in collections
    private final CopyOnWriteArraySet<URI> uris = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArrayList<ResourceProvider> providers = new CopyOnWriteArrayList<>();
    private final URLStreamHandlerFactory factory;

    // primary constructor, intentionally disable the super
    // implementation by sending empty data.
    public URLClassPath(URL[] urls, URLStreamHandlerFactory factory) {
        super(new URL[] {}, null);

        this.factory = factory;

        for (URL url: urls) {
            addURL(url);
        }
    }

    // convenience constructor used by URLClassLoader
    public URLClassPath(URL[] urls) {
        this(urls, null);
    }

    // no-op because we hold no state
    @Override
    public List<IOException> closeLoaders() {
        return Collections.emptyList();
    }

    ///////////////////////////////////////////////////////////////////////////////
    // getters / setters
    @Override
    public URL[] getURLs() {
        List<URL> urls = new ArrayList<>();
        for (URI uri : uris) {
            urls.add(toURL(uri));
        }
        return urls.toArray(new URL[]{ });
    }

    // if called concurrently, it is possible that ResourceProvider
    // implementations are saved in a different order than the URIs
    // that they are associated with. This is an acceptable trade-off
    // for simplicity of implementation.
    @Override
    public void addURL(URL url) {
        if (url == null) return;

        URI uri = toURI(url);
        if (uris.add(uri)) {
            String scheme = uri.getScheme();
            if (scheme.equals("jar") || scheme.equals("zip")) {
                try {
                    // http://stackoverflow.com/questions/8014099
                    JarURLConnection connection = (JarURLConnection) url.openConnection();
                    URI file = toURI(connection.getJarFileURL());
                    providers.add(new ArchiveResourceProvider(file, connection.getEntryName()));
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            if (uri.getScheme().equals("file")) {
                String path = uri.getPath();
                if (path == null)
                    // TODO: this might be a relative URI, reparse?
                    throw new IllegalArgumentException("bad URI (no path): " + uri);
                else if (path.endsWith(".jar") || path.endsWith(".zip"))
                    providers.add(new ArchiveResourceProvider(uri, null));
                else if (path.endsWith("/"))
                    providers.add(new DirectoryResourceProvider(uri));
                else
                    throw new UnsupportedOperationException("Unknown archive: " + uri);
            } else {
                providers.add(new GenericResourceProvider(uri, factory));
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    // redundant public API
    @Override
    public sun.misc.Resource getResource(String name, boolean ignoredSecurityCheck) {
        return getResource(name);
    }

    @Override
    public Enumeration<sun.misc.Resource> getResources(String name, boolean ignoredSecurityCheck) {
        return getResources(name);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // the core public API
    @Override
    public URL findResource(String name, boolean ignoredSecurityCheck) {
        for (ResourceProvider provider : providers) {
            URI found = provider.findFirst(name);
            if (found != null) return toURL(found);
        }
        return null;
    }

    @Override
    public sun.misc.Resource getResource(String name) {
        for (ResourceProvider provider : providers) {
            SimpleResource found = provider.getFirst(name);
            if (found != null) return found;
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    // exhaustive variants
    @Override
    public Enumeration<URL> findResources(String name, boolean ignoredSecurityCheck) {
        Set<URI> all = new LinkedHashSet<>();
        for (ResourceProvider provider : providers) {
            List<URI> found = provider.findAll(name);
            all.addAll(found);
        }
        List<URL> urls = new ArrayList<>();
        for (URI uri : all) {
            urls.add(toURL(uri));
        }
        return Collections.enumeration(urls);
    }

    @Override
    public Enumeration<sun.misc.Resource> getResources(String name) {
        Set<sun.misc.Resource> all = new LinkedHashSet<>();
        for (ResourceProvider provider : providers) {
            List<SimpleResource> found = provider.getAll(name);
            all.addAll(found);
        }
        return Collections.enumeration(all);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // the core implementation
    static interface ResourceProvider {
        URI findFirst(String name);
        List<URI> findAll(String name);

        SimpleResource getFirst(String name);
        List<SimpleResource> getAll(String name);
    }

    static private final class DirectoryResourceProvider implements ResourceProvider {
        // should we perhaps be using the nio FileSystem API?
        private final URI base;

        public DirectoryResourceProvider(URI base) {
            this.base = base;
        }

        @Override
        public URI findFirst(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }

        @Override
        public List<URI> findAll(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }

        @Override
        public SimpleResource getFirst(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }

        @Override
        public List<SimpleResource> getAll(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }
    }

    static final class ArchiveResourceProvider implements ResourceProvider {
        // The nio FileSystem API is reported to keep persistent file
        // handles, which is no good at all, so drop down to old
        // fashioned JarFile / ZipFile access.

        private final URI archive;
        private final String base;

        public ArchiveResourceProvider(URI uri, String base) {
            this.archive = uri;
            this.base = base;
        }

        @Override
        public URI findFirst(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }

        @Override
        public List<URI> findAll(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }

        @Override
        public SimpleResource getFirst(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }

        @Override
        public List<SimpleResource> getAll(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }
    }

    static final class GenericResourceProvider implements ResourceProvider {
        private final URI base;
        // factory is null to use the system default
        private final URLStreamHandlerFactory factory;

        public GenericResourceProvider(URI base, URLStreamHandlerFactory factory) {
            this.base = base;
            this.factory = factory;
        }

        @Override
        public URI findFirst(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }

        @Override
        public List<URI> findAll(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }

        @Override
        public SimpleResource getFirst(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }

        @Override
        public List<SimpleResource> getAll(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }
    }
}

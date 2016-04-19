// Copyright (C) 2016 Sam Halliday
// License: http://www.gnu.org/software/classpath/license.html
package fommil;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;
import java.util.zip.*;

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
 * "Class-Path" entries in Manifests are not supported (it's
 * over-engineering): only explicitly added URLs will be scanned.
 *
 * Directory based URLs are lazily fetched with no caching.
 *
 * Network protocols are not supported (let's be honest, you shouldn't
 * be using network classloaders in this day and age).
 *
 * No security checking is performed: all URIs and user requests are
 * trusted to the extend that the JVM's security manager allows.
 */
final public class URLClassPath extends sun.misc.URLClassPath {
    private static final Logger log = Logger.getLogger(URLClassPath.class.getName());

    // recall that java.net.URL#equals will use a DNS, so avoid URL in collections
    private final CopyOnWriteArraySet<URI> uris = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArrayList<ResourceProvider> providers = new CopyOnWriteArrayList<>();
    private final URLStreamHandlerFactory factory;
    private final AtomicBoolean closed = new AtomicBoolean(false);

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

    // legacy behaviour is to reject all future calls, even though we
    // have nothing to close
    @Override
    public List<IOException> closeLoaders() {
        if (log.isLoggable(Level.FINER))
            log.finer("closeLoaders()");
        closed.set(true);
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
        if (log.isLoggable(Level.FINER))
            log.finer("addURL(" + url + ")");
        if (closed.get() || url == null) return;

        URI uri = toURI(url);
        if (uris.add(uri)) {
            String scheme = uri.getScheme();
            if (scheme.equals("jar")) {
                try {
                    // http://stackoverflow.com/questions/8014099
                    JarURLConnection connection = (JarURLConnection) url.openConnection();
                    URL jarURL = connection.getJarFileURL();
                    URI jarURI = toURI(jarURL);
                    if (log.isLoggable(Level.FINE))
                        log.fine("addURL jarFileURL = " + jarURL + ", jarFileURI = " + jarURI);
                    providers.add(new ArchiveResourceProvider(jarURI, connection.getEntryName()));
                } catch (IOException e) {
                    throw new IllegalArgumentException(uri + " is a bad archive", e);
                }
            } else if (uri.getScheme().equals("file")) {
                String path = uri.getPath();
                if (path == null)
                    throw new IllegalArgumentException("bad URL (no path part) " + uri);

                if (path.endsWith(".jar") || path.endsWith(".zip")) {
                    try {
                        providers.add(new ArchiveResourceProvider(uri, null));
                    } catch (IOException e) {
                        throw new IllegalArgumentException(uri + " is a bad archive", e);
                    }
                } else {
                    providers.add(new DirectoryResourceProvider(uri));
                }
            } else {
                throw new UnsupportedOperationException("Generic URL scheme: " + uri);
                //providers.add(new GenericResourceProvider(uri, factory));
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
        if (closed.get()) return null;
        try {
            for (ResourceProvider provider : providers) {
                URI found = provider.find(name);
                if (found != null) return toURL(found);
            }
            if (log.isLoggable(Level.FINE))
                log.fine("findResource missed: " + name);
            return null;
        } catch (IOException e) {
            throw new IllegalStateException("while finding " + name, e);
        }
    }

    @Override
    public sun.misc.Resource getResource(String name) {
        if (closed.get()) return null;
        try {
            for (ResourceProvider provider : providers) {
                SimpleResource found = provider.get(name);
                if (found != null) return found;
            }
            if (log.isLoggable(Level.FINE))
                log.fine("getResource missed: " + name);
            return null;
        } catch (IOException e) {
            throw new IllegalStateException("while finding " + name, e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    // exhaustive variants
    @Override
    public Enumeration<URL> findResources(String name, boolean ignoredSecurityCheck) {
        if (closed.get()) return null;
        try {
            Set<URI> all = new LinkedHashSet<>();
            for (ResourceProvider provider : providers) {
                URI found = provider.find(name);
                if (found != null)
                    all.add(found);
            }
            List<URL> urls = new ArrayList<>();
            for (URI uri : all) {
                urls.add(toURL(uri));
            }
            if (log.isLoggable(Level.FINE) && urls.isEmpty())
                log.fine("findResources missed: " + name);

            return Collections.enumeration(urls);
        } catch (IOException e) {
            throw new IllegalStateException("while finding " + name, e);
        }
    }

    @Override
    public Enumeration<sun.misc.Resource> getResources(String name) {
        if (closed.get()) return null;
        try {
            Set<sun.misc.Resource> all = new LinkedHashSet<>();
            for (ResourceProvider provider : providers) {
                SimpleResource found = provider.get(name);
                if (found != null)
                    all.add(found);
            }
            if (log.isLoggable(Level.FINE) && all.isEmpty())
                log.fine("getResources missed: " + name);

            return Collections.enumeration(all);
        } catch (IOException e) {
            throw new IllegalStateException("while finding " + name, e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // the core implementation
    static interface ResourceProvider {
        URI find(String name) throws IOException;
        SimpleResource get(String name)throws IOException;
    }

    static private final class DirectoryResourceProvider implements ResourceProvider {
        private static final Logger log = Logger.getLogger(DirectoryResourceProvider.class.getName());

        // should we perhaps be using the nio FileSystem API?
        private final URI base;

        public DirectoryResourceProvider(URI base) {
            this.base = base;
        }

        @Override
        public URI find(String name) throws IOException {
            File file = new File(new File(base), name);
            if (!file.isFile()) return null;
            else return file.toURI();
        }

        @Override
        public SimpleResource get(String name) throws IOException {
            URI found = find(name);
            if (found == null) return null;

            File file = new File(find(name));
            InputStream is = new FileInputStream(file);
            byte[] bytes = ClassMonkeyUtils.slurp(is);
            return new SimpleResource(toURL(base), name, toURL(found), bytes);
        }

        @Override
        public String toString() {
            return "DirectoryResourceProvider(" + base + ")";
        }
    }

    static final class ArchiveResourceProvider implements ResourceProvider {
        private static final Logger log = Logger.getLogger(ArchiveResourceProvider.class.getName());
        // The nio FileSystem API is reported to keep persistent file
        // handles, which is no good at all, so drop down to old
        // fashioned JarFile / ZipFile access.

        private final URI archive;
        private final String base;
        private final Map<String, ZipEntry> entriesByName = new HashMap<>();

        public ArchiveResourceProvider(URI uri, String base) throws IOException {
            this.archive = uri;
            this.base = base;

            File file = new File(uri);
            // legacy behaviour is to ignore files that don't exist
            if (!file.isFile()) return;

            try (
                 ZipFile zip = new ZipFile(file);
            ) {
                List<? extends ZipEntry> entries = Collections.list(zip.entries());
                for (ZipEntry entry : entries) {
                    String name = entry.getName();
                    if ((base == null || name.startsWith(base)) && !name.endsWith("/")) {
                        if (log.isLoggable(Level.FINEST))
                            log.finest(toString() + " += '" + name + "'");
                        entriesByName.put(name, entry);
                    }
                }
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(uri + " is a bad archive", e);
            }

            if (log.isLoggable(Level.FINER))
                log.finer(toString() + " base=" + base);
        }

        @Override
        public URI find(String name) throws IOException {
            if (log.isLoggable(Level.FINEST))
                log.finest(toString() + ".find(" + name + ")");
            name = name.replaceAll("^/", "");
            if (!entriesByName.containsKey(name)) return null;
            try {
                File file = new File(archive);
                String path = file.getCanonicalPath().replaceAll("^/*", "");
                URI found = new URI("jar:file:///" + path + "!/" + name);
                if (log.isLoggable(Level.FINE))
                    log.fine(toString() + " find(" + name + ") = " + found);
                return found;
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }

        @Override
        public SimpleResource get(String name) throws IOException {
            if (log.isLoggable(Level.FINEST))
                log.finest(toString() + ".get(" + name + ")");
            name = name.replaceAll("^/", "");
            ZipEntry entry = entriesByName.get(name);
            URI uri = find(name);
            if (entry == null || uri == null) return null;
            try (
                 ZipFile zip = new ZipFile(new File(archive));
            ) {
                InputStream in = zip.getInputStream(entry);
                byte[] bytes = slurp(in);
                return new SimpleResource(toURL(archive), name, toURL(uri), bytes);
            }
        }

        @Override
        public String toString() {
            return "ArchiveResourceProvider(" + archive + ")";
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
        public URI find(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }

        @Override
        public SimpleResource get(String name) {
            throw new UnsupportedOperationException("TODO for " + name);
        }
    }
}

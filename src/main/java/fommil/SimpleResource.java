// Copyright (C) 2016 Sam Halliday
// License: http://www.gnu.org/software/classpath/license.html
package fommil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.CodeSigner;
import java.util.Arrays;
import java.util.jar.Manifest;

import static fommil.ClassMonkeyUtils.*;

/**
 * Clean-room implementation (not subject to the Oracle licences) of
 * sun.misc.Resource designed to be stateless, simple and not leak any
 * resources.
 */
final class SimpleResource extends sun.misc.Resource {

    private final String name;
    private final URI source, loc;
    private final byte[] compressed;
    private final int length;

    // assumes that creater is trusted and will not retain a reference to bytes
    // code can be null if this is not a .class file
    SimpleResource(URI source, String name, URI loc, byte[] bytes) throws IOException {
        if (name == null) throw new IllegalArgumentException("`name' must not be null");
        if (loc == null) throw new IllegalArgumentException("`loc' must not be null");
        if (bytes == null) throw new IllegalArgumentException("`bytes' must not be null");

        this.source = source;
        this.name = name;
        this.loc = loc;
        this.compressed = deflate(bytes);
        this.length = bytes.length;
    }

    protected URI getLoc() {
        return loc;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public URL getURL() {
        return toURL(loc);
    }

    @Override
    public byte[] getBytes() throws IOException {
        return enflate(compressed);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // simple wrappers
    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(getBytes());
    }

    @Override
    public int getContentLength() throws IOException {
        return length;
    }

    @Override
    public ByteBuffer getByteBuffer() throws IOException {
        return ByteBuffer.wrap(getBytes());
    }

    @Override
    public URL getCodeSourceURL() {
        return toURL(source);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // things we don't care to implement...
    @Override
    public Manifest getManifest() throws IOException {
        return null;
    }

    @Override
    public java.security.cert.Certificate[] getCertificates() {
        return null;
    }

    @Override
    public CodeSigner[] getCodeSigners() {
        return null;
    }
}

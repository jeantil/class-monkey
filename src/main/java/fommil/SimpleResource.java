// Copyright (C) 2016 Sam Halliday
// License: http://www.gnu.org/software/classpath/license.html
package fommil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.CodeSigner;
import java.util.Arrays;
import java.util.jar.Manifest;

/**
 * Clean-room implementation (not subject to the Oracle licences) of
 * sun.misc.Resource designed to be stateless, simple and not leak any
 * resources.
 */
final class SimpleResource extends sun.misc.Resource {

    private final String name;
    private final URL url;
    private final byte[] bytes;

    // assumes that creater is trusted and will not retain a reference to bytes
    // code can be null if this is not a .class file
    SimpleResource(String name, URL url, byte[] bytes) {
        if (name == null) throw new IllegalArgumentException("`name' must not be null");
        if (url == null) throw new IllegalArgumentException("`url' must not be null");
        if (bytes == null) throw new IllegalArgumentException("`bytes' must not be null");

        this.name = name;
        this.url = url;
        this.bytes = bytes;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return bytes.clone();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // simple wrappers
    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public int getContentLength() throws IOException {
        return bytes.length;
    }

    @Override
    public ByteBuffer getByteBuffer() throws IOException {
        return ByteBuffer.wrap(bytes.clone());
    }

    // I don't really understand this one
    @Override
    public URL getCodeSourceURL() {
        if (!name.endsWith(".class")) return null;
        return url;
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

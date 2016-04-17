// Copyright (C) 2016 Sam Halliday
// License: http://www.gnu.org/software/classpath/license.html
package fommil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.CodeSigner;
import java.util.jar.Manifest;

/**
 * Clean-room implementation (not subject to the Oracle licences) of
 * sun.misc.Resource designed to be stateless, simple and not leak any
 * resources.
 */
final public class SimpleResource extends sun.misc.Resource {
    public String getName() {
        return null;
    }

    public URL getURL() {
        return null;
    }

    public URL getCodeSourceURL() {
        return null;
    }

    public InputStream getInputStream() throws IOException {
        return null;
    }

    public int getContentLength() throws IOException {
        return -1;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return null;
    }

    @Override
    public ByteBuffer getByteBuffer() throws IOException {
        return null;
    }

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

// Copyright (C) 2016 Sam Halliday
// License: http://www.gnu.org/software/classpath/license.html
package fommil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * The obligatory *Utils class.
 */
public final class ClassMonkeyUtils {
//    private static final Logger log = Logger.getLogger(ClassMonkeyUtils.class.getName());

    private ClassMonkeyUtils () { }

    /**
     * Fully consume an InputStream into a byte array, and close the input.
     */
    public static byte[] slurp(InputStream in) throws IOException {
        if (in == null) throw new NullPointerException("`in' must not be null");
        try {
            int nRead;
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((nRead = in.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, nRead);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    public static byte[] deflate(byte[] data) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Deflater deflater = new Deflater();
            try {
                deflater.setInput(data);
                deflater.finish();

                byte[] buffer = new byte[1024];
                while (!deflater.finished()) {
                    int count = deflater.deflate(buffer);
                    out.write(buffer, 0, count);
                }
                return out.toByteArray();
            } finally {
                deflater.end();
            }
        }
    }

    public static byte[] enflate(byte[] data) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Inflater inflater = new Inflater();
            try {
                inflater.setInput(data);

                byte[] buffer = new byte[1024];
                while (!inflater.finished()) {
                    int count = inflater.inflate(buffer);
                    out.write(buffer, 0, count);
                }
                return out.toByteArray();
            } finally {
                inflater.end();
            }
        } catch (DataFormatException e) {
            throw new IOException(e);
        }
    }

    /**
     * Perform the conversion without checked exceptions.
     */
    public static URL toURL(URI uri) {
        if (uri == null) throw new NullPointerException("`uri' must not be null");
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(uri + " is not a valid URL", e);
        }
    }

    /**
     * Perform the conversion without checked exceptions.
     */
    public static URI toURI(URL url) {
        if (url == null) throw new NullPointerException("`url' must not be null");
        try {
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                // Windows hacks
                return new File(url.getFile()).toURI();
            } else if ("jar".equals(protocol)) {
                String cleaned = url.toExternalForm().replaceAll("^jar:file:([a-zA-Z]+):", "jar:file:///$1:").replace("\\", "/");
                return URI.create(cleaned);
            }
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("parsing " + url, e);
        }
    }
}

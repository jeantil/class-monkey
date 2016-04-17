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

/**
 * The obligatory *Utils class.
 */
final class ClassMonkeyUtils {
    private ClassMonkeyUtils () { }

    /**
     * Fully consume an InputStream into a byte array, and close the input.
     */
    static byte[] slurp(InputStream in) throws IOException {
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

    /**
     * Perform the conversion without checked exceptions.
     */
    static URL toURL(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(uri + " is not a valid URL", e);
        }
    }

    /**
     * Perform the conversion without checked exceptions.
     */
    static URI toURI(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(url + " is not a valid URI", e);
        }
    }
}

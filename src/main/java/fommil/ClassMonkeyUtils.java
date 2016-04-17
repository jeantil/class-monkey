// Copyright (C) 2016 Sam Halliday
// License: http://www.gnu.org/software/classpath/license.html
package fommil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

}

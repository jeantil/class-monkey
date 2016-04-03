// Copyright 1999 Oracle and/or its affiliates
// Copyright 2016 Sam Halliday
// Licence: http://openjdk.java.net/legal/gplv2+ce.html
package jdk.testlibrary;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;


/**
 * Common library for various test file utility functions.
 */
public final class FileUtils {

    private static final boolean isWindows =
                            System.getProperty("os.name").startsWith("Windows");
    private static final int RETRY_DELETE_MILLIS = isWindows ? 500 : 0;
    private static final int MAX_RETRY_DELETE_TIMES = isWindows ? 15 : 0;

    /**
     * Deletes a file, retrying if necessary.
     *
     * @param path  the file to delete
     *
     * @throws NoSuchFileException
     *         if the file does not exist (optional specific exception)
     * @throws DirectoryNotEmptyException
     *         if the file is a directory and could not otherwise be deleted
     *         because the directory is not empty (optional specific exception)
     * @throws IOException
     *         if an I/O error occurs
     */
    public static void deleteFileWithRetry(Path path)
        throws IOException
    {
        try {
            deleteFileWithRetry0(path);
        } catch (InterruptedException x) {
            throw new IOException("Interrupted while deleting.", x);
        }
    }

    /**
     * Deletes a file, retrying if necessary.
     * No exception thrown if file doesn't exist.
     *
     * @param path  the file to delete
     *
     * @throws NoSuchFileException
     *         if the file does not exist (optional specific exception)
     * @throws DirectoryNotEmptyException
     *         if the file is a directory and could not otherwise be deleted
     *         because the directory is not empty (optional specific exception)
     * @throws IOException
     *         if an I/O error occurs
     */
    public static void deleteFileIfExistsWithRetry(Path path)
        throws IOException
    {
        try {
            if(Files.exists(path))
                deleteFileWithRetry0(path);
        } catch (InterruptedException x) {
            throw new IOException("Interrupted while deleting.", x);
        }
    }

    private static void deleteFileWithRetry0(Path path)
        throws IOException, InterruptedException
    {
        int times = 0;
        IOException ioe = null;
        while (true) {
            try {
                Files.delete(path);
                while (Files.exists(path)) {
                    times++;
                    if (times > MAX_RETRY_DELETE_TIMES)
                        throw new IOException("File still exists after " + times + " waits.");
                    Thread.sleep(RETRY_DELETE_MILLIS);
                }
                break;
            } catch (NoSuchFileException | DirectoryNotEmptyException x) {
                throw x;
            } catch (IOException x) {
                // Backoff/retry in case another process is accessing the file
                times++;
                if (ioe == null)
                    ioe = x;
                else
                    ioe.addSuppressed(x);

                if (times > MAX_RETRY_DELETE_TIMES)
                    throw ioe;
                Thread.sleep(RETRY_DELETE_MILLIS);
            }
        }
    }

    /**
     * Deletes a directory and its subdirectories, retrying if necessary.
     *
     * @param dir  the directory to delete
     *
     * @throws  IOException
     *          If an I/O error occurs. Any such exceptions are caught
     *          internally. If only one is caught, then it is re-thrown.
     *          If more than one exception is caught, then the second and
     *          following exceptions are added as suppressed exceptions of the
     *          first one caught, which is then re-thrown.
     */
    public static void deleteFileTreeWithRetry(Path dir)
         throws IOException
    {
        IOException ioe = null;
        final List<IOException> excs = deleteFileTreeUnchecked(dir);
        if (!excs.isEmpty()) {
            ioe = excs.remove(0);
            for (IOException x : excs)
                ioe.addSuppressed(x);
        }
        if (ioe != null)
            throw ioe;
    }

    public static List<IOException> deleteFileTreeUnchecked(Path dir) {
        final List<IOException> excs = new ArrayList<>();
        try {
            java.nio.file.Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        deleteFileWithRetry0(file);
                    } catch (IOException x) {
                        excs.add(x);
                    } catch (InterruptedException x) {
                        excs.add(new IOException("Interrupted while deleting.", x));
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    try {
                        deleteFileWithRetry0(dir);
                    } catch (IOException x) {
                        excs.add(x);
                    } catch (InterruptedException x) {
                        excs.add(new IOException("Interrupted while deleting.", x));
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    excs.add(exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException x) {
            excs.add(x);
        }
        return excs;
    }
}

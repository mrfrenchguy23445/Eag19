/*
 * Copyright (c) 2025 lax1dude. All Rights Reserved.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package net.lax1dude.eaglercraft.v1_8;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code java.io.File} operations the game used to get from {@code org.apache.commons.io.FileUtils}.
 *
 * <p>Commons IO is a jar, so it cannot be patched, and every one of these methods reaches
 * {@code File.toPath()} the moment it runs: {@code copyFile} goes through {@code Files.copy},
 * {@code readFileToString} through {@code Files.newInputStream}, {@code forceDelete} through
 * {@code cleanDirectory}'s walk. {@code java.io.File} is a class the classlib owns -- it is
 * complete except for {@code toPath()} -- so there is nowhere to add the missing method, and the
 * nine errors that came out of it in the last link cannot be shimmed away. The call sites have to
 * stop asking for a {@code Path} instead.
 *
 * <p>So the methods are here, over {@link FileInputStream} and {@link FileOutputStream}, which the
 * classlib does have. This is not a reimplementation of Commons IO -- it is the nine methods the
 * game actually calls, with their signatures kept identical so that converting a call site is a
 * change to one import line.
 *
 * <p>The behaviour follows Commons IO 2.11 rather than being invented, and the differences worth
 * knowing about are called out on the individual methods.
 *
 * <p>What is deliberately left to the real jar: {@code IOUtils}, {@code CountingOutputStream},
 * {@code TrueFileFilter} and {@code LastModifiedFileComparator} all link as-is, so they stay on
 * Commons IO. Only {@code FileUtils} is here. Note the package already shadows
 * {@code IOUtils} -- {@link IOUtils} in this same package is Eaglercraft's own -- so the two
 * libraries' identically-named helpers now live on opposite sides of an import.
 */
public final class FileUtils {

    private FileUtils() {
    }

    /**
     * Copies a file, overwriting the destination and preserving its modified time.
     *
     * <p>Commons IO's {@code copyFile(File, File)} delegates to the three-argument form with
     * {@code preserveFileDate} set, which is what the one caller wanted: {@code FileDownload}
     * renames a fully-downloaded temp file over the real one, and without the timestamp the
     * downloaded-pack cache would consider the new pack older than it is and re-fetch it on the
     * next launch.
     *
     * <p>Commons IO also rejects copying a file onto itself, comparing canonical paths. That is
     * kept, because without it a self-copy would truncate the file before reading it.
     *
     * <p>One thing that is not kept: Commons IO calls {@code deleteOnExit} as it goes, to cover a
     * shutdown that might not arrive cleanly. {@code File.deleteOnExit} exists on the classlib's
     * {@code File} but has nowhere to register -- there is no shutdown hook to attach to -- so
     * omitting it changes nothing that was working.
     */
    public static void copyFile(File srcFile, File destFile) throws IOException {
        if (!srcFile.exists()) {
            throw new IOException("File '" + srcFile + "' does not exist");
        }

        if (srcFile.isDirectory()) {
            throw new IOException("File '" + srcFile + "' exists but is a directory");
        }

        try {
            if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
                throw new IOException("File '" + srcFile + "' cannot be copied onto itself");
            }
        } catch (IOException var1) {
            // Commons IO treats an unresolvable path as "not the same file" rather than failing.
            // A browser filesystem has no symlinks to collapse, so canonicalisation failing here
            // means the path simply does not resolve yet, which is not an error worth raising.
        }

        requireParentDirectory(destFile);

        long lastModified = srcFile.lastModified();

        try (InputStream var2 = new FileInputStream(srcFile);
             OutputStream var3 = new FileOutputStream(destFile)) {
            IOUtils.copy(var2, var3);
        }

        if (lastModified > 0L) {
            destFile.setLastModified(lastModified);
        }
    }

    /**
     * Copies a stream to a file, truncating it if it already exists.
     *
     * <p>The stream is not closed. Commons IO does not close it either, and the one caller
     * ({@code HttpTexture}) closes its own response in a finally block.
     */
    public static void copyInputStreamToFile(InputStream source, File destination) throws IOException {
        requireParentDirectory(destination);

        try (OutputStream var2 = new FileOutputStream(destination)) {
            IOUtils.copy(source, var2);
        }
    }

    /**
     * Reads a whole file as text.
     *
     * <p>This is a byte-for-byte read, then a decode. That is Commons IO's
     * {@code IOUtils.toString}, and it is deliberately not
     * {@link IOUtils#inputStreamToString}, which reads line by line: that one drops a trailing
     * newline and rewrites CRLF as LF, which is fine for a line-oriented caller and wrong for a
     * whole-file read that is about to be parsed as JSON.
     */
    public static String readFileToString(File file, Charset charset) throws IOException {
        try (InputStream var2 = new FileInputStream(file)) {
            return new String(readAllBytes(var2), charset);
        }
    }

    /**
     * Reads a whole file as UTF-8, which is what Commons IO's one-argument form does.
     */
    public static String readFileToString(File file) throws IOException {
        return readFileToString(file, StandardCharsets.UTF_8);
    }

    /**
     * Writes text to a file, truncating it if it already exists.
     *
     * <p>The parent directory is created if missing, as Commons IO does, because both callers
     * ({@code RealmsPersistence} and {@code ServerStatsCounter}) write into a directory the
     * launcher is expected to have created but which a fresh profile will not have.
     */
    public static void writeStringToFile(File file, String data, Charset charset) throws IOException {
        requireParentDirectory(file);

        try (OutputStream var2 = new FileOutputStream(file)) {
            var2.write(data.getBytes(charset));
        }
    }

    /**
     * Writes text to a file as UTF-8.
     */
    public static void writeStringToFile(File file, String data) throws IOException {
        writeStringToFile(file, data, StandardCharsets.UTF_8);
    }

    /**
     * Deletes a file, or a directory and everything in it, and throws if it cannot.
     *
     * <p>A missing file counts as deleted. That is Commons IO's behaviour, and it matters at the
     * one call site on the client's path -- {@code Minecraft.archiveProfilingReport} deletes the
     * profiling archive it has just written, and "already gone" is the outcome it wants.
     */
    public static void forceDelete(File file) throws IOException {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] var1 = file.listFiles();

            if (var1 != null) {
                for (File var2 : var1) {
                    forceDelete(var2);
                }
            }
        }

        if (!file.delete()) {
            throw new IOException("Unable to delete file: " + file);
        }
    }

    /**
     * Deletes a file, or a directory and everything in it, and swallows every failure.
     *
     * <p>The swallow is the point of the method, not an oversight: all four callers are
     * clean-up on a path where failing is worse than leaking -- a stale resource-pack file that
     * cannot be removed, or a temp download that cannot be cleaned up.
     */
    public static void deleteQuietly(File file) {
        try {
            forceDelete(file);
        } catch (Throwable var1) {
        }
    }

    /**
     * Lists a directory tree, recursively, keeping the entries the filter accepts.
     *
     * <p>Replaces {@code FileUtils.listFiles(File, FileFilter, IOFileFilter)}. Commons IO's version
     * took a third {@code IOFileFilter} for directories; the only caller passed {@code null} for
     * it, meaning "no restriction", so the parameter is gone rather than kept for a value nobody
     * supplies. Only regular files are returned, which is what the caller then sorts by modified
     * time -- it wants download artefacts, not the directories they sit in.
     *
     * <p>A directory that cannot be listed contributes nothing rather than failing the whole walk.
     */
    public static List<File> listFiles(File directory, FileFilter filter) {
        List<File> var1 = new ArrayList<>();
        collectFiles(directory, filter, var1);
        return var1;
    }

    private static void collectFiles(File directory, FileFilter filter, List<File> out) {
        File[] var1 = directory.listFiles();

        if (var1 == null) {
            return;
        }

        for (File var2 : var1) {
            if (var2.isDirectory()) {
                collectFiles(var2, filter, out);
            } else if (filter.accept(var2)) {
                out.add(var2);
            }
        }
    }

    private static void requireParentDirectory(File file) throws IOException {
        File var1 = file.getParentFile();

        if (var1 != null && !var1.exists() && !var1.mkdirs() && !var1.isDirectory()) {
            throw new IOException("Unable to create directory: " + var1);
        }
    }

    private static byte[] readAllBytes(InputStream stream) throws IOException {
        java.io.ByteArrayOutputStream var1 = new java.io.ByteArrayOutputStream(Math.max(64, stream.available()));
        byte[] var2 = new byte[8192];

        for (int var3 = stream.read(var2); var3 >= 0; var3 = stream.read(var2)) {
            var1.write(var2, 0, var3);
        }

        return var1.toByteArray();
    }
}
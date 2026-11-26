package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

/*
 * Single provider, POSIX separator, one root. provider() and getFileStore() are
 * absent on purpose: java.nio.file.spi.FileSystemProvider and java.nio.file.FileStore
 * are not modelled, and the only caller of a FileSystem in Minecraft
 * (FileZipper / Util.ZIP_FILE_SYSTEM_PROVIDER) was already removed when the zip
 * filesystem went, because the browser client has no real jars.
 *
 * newWatchService() is the exception to "not modelled" and is added here because the
 * one caller that needs it, PackSelectionScreen.Watcher, is on the client's screen
 * path and was reported missing. It hands back a service that can never report a
 * change -- see TWatchService for what that costs -- but that is still the right
 * answer over not linking: the caller's loop then reads as "no changes" instead of
 * failing to link.
 */
public interface TFileSystem {

	String SEPARATOR = "/";

	String getSeparator();

	TPath getPath(String param0, String... param1);

	TPath getPath(URI param0);

	Set<TPath> getRootDirectories();

	/**
	 * Always a fresh instance, as in the JDK, so that closing one cannot affect another
	 * caller. The service is inert either way; see TWatchService.
	 */
	TWatchService newWatchService() throws IOException;

	@Override
	String toString();
}

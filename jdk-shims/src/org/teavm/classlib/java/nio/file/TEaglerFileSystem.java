package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

final class TEaglerFileSystem implements TFileSystem {

	@Override
	public String getSeparator() {
		return "/";
	}

	@Override
	public TPath getPath(String param0, String... param1) {
		StringBuilder var2 = new StringBuilder(param0 == null ? "" : param0);
		if (param1 != null) {
			for (String var3 : param1) {
				if (var3 != null && !var3.isEmpty()) {
					if (var2.length() > 0 && var2.charAt(var2.length() - 1) != '/') {
						var2.append('/');
					}

					var2.append(var3);
				}
			}
		}

		return new TEaglerPath(normalizeSyntax(var2.toString()));
	}

	@Override
	public TPath getPath(URI param0) {
		if (!"file".equalsIgnoreCase(param0.getScheme())) {
			throw new TFileSystemNotFoundException("Only file: URIs are supported, got " + param0.getScheme());
		}

		return new TEaglerPath(normalizeSyntax(param0.getPath() == null ? "" : param0.getPath()));
	}

	@Override
	public Set<TPath> getRootDirectories() {
		return Collections.singleton(new TEaglerPath("/"));
	}

	/**
	 * A fresh instance each call, so callers cannot close each other's service.
	 *
	 * The service itself cannot report anything -- VirtualFileSystem has no change
	 * notification -- which is why Poll returns null unconditionally. See TWatchService.
	 */
	@Override
	public TWatchService newWatchService() {
		return new TWatchService() {
			@Override
			public TWatchKey poll() {
				return null;
			}

			@Override
			public void close() throws IOException {
			}

			@Override
			public String toString() {
				return "eaglercraft-watch-service";
			}
		};
	}

	@Override
	public String toString() {
		return "/";
	}

	/**
	 * Backslash to slash, and collapse of duplicate and trailing slashes. '.' and
	 * '..' are left in place here on purpose: Path.normalize() is a public
	 * operation whose result must still contain them if the caller asked for the
	 * un-normalized path back, and Files resolves them on the way to the store.
	 */
	static String normalizeSyntax(String param0) {
		String var1 = param0.replace('\\', '/');
		var1 = var1.replaceAll("/+", "/");
		if (var1.length() > 1 && var1.endsWith("/")) {
			var1 = var1.substring(0, var1.length() - 1);
		}

		return var1;
	}
}

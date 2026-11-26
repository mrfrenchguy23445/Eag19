package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.File;
import java.net.URI;

public final class TPaths {

	private TPaths() {
	}

	public static TPath get(String param0, String... param1) {
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

		return TFileSystems.getDefault().getPath(var2.toString());
	}

	public static TPath get(URI param0) {
		if (!"file".equalsIgnoreCase(param0.getScheme())) {
			throw new TFileSystemNotFoundException("Only file: URIs are supported, got " + param0.getScheme());
		}

		return TFileSystems.getDefault().getPath(param0.getPath() == null ? "" : param0.getPath());
	}

	public static TPath get(File param0) {
		return TFileSystems.getDefault().getPath(param0.getPath());
	}
}

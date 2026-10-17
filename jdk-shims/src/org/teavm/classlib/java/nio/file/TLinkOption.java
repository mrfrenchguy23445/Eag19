package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

/*
 * Marker options. Both values are accepted and neither changes anything: there is
 * no symbolic link support in the virtual filesystem, so there is nothing for
 * NOFOLLOW_LINKS to skip.
 */
public enum TLinkOption implements org.teavm.classlib.java.nio.file.TCopyOption {

	NOFOLLOW_LINKS;

	/**
	 * Declared on the enum in the JDK too, not inherited from CopyOption -- the
	 * interface is only a marker there.
	 */
	public boolean followLinks() {
		return false;
	}
}

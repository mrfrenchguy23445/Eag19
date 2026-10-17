package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

/**
 * FOLLOW_LINKS is accepted and ignored: the virtual filesystem has no symbolic
 * links, so there is nothing to follow and nothing to refuse to follow.
 */
public enum TFileVisitOption implements java.nio.file.OpenOption {

	FOLLOW_LINKS
}

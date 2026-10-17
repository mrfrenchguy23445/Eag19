package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

public enum TStandardCopyOption implements TCopyOption, OpenOption {

	REPLACE_EXISTING,
	COPY_ATTRIBUTES,
	ATOMIC_MOVE,
	REPLACE_EXISTING_ATOMIC_MOVE
}

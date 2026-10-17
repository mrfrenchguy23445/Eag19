package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

public enum TFileVisitResult {

	CONTINUE,
	TERMINATE,
	SKIP_SUBTREE;

	public boolean isTerminate() {
		return this == TERMINATE;
	}

	public boolean isSkipSubtree() {
		return this == SKIP_SUBTREE;
	}

	public boolean isContinue() {
		return this == CONTINUE;
	}
}

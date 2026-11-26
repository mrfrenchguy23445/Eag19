/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * org.apache.log4j.Category.setLevel delegates to this in the real 1.2 bridge.
 */
package org.apache.log4j.legacy.core;

/** Reached from org.apache.log4j.Category.setLevel, matching the real log4j 1.2 bridge. */
public class CategoryUtil {
	private CategoryUtil() {
	}

	public static void setLevel(org.apache.logging.log4j.core.Logger logger, org.apache.log4j.Category category) {
	}

	public static void setLevel(org.apache.logging.log4j.core.Logger logger, String category, org.apache.log4j.Level level) {
	}
}

/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * Marker is the null-marker parameter netty passes on every log call.
 */
package org.apache.logging.log4j;

public interface Marker {
	String getName();
}

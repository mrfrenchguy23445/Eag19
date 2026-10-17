/*
 * Hand-written stub -- see jdk-shims/build.gradle.kts.
 *
 * netty's Log4JLogger passes log4j 1.2 Level values as Priority to Logger.log().
 */
package org.apache.log4j;

public interface Priority {
	int getPriority();

	boolean isGreaterOrEqual(Priority other);
}

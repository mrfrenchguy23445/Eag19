package org.teavm.classlib.java.nio.file.attribute;

import java.nio.file.attribute.*;

/**
 * Only name() and value() are ever read, and only by code that passes an empty
 * varargs -- Minecraft never supplies an attribute. value() therefore always
 * returns null; the file system has no attribute store to read it from.
 */
public interface TFileAttribute<T> {

	String name();

	T value();
}

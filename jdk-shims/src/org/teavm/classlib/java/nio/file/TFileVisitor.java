package org.teavm.classlib.java.nio.file;

import java.nio.file.*;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

public interface TFileVisitor<T> {

	TFileVisitResult preVisitDirectory(T param0, BasicFileAttributes param1) throws IOException;

	TFileVisitResult visitFile(T param0, BasicFileAttributes param1) throws IOException;

	TFileVisitResult visitFileFailed(T param0, IOException param1) throws IOException;

	TFileVisitResult postVisitDirectory(T param0, IOException param1) throws IOException;
}

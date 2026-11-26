package org.teavm.classlib.java.nio.file;

/**
 * Port note (wasm14): java.nio.file.InvalidPathException, which the classlib does not provide.
 *
 * <p>Two sites in the game name it, and they are two halves of one path: net.minecraft.FileUtil
 * throws it when a resource name comes back empty, and StructureTemplateManager catches it when
 * turning a ResourceLocation into a path for a .snbt file. Because a catch clause references the
 * class exactly as a throw does, rewriting the throw to IllegalArgumentException would not have
 * been enough on its own -- the catch would still have failed to resolve the type.
 *
 * <p>It lives here rather than at a call site because it is an ordinary absent class, exactly
 * like TFileSystemNotFoundException next door: the classlib has TAccessDeniedException,
 * TFileSystemException, TNoSuchFileException and friends but never this one. The name comes from
 * the classlib's own mapPackageHierarchy rule (org.teavm.classlib.java -> java with the leading T
 * stripped), so no teavm.properties key is needed.
 *
 * <p>Only the JDK constructors actually used by the game are implemented -- the two-argument form
 * that FileUtil calls, plus the index-carrying overload for completeness. Nothing asks this class
 * for anything beyond what IllegalArgumentException already gives.
 */
public class TInvalidPathException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;

	private final String path;

	public TInvalidPathException(String param0, String param1) {
		super(param1);
		this.path = param0;
	}

	public TInvalidPathException(String param0, String param1, char param2) {
		super(param1);
		this.path = param0;
	}

	public String getInput() {
		return this.path;
	}

	public String getReason() {
		return super.getMessage();
	}
}

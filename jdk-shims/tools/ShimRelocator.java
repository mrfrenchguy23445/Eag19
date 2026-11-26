import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.teavm.asm.ClassReader;
import org.teavm.asm.ClassVisitor;
import org.teavm.asm.ClassWriter;
import org.teavm.asm.commons.ClassRemapper;
import org.teavm.asm.commons.Remapper;

/**
 * Relocates the compiled jdk-shims into the namespace the TeaVM classlib reserves for itself.
 *
 * <p>TeaVM resolves a class by asking {@code ClasspathResourceMapper.apply(name)}, whose final
 * fallback is {@code ClassLoader.getResourceAsStream(name.replace('.','/') + ".class")}. For any
 * {@code java.*} / {@code javax.*} name the bootstrap loader answers that request from the running
 * JDK's own module image, so a jar on the classpath is never consulted -- a shim packaged as
 * {@code java/util/concurrent/CompletableFuture.class} is invisible, and TeaVM silently compiles the
 * host JDK's real implementation instead.
 *
 * <p>The classlib avoids this by keeping its classes under
 * {@code org.teavm.classlib.java} with a {@code T} on each simple name, and declaring, in its own
 * {@code META-INF/teavm.properties}:
 *
 * <pre>
 * includePackageHierarchy|java=false
 * stripPrefixFromPackageHierarchyClasses|org.teavm.classlib.java=T
 * mapPackageHierarchy|org.teavm.classlib.java=java
 * </pre>
 *
 * so {@code java.util.concurrent.CompletableFuture} is served from
 * {@code org/teavm.classlib.java.util.concurrent.TCompletableFuture} while every reference inside
 * the program keeps its public {@code java.*} spelling. Those three lines already ship with
 * teavm-classlib, so adding classes in that namespace needs no properties at all.
 *
 * <p>So this tool performs exactly one rewrite: the shims' own names move from
 * {@code java/util/concurrent/CompletableFuture} to
 * {@code org/teavm/classlib/java/util/concurrent/TCompletableFuture} (nested
 * {@code $Inner} names keep their suffix, so {@code CompletableFuture$Dep} becomes
 * {@code TCompletableFuture$Dep}). References between shim classes are rewritten to match;
 * references to everything else -- {@code java/lang/Object}, {@code java/util/Objects},
 * {@code javax/sound/...} -- are left untouched so they keep resolving through the classlib.
 */
public final class ShimRelocator {
    /** Namespace the classlib claims; see the class comment. Trailing slash, slash-separated. */
    private static final String CLASSLIB_ROOT = "org/teavm/classlib/";
    private static final String[] SHIM_PREFIXES = { "java/", "javax/" };

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("usage: ShimRelocator <in.jar|dir> <out.jar|dir> <workDir>");
            System.exit(2);
        }
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Path work = Path.of(args[2]);
        Files.createDirectories(work);

        List<Entry> entries = new ArrayList<>();
        if (Files.isDirectory(in)) {
            Files.walkFileTree(in, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".class")) {
                        entries.add(new Entry(in.relativize(file).toString().replace('\\', '/'), file));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            try (ZipFile zip = new ZipFile(in.toFile())) {
                var it = zip.entries();
                while (it.hasMoreElements()) {
                    ZipEntry e = it.nextElement();
                    if (!e.isDirectory() && e.getName().endsWith(".class")) {
                        Path tmp = work.resolve(e.getName().replace('/', '_'));
                        try (InputStream is = zip.getInputStream(e)) {
                            Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                        entries.add(new Entry(e.getName(), tmp));
                    }
                }
            }
        }

        // The rewrite set is exactly "the classes in this jar", so a shim that mentions a real JDK
        // type is never renamed by accident, and a shim referring to a sibling shim always is.
        // Names here are ASM internal names (slash-separated), which is also what Remapper.map
        // is handed -- mixing the dotted form in would silently match nothing.
        Set<String> ownNames = new HashSet<>();
        for (Entry e : entries) {
            ownNames.add(internalName(e.path));
        }

        Remapper remapper = new Remapper() {
            @Override
            public String map(String internalName) {
                if (internalName == null || !ownNames.contains(internalName)) {
                    return internalName;
                }
                return relocate(internalName);
            }
        };

        List<Out> outputs = new ArrayList<>();
        for (Entry e : entries) {
            byte[] original;
            try (InputStream is = Files.newInputStream(e.source)) {
                original = is.readAllBytes();
            }
            ClassReader reader = new ClassReader(original);
            ClassWriter writer = new ClassWriter(0);
            reader.accept(new ClassRemapper(writer, remapper), 0);
            String newPath = relocate(internalName(e.path)) + ".class";
            outputs.add(new Out(newPath, writer.toByteArray()));
        }

        // TeaVM looks a class up by resource path but reads its identity out of the bytecode, so
        // a path that disagrees with the internal name would be picked up and then silently
        // treated as a different class. Check the two agree, and that the T-namespace move stuck.
        List<String> problems = new ArrayList<>();
        for (Out o : outputs) {
            String expected = relocate(internalName(o.path));
            String actual = new ClassReader(o.bytes).getClassName().replace('.', '/');
            if (!expected.equals(o.path.substring(0, o.path.length() - 6))) {
                problems.add("entry path is not the relocated name: " + o.path);
            }
            if (!expected.equals(actual)) {
                problems.add("internal name is not the relocated name: " + actual + " (expected " + expected + ")");
            }
        }
        if (!problems.isEmpty()) {
            for (String p : problems) {
                System.err.println("ERROR " + p);
            }
            System.exit(1);
        }

        boolean outIsDirectory = Files.isDirectory(out);
        if (!outIsDirectory && out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }

        if (outIsDirectory) {
            for (Out o : outputs) {
                Path target = out.resolve(o.path);
                Files.createDirectories(target.getParent());
                try (OutputStream os = Files.newOutputStream(target)) {
                    os.write(o.bytes);
                }
            }
        } else {
            try (OutputStream fileOut = Files.newOutputStream(out);
                    JarOutputStream jos = new JarOutputStream(fileOut)) {
                // STORED, like the rest of the build: TeaVM reads these with getResourceAsStream
                // and a deflated entry would be re-inflated on every lookup.
                for (Out o : outputs) {
                    JarEntry je = new JarEntry(o.path);
                    je.setMethod(ZipEntry.STORED);
                    je.setSize(o.bytes.length);
                    je.setCompressedSize(o.bytes.length);
                    je.setCrc(crc32(o.bytes));
                    jos.putNextEntry(je);
                    jos.write(o.bytes);
                    jos.closeEntry();
                }
            }
        }
        System.out.println("relocated " + outputs.size() + " classes -> " + out);
    }

    /**
     * {@code java/util/concurrent/CompletableFuture} to
     * {@code org/teavm/classlib/java/util/concurrent/TCompletableFuture}. Takes and returns an
     * ASM internal name, so the separator stays {@code /} throughout and the simple name -- which
     * for a nested class still carries its {@code $Inner} suffix -- gets the {@code T} in front of
     * it.
     */
    private static String relocate(String internalName) {
        for (String prefix : SHIM_PREFIXES) {
            if (internalName.startsWith(prefix)) {
                String rest = internalName.substring(prefix.length());
                int lastSlash = rest.lastIndexOf('/');
                return CLASSLIB_ROOT + prefix + rest.substring(0, lastSlash + 1)
                        + "T" + rest.substring(lastSlash + 1);
            }
        }
        return internalName;
    }

    /** Entry/resource path ({@code a/b/C.class}) to ASM internal name ({@code a/b/C}). */
    private static String internalName(String classPath) {
        return classPath.endsWith(".class")
                ? classPath.substring(0, classPath.length() - 6)
                : classPath;
    }

    private static long crc32(byte[] data) {
        CRC32 c = new CRC32();
        c.update(data);
        return c.getValue();
    }

    private record Entry(String path, Path source) { }

    private record Out(String path, byte[] bytes) { }
}
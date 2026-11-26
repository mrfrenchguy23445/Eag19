package net.minecraft;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformRuntime;
import org.slf4j.Logger;

public class SystemReport {
   public static final long BYTES_PER_MEBIBYTE = 1048576L;
   private static final long ONE_GIGA = 1000000000L;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String OPERATING_SYSTEM = System.getProperty("os.name")
      + " ("
      + System.getProperty("os.arch")
      + ") version "
      + System.getProperty("os.version");
   private static final String JAVA_VERSION = System.getProperty("java.version") + ", " + System.getProperty("java.vendor");
   private static final String JAVA_VM_VERSION = System.getProperty("java.vm.name")
      + " ("
      + System.getProperty("java.vm.info")
      + "), "
      + System.getProperty("java.vm.vendor");
   private final Map<String, String> entries = Maps.newLinkedHashMap();

   public SystemReport() {
      this.setDetail("Minecraft Version", SharedConstants.getCurrentVersion().getName());
      this.setDetail("Minecraft Version ID", SharedConstants.getCurrentVersion().getId());
      this.setDetail("Operating System", OPERATING_SYSTEM);
      this.setDetail("Java Version", JAVA_VERSION);
      this.setDetail("Java VM Version", JAVA_VM_VERSION);
      // Runtime.maxMemory()/totalMemory()/freeMemory() and availableProcessors() are absent from
      // the WASM-GC TeaVM classlib. PlatformRuntime exposes the equivalent quantities for the
      // WASM heap. availableProcessors() has no WASM counterpart -- the browser build runs on a
      // single thread -- so it reports 1.
      this.setDetail("Memory", () -> {
         long l_var1 = PlatformRuntime.maxMemory();
         long l_var3 = PlatformRuntime.totalMemory();
         long l_var5 = PlatformRuntime.freeMemory();
         long l_var7 = l_var1 / 1048576L;
         long l_var9 = l_var3 / 1048576L;
         long l_var11 = l_var5 / 1048576L;
         return l_var5 + " bytes (" + l_var11 + " MiB) / " + l_var3 + " bytes (" + l_var9 + " MiB) up to " + l_var1 + " bytes (" + l_var7 + " MiB)";
      });
      this.setDetail("CPUs", () -> String.valueOf(1));
      this.ignoreErrors("hardware", () -> this.putHardware());
      this.setDetail("JVM Flags", () -> {
         List<String> l_var0 = Util.getVmArguments().collect(Collectors.toList());
         return String.format(Locale.ROOT, "%d total; %s", l_var0.size(), String.join(" ", l_var0));
      });
   }

   public void setDetail(String param0, String param1) {
      this.entries.put(param0, param1);
   }

   public void setDetail(String param0, Supplier<String> param1) {
      try {
         this.setDetail(param0, param1.get());
      } catch (Exception var3) {
         LOGGER.warn("Failed to get system info for {}", param0, var3);
         this.setDetail(param0, "ERR");
      }
   }

   /**
    * Records the host environment details that are meaningful for a browser client.
    *
    * <p>Upstream queries the OS through {@code oshi.SystemInfo}, which reports the physical CPU,
    * the discrete graphics cards and the physical/virtual memory layout. {@code oshi} is not
    * available in the WASM-GC TeaVM build: it reaches into native code and its classes are not in
    * the TeaVM classlib. The WASM client has no view of the host hardware at all -- it sees a
    * single-threaded WebAssembly module with a linear-memory heap -- so the details are taken
    * from {@link PlatformRuntime} (GL renderer and version) and the heap sizes reported above.
    */
   private void putHardware() {
      this.ignoreErrors("graphics", () -> this.setDetail("Graphics card #0 name", PlatformRuntime::getGLRenderer));
      this.ignoreErrors("graphics", () -> this.setDetail("Graphics card #0 versionInfo", PlatformRuntime::getGLVersion));
      this.ignoreErrors("graphics", () -> this.setDetail("Graphics card #0 vendor", () -> "WebGL/WebGL2"));
   }

   private void ignoreErrors(String param0, Runnable param1) {
      try {
         param1.run();
      } catch (Throwable var3) {
         LOGGER.warn("Failed retrieving info for group {}", param0, var3);
      }
   }

   public void appendToCrashReportString(StringBuilder param0) {
      param0.append("-- ").append("System Details").append(" --\n");
      param0.append("Details:");
      this.entries.forEach((l_param1, l_param2) -> {
         param0.append("\n\t");
         param0.append(l_param1);
         param0.append(": ");
         param0.append(l_param2);
      });
   }

   public String toLineSeparatedString() {
      return this.entries
         .entrySet()
         .stream()
         .map(l_param0 -> l_param0.getKey() + ": " + l_param0.getValue())
         .collect(Collectors.joining(System.lineSeparator()));
   }
}

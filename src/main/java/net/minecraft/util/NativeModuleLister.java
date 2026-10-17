package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.CrashReportCategory;
import org.slf4j.Logger;

/**
 * Enumerates the operating system's loaded native modules for the crash report.
 *
 * <p>Upstream drives this through JNA: {@code Platform.isWindows()} gates it, then
 * {@code Kernel32.INSTANCE.GetCurrentProcessId()} and {@code Kernel32Util.getModules} enumerate the
 * process's modules and {@code Version.INSTANCE.GetFileVersionInfo*} reads each one's version
 * resource. None of that exists here. JNA is a JNI library, so it is not merely missing from the
 * WASM-GC TeaVM classlib, it cannot work on the target at all -- there is no native process, no
 * module table and no PE version resource to read. The upstream code already returns an empty list
 * on every non-Windows platform, so an always-empty result is a case the original design already
 * handles; on this target it is simply the only case.
 *
 * <p>The JNA calls are deleted rather than guarded. An {@code if} would not help: TeaVM compiles
 * the whole method body regardless of which branch is reachable, so a JNA reference anywhere in
 * {@link #listModules()} -- even inside a branch that never runs -- fails the build on the missing
 * class. The same reasoning is why {@code NativeModuleLister} keeps no Windows-only helper at all.
 *
 * <p>{@link NativeModuleInfo} and {@link NativeModuleVersion} are retained. They are public types
 * and the crash-report rendering path names them, so dropping them would be a wider API change than
 * the behaviour they now describe (nothing) justifies.
 */
public class NativeModuleLister {
   private static final Logger LOGGER = LogUtils.getLogger();

   /** Always empty: a WASM instance has no native modules. */
   public static List<NativeModuleLister.NativeModuleInfo> listModules() {
      return ImmutableList.of();
   }

   public static void addCrashSection(CrashReportCategory param0) {
      // Kept wired up rather than removed: the crash screen always renders a "Modules" row, and an
      // empty one is the truthful value here. Removing the row would change the crash screen layout
      // for no diagnostic gain.
      param0.setDetail(
         "Modules",
         () -> listModules()
            .stream()
            .sorted(Comparator.comparing(l2_param0 -> l2_param0.name))
            .map(l2_param0 -> "\n\t\t" + l2_param0)
            .collect(Collectors.joining())
      );
   }

   public static class NativeModuleInfo {
      public final String name;
      public final Optional<NativeModuleLister.NativeModuleVersion> version;

      public NativeModuleInfo(String param0, Optional<NativeModuleLister.NativeModuleVersion> param1) {
         this.name = param0;
         this.version = param1;
      }

      @Override
      public String toString() {
         return this.version.<String>map(l_param0 -> this.name + ":" + l_param0).orElse(this.name);
      }
   }

   public static class NativeModuleVersion {
      public final String description;
      public final String version;
      public final String company;

      public NativeModuleVersion(String param0, String param1, String param2) {
         this.description = param0;
         this.version = param1;
         this.company = param2;
      }

      @Override
      public String toString() {
         return this.description + ":" + this.version + ":" + this.company;
      }
   }
}
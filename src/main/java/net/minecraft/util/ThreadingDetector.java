package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import org.slf4j.Logger;

public class ThreadingDetector {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String name;
   // Port note (wasm14): the Semaphore, the reentrant lock it was guarded with, and the two
   // fields that carried a deferred exception are gone with checkAndLock/checkAndUnlock below.

   public ThreadingDetector(String param0) {
      this.name = param0;
   }

   // Port note (wasm14): the contention guard is a no-op in this build, which exists to reach
   // the title screen. Upstream it parked the caller on a java.util.concurrent.Semaphore until
   // the other thread finished, then threw an "accessing X from multiple threads" report. That
   // type is absent from the WASM-GC classlib, and every PalettedContainer construction carried
   // a reference to it, so five separate link failures came out of one debug-only assertion --
   // each reached from chunk state, i.e. from rendering, not from any server code.
   //
   // The guard only ever mattered because two threads could touch one container. Every executor
   // on this port is a direct executor, so there is no second thread for it to catch; a no-op is
   // what it effectively already was. It comes back with the semaphore, along with the rest of
   // the multi-threaded build.
   //
   // makeThreadingException is left alone: LegacyRandomSource still throws it when a worldgen
   // source is used off its owning thread, and that path never needed a Semaphore.
   public void checkAndLock() {
   }

   public void checkAndUnlock() {
   }

   public static ReportedException makeThreadingException(String param0, @Nullable Thread param1) {
      String var2 = Stream.of(Thread.currentThread(), param1).filter(Objects::nonNull).map(ThreadingDetector::stackTrace).collect(Collectors.joining("\n"));
      String var3 = "Accessing " + param0 + " from multiple threads";
      CrashReport var4 = new CrashReport(var3, new IllegalStateException(var3));
      CrashReportCategory var5 = var4.addCategory("Thread dumps");
      var5.setDetail("Thread dumps", var2);
      LOGGER.error("Thread dumps: \n" + var2);
      return new ReportedException(var4);
   }

   private static String stackTrace(Thread param0) {
      return param0.getName() + ": \n\tat " + Arrays.stream(param0.getStackTrace()).map(Object::toString).collect(Collectors.joining("\n\tat "));
   }
}

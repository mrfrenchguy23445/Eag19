package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.net.SocketAddress;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public interface JvmProfiler {
   /**
    * Always the no-op profiler.
    *
    * <p>Upstream probes for JFR with {@code Runtime.class.getModule().getLayer().findModule("jdk.jfr")}.
    * Neither {@code java.lang.Module} nor {@code Class.getModule()} exists in the WASM-GC TeaVM
    * classlib, and that is not incidental: {@code jdk.jfr} is a JVM-internal module with no analogue
    * on a single-threaded WASM heap, so the probe could only ever have answered "absent" here.
    *
    * <p>Hard-coding that answer has a second effect worth stating, because it is why this is a
    * direct constant rather than a cheaper substitute: {@code INSTANCE} is the only reference to
    * {@link JfrProfiler} in the codebase, so making it unconditional also makes the entire JFR
    * backend -- its {@code jdk.jfr.recorder} imports and file-rotation logic -- unreachable, and
    * therefore uncompiled. Profiling still works; it just uses the built-in
    * {@link net.minecraft.util.profiling.ProfilerFiller} timers rather than JFR events.
    */
   JvmProfiler INSTANCE = new JvmProfiler.NoOpProfiler();

   boolean start(Environment var1);

   Path stop();

   boolean isRunning();

   boolean isAvailable();

   void onServerTick(float var1);

   void onPacketReceived(int var1, int var2, SocketAddress var3, int var4);

   void onPacketSent(int var1, int var2, SocketAddress var3, int var4);

   @Nullable
   ProfiledDuration onWorldLoadedStarted();

   @Nullable
   ProfiledDuration onChunkGenerate(ChunkPos var1, ResourceKey<Level> var2, String var3);

   class NoOpProfiler implements JvmProfiler {
      private static final Logger LOGGER = LogUtils.getLogger();
      static final ProfiledDuration noOpCommit = () -> {};

      @Override
      public boolean start(Environment param0) {
         LOGGER.warn("Attempted to start Flight Recorder, but it's not supported on this JVM");
         return false;
      }

      @Override
      public Path stop() {
         throw new IllegalStateException("Attempted to stop Flight Recorder, but it's not supported on this JVM");
      }

      @Override
      public boolean isRunning() {
         return false;
      }

      @Override
      public boolean isAvailable() {
         return false;
      }

      @Override
      public void onPacketReceived(int param0, int param1, SocketAddress param2, int param3) {
      }

      @Override
      public void onPacketSent(int param0, int param1, SocketAddress param2, int param3) {
      }

      @Override
      public void onServerTick(float param0) {
      }

      @Override
      public ProfiledDuration onWorldLoadedStarted() {
         return noOpCommit;
      }

      @Nullable
      @Override
      public ProfiledDuration onChunkGenerate(ChunkPos param0, ResourceKey<Level> param1, String param2) {
         return null;
      }
   }
}

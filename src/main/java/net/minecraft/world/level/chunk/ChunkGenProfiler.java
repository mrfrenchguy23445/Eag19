package net.minecraft.world.level.chunk;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

/**
 * Accumulates wall-time spent per {@link ChunkStatus} stage during chunk
 * generation. On this wasm-gc build everything runs on a single cooperative
 * thread (the "Chunk source main thread executor" is a fake executor drained
 * on the browser main loop, not real parallelism), so the elapsed time of a
 * generation future is a faithful measure of the CPU time that stage consumed.
 *
 * Purpose: turn "Preparing spawn area is really slow" into attributable
 * numbers (FEATURES vs LIGHT vs FULL vs ...). Logs a compact summary line
 * every {@link #LOG_INTERVAL} completions via the standard Minecraft logger.
 * Costs a few nanoseconds per record; safe to leave in production paths.
 */
final class ChunkGenProfiler {

   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int LOG_INTERVAL = 512;

   private static final Map<String, long[]> STAGES = new HashMap<>(); // name -> [count, nanos]
   private static long completed;
   private static long windowStartNanos = System.nanoTime();

   private ChunkGenProfiler() {
   }

   static void record(String stageName, long nanos) {
      long[] acc = STAGES.computeIfAbsent(stageName, k -> new long[2]);
      acc[0]++;
      acc[1] += nanos;
      if (++completed % LOG_INTERVAL == 0) {
         flush();
      }
   }

   private static void flush() {
      long now = System.nanoTime();
      double windowSec = (now - windowStartNanos) / 1_000_000_000.0;
      if (windowSec <= 0.0) {
         return;
      }
      StringBuilder sb = new StringBuilder(160);
      sb.append(String.format("chunks/s=%.1f over %.1fs | ", completed / windowSec, windowSec));
      for (Map.Entry<String, long[]> e : STAGES.entrySet()) {
         long[] acc = e.getValue();
         sb.append(String.format("%s: n=%d avg=%.2fms total=%.1fs; ",
            e.getKey(), acc[0], (acc[1] / 1e6) / Math.max(1, acc[0]), acc[1] / 1e9));
      }
      LOGGER.info(sb.toString());
      // Reset window so the next line reflects the recent rate, not lifetime average.
      completed = 0;
      windowStartNanos = now;
      STAGES.clear();
   }
}

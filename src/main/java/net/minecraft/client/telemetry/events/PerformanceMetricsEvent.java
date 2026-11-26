package net.minecraft.client.telemetry.events;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;

public final class PerformanceMetricsEvent extends AggregatedTelemetryEvent {
   // WASM-GC has a single fixed WebAssembly.Memory that the GC never grows or shrinks,
   // so there is no separate maximum to report -- the heap ceiling and the live heap size
   // are the same number. totalMemory() is what the classlib models; maxMemory() does not
   // exist on it. Telemetry is opt-in and never leaves the browser regardless.
   private static final long DEDICATED_MEMORY_KB = toKilobytes(Runtime.getRuntime().totalMemory());
   private final LongList fpsSamples = new LongArrayList();
   private final LongList frameTimeSamples = new LongArrayList();
   private final LongList usedMemorySamples = new LongArrayList();

   @Override
   public void tick(TelemetryEventSender param0) {
      if (Minecraft.getInstance().telemetryOptInExtra()) {
         super.tick(param0);
      }
   }

   private void resetValues() {
      this.fpsSamples.clear();
      this.frameTimeSamples.clear();
      this.usedMemorySamples.clear();
   }

   @Override
   public void takeSample() {
      this.fpsSamples.add(Minecraft.getInstance().getFps());
      this.takeUsedMemorySample();
      this.frameTimeSamples.add(Minecraft.getInstance().getFrameTimeNs());
   }

   private void takeUsedMemorySample() {
      long var1 = Runtime.getRuntime().totalMemory();
      long var3 = Runtime.getRuntime().freeMemory();
      long var5 = var1 - var3;
      this.usedMemorySamples.add(toKilobytes(var5));
   }

   @Override
   public void sendEvent(TelemetryEventSender param0) {
      param0.send(TelemetryEventType.PERFORMANCE_METRICS, l_param0 -> {
         l_param0.put(TelemetryProperty.FRAME_RATE_SAMPLES, new LongArrayList(this.fpsSamples));
         l_param0.put(TelemetryProperty.RENDER_TIME_SAMPLES, new LongArrayList(this.frameTimeSamples));
         l_param0.put(TelemetryProperty.USED_MEMORY_SAMPLES, new LongArrayList(this.usedMemorySamples));
         l_param0.put(TelemetryProperty.NUMBER_OF_SAMPLES, this.getSampleCount());
         l_param0.put(TelemetryProperty.RENDER_DISTANCE, Minecraft.getInstance().options.getEffectiveRenderDistance());
         l_param0.put(TelemetryProperty.DEDICATED_MEMORY_KB, (int)DEDICATED_MEMORY_KB);
      });
      this.resetValues();
   }

   private static long toKilobytes(long param0) {
      return param0 / 1000L;
   }
}

package net.minecraft.util.profiling.metrics.profiling;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;
import net.minecraft.util.profiling.ProfileCollector;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsRegistry;
import net.minecraft.util.profiling.metrics.MetricsSamplerProvider;
import org.slf4j.Logger;

public class ServerMetricsSamplersProvider implements MetricsSamplerProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Set<MetricSampler> samplers = new ObjectOpenHashSet<>();
   private final ProfilerSamplerAdapter samplerFactory = new ProfilerSamplerAdapter();

   public ServerMetricsSamplersProvider(LongSupplier param0, boolean param1) {
      this.samplers.add(tickTimeSampler(param0));
      if (param1) {
         this.samplers.addAll(runtimeIndependentSamplers());
      }
   }

   public static Set<MetricSampler> runtimeIndependentSamplers() {
      Builder<MetricSampler> var0 = ImmutableSet.builder();

      try {
         ServerMetricsSamplersProvider.CpuStats var1 = new ServerMetricsSamplersProvider.CpuStats();
         IntStream.range(0, var1.nrOfCpus)
            .mapToObj(l_param1 -> MetricSampler.create("cpu#" + l_param1, MetricCategory.CPU, () -> var1.loadForCpu(l_param1)))
            .forEach(var0::add);
      } catch (Throwable var1) {
         LOGGER.warn("Failed to query cpu, no cpu stats will be recorded", var1);
      }

      var0.add(
         MetricSampler.create(
            "heap MiB", MetricCategory.JVM, () -> (float)(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576.0F
         )
      );
      var0.addAll(MetricsRegistry.INSTANCE.getRegisteredSamplers());
      return var0.build();
   }

   @Override
   public Set<MetricSampler> samplers(Supplier<ProfileCollector> param0) {
      this.samplers.addAll(this.samplerFactory.newSamplersFoundInProfiler(param0));
      return this.samplers;
   }

   public static MetricSampler tickTimeSampler(final LongSupplier param0) {
      Stopwatch var1 = Stopwatch.createUnstarted(new Ticker() {
         @Override
         public long read() {
            return param0.getAsLong();
         }
      });
      ToDoubleFunction<Stopwatch> var2 = l_param0 -> {
         if (l_param0.isRunning()) {
            l_param0.stop();
         }

         long l_var1 = l_param0.elapsed(TimeUnit.NANOSECONDS);
         l_param0.reset();
         return l_var1;
      };
      MetricSampler.ValueIncreasedByPercentage var3 = new MetricSampler.ValueIncreasedByPercentage(2.0F);
      return MetricSampler.builder("ticktime", MetricCategory.TICK_LOOP, var2, var1).withBeforeTick(Stopwatch::start).withThresholdAlert(var3).build();
   }

   static class CpuStats {
      /*
       * PORT NOTE (wasm_gc): upstream instantiates oshi.SystemInfo here, which
       * is a jna-native stack the TeaVM classlib cannot link (and cannot run
       * in a browser anyway). Port note mirrored from net.minecraft.SystemReport:
       * answer "1 logical cpu at 0% load" so the boot-time
       * runtimeIndependentSamplers() call still builds its samplers, and the
       * profile screen shows cpu#0 at 0.0 load.
       */
      public final int nrOfCpus = 1;
      private double[] currentLoad = new double[] { 0.0 };
      private long lastPollMs;

      public double loadForCpu(int param0) {
         this.lastPollMs = System.currentTimeMillis();
         return this.currentLoad[param0] * 100.0;
      }
   }
}

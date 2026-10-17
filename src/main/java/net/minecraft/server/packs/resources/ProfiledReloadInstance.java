package net.minecraft.server.packs.resources;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.Util;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.ProfileResults;
import org.slf4j.Logger;

public class ProfiledReloadInstance extends SimpleReloadInstance<ProfiledReloadInstance.State> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Stopwatch total = Stopwatch.createUnstarted();

   public ProfiledReloadInstance(
      ResourceManager param0, List<PreparableReloadListener> param1, Executor param2, Executor param3, CompletableFuture<Unit> param4
   ) {
      super(param2, param3, param0, param1, (l_param1, l_param2, l_param3, l_param4, l_param5) -> {
         AtomicLong l_var6 = new AtomicLong();
         AtomicLong l_var7 = new AtomicLong();
         ActiveProfiler l_var8 = new ActiveProfiler(Util.timeSource, () -> 0, false);
         ActiveProfiler l_var9 = new ActiveProfiler(Util.timeSource, () -> 0, false);
         CompletableFuture<Void> l_var10 = l_param3.reload(l_param1, l_param2, l_var8, l_var9, l2_param2 -> l_param4.execute(() -> {
            long l3_var2 = Util.getNanos();
            l2_param2.run();
            l_var6.addAndGet(Util.getNanos() - l3_var2);
         }), l2_param2 -> l_param5.execute(() -> {
            long l3_var2 = Util.getNanos();
            l2_param2.run();
            l_var7.addAndGet(Util.getNanos() - l3_var2);
         }));
         return l_var10.thenApplyAsync(l2_param5 -> {
            LOGGER.debug("Finished reloading " + l_param3.getName());
            return new ProfiledReloadInstance.State(l_param3.getName(), l_var8.getResults(), l_var9.getResults(), l_var6, l_var7);
         }, param3);
      }, param4);
      this.total.start();
      this.allDone = this.allDone.thenApplyAsync(this::finish, param3);
   }

   private List<ProfiledReloadInstance.State> finish(List<ProfiledReloadInstance.State> param0) {
      this.total.stop();
      long var2 = 0L;
      LOGGER.info("Resource reload finished after {} ms", this.total.elapsed(TimeUnit.MILLISECONDS));

      for (ProfiledReloadInstance.State var5 : param0) {
         ProfileResults var6 = var5.preparationResult;
         ProfileResults var7 = var5.reloadResult;
         long var8 = TimeUnit.NANOSECONDS.toMillis(var5.preparationNanos.get());
         long var10 = TimeUnit.NANOSECONDS.toMillis(var5.reloadNanos.get());
         long var12 = var8 + var10;
         String var14 = var5.name;
         LOGGER.info("{} took approximately {} ms ({} ms preparing, {} ms applying)", var14, var12, var8, var10);
         var2 += var10;
      }

      LOGGER.info("Total blocking time: {} ms", var2);
      return param0;
   }

   public static class State {
      final String name;
      final ProfileResults preparationResult;
      final ProfileResults reloadResult;
      final AtomicLong preparationNanos;
      final AtomicLong reloadNanos;

      State(String param0, ProfileResults param1, ProfileResults param2, AtomicLong param3, AtomicLong param4) {
         this.name = param0;
         this.preparationResult = param1;
         this.reloadResult = param2;
         this.preparationNanos = param3;
         this.reloadNanos = param4;
      }
   }
}

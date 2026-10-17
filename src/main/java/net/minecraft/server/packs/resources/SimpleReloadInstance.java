package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.Util;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.InactiveProfiler;

public class SimpleReloadInstance<S> implements ReloadInstance {
   private static final int PREPARATION_PROGRESS_WEIGHT = 2;
   private static final int EXTRA_RELOAD_PROGRESS_WEIGHT = 2;
   private static final int LISTENER_PROGRESS_WEIGHT = 1;
   protected final CompletableFuture<Unit> allPreparations = new CompletableFuture<>();
   protected CompletableFuture<List<S>> allDone;
   final Set<PreparableReloadListener> preparingListeners;
   private final int listenerCount;
   private int startedReloads;
   private int finishedReloads;
   private final AtomicInteger startedTaskCounter = new AtomicInteger();
   private final AtomicInteger doneTaskCounter = new AtomicInteger();

   public static SimpleReloadInstance<Void> of(
      ResourceManager param0, List<PreparableReloadListener> param1, Executor param2, Executor param3, CompletableFuture<Unit> param4
   ) {
      return new SimpleReloadInstance<>(
         param2,
         param3,
         param0,
         param1,
         (l_param1, l_param2, l_param3, l_param4, l_param5) -> l_param3.reload(
            l_param1, l_param2, InactiveProfiler.INSTANCE, InactiveProfiler.INSTANCE, param2, l_param5
         ),
         param4
      );
   }

   protected SimpleReloadInstance(
      Executor param0,
      final Executor param1,
      ResourceManager param2,
      List<PreparableReloadListener> param3,
      SimpleReloadInstance.StateFactory<S> param4,
      CompletableFuture<Unit> param5
   ) {
      this.listenerCount = param3.size();
      diag("constructor: " + param3.size() + " listeners, initialTaskDone=" + param5.isDone());
      this.startedTaskCounter.incrementAndGet();
      param5.thenRun(this.doneTaskCounter::incrementAndGet);
      List<CompletableFuture<S>> var7 = Lists.newArrayList();
      CompletableFuture<?> var8 = param5;
      this.preparingListeners = Sets.newHashSet(param3);

      for (final PreparableReloadListener var10 : param3) {
         final CompletableFuture<?> var11 = var8;
         CompletableFuture<S> var12 = param4.create(new PreparableReloadListener.PreparationBarrier() {
            @Override
            public <T> CompletableFuture<T> wait(T l_param0) {
               param1.execute(() -> {
                  SimpleReloadInstance.this.preparingListeners.remove(var10);
                  diag("prepare done: \"" + var10.getName() + "\" (" + SimpleReloadInstance.this.preparingListeners.size() + " left)");
                  if (SimpleReloadInstance.this.preparingListeners.isEmpty()) {
                     diag("allPreparations completed");
                     SimpleReloadInstance.this.allPreparations.complete(Unit.INSTANCE);
                  }
               });
               return SimpleReloadInstance.this.allPreparations.thenCombine((CompletionStage<? extends T>)var11, (l2_param1, l2_param2) -> l_param0);
            }
         }, param2, var10, l_param1 -> {
            this.startedTaskCounter.incrementAndGet();
            param0.execute(() -> {
               diag("prepare task run: \"" + var10.getName() + "\"");
               l_param1.run();
               this.doneTaskCounter.incrementAndGet();
            });
         }, l_param1 -> {
            this.startedReloads++;
            param1.execute(() -> {
               diag("apply task run: \"" + var10.getName() + "\"");
               l_param1.run();
               this.finishedReloads++;
               diag("apply task done: \"" + var10.getName() + "\"");
            });
         });
         var7.add(var12);
         var8 = var12;
      }

      this.allDone = Util.sequenceFailFast(var7);
      this.allDone.whenComplete((l1, l2) -> diag("allDone whenComplete: " + var7.size() + " stages"
         + (l1 == null ? " completed" : " result=" + l1) + (l2 == null ? "" : " err=" + l2)));
   }

   private static void diag(String msg) {
      System.out.println("[diag] SimpleReloadInstance: " + msg);
      try {
         net.lax1dude.eaglercraft.v1_8.log4j.LogManager.getLogger("Diag").info("{}", "SimpleReloadInstance: " + msg);
      } catch (Throwable var2) {
         System.out.println("[diag-eag-fail] SimpleReloadInstance: " + var2);
      }
   }

   @Override
   public CompletableFuture<?> done() {
      return this.allDone;
   }

   @Override
   public float getActualProgress() {
      int var1 = this.listenerCount - this.preparingListeners.size();
      float var2 = this.doneTaskCounter.get() * 2 + this.finishedReloads * 2 + var1 * 1;
      float var3 = this.startedTaskCounter.get() * 2 + this.startedReloads * 2 + this.listenerCount * 1;
      return var2 / var3;
   }

   public static ReloadInstance create(
      ResourceManager param0, List<PreparableReloadListener> param1, Executor param2, Executor param3, CompletableFuture<Unit> param4, boolean param5
   ) {
      return param5 ? new ProfiledReloadInstance(param0, param1, param2, param3, param4) : of(param0, param1, param2, param3, param4);
   }

   protected interface StateFactory<S> {
      CompletableFuture<S> create(
         PreparableReloadListener.PreparationBarrier var1, ResourceManager var2, PreparableReloadListener var3, Executor var4, Executor var5
      );
   }
}

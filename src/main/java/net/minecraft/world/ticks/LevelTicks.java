package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class LevelTicks<T> implements LevelTickAccess<T> {
   private static final Comparator<LevelChunkTicks<?>> CONTAINER_DRAIN_ORDER = (param0, param1) -> ScheduledTick.INTRA_TICK_DRAIN_ORDER
      .compare(param0.peek(), param1.peek());
   private final LongPredicate tickCheck;
   private final Supplier<ProfilerFiller> profiler;
   private final Long2ObjectMap<LevelChunkTicks<T>> allContainers = new Long2ObjectOpenHashMap<>();
   private final Long2LongMap nextTickForContainer = Util.make(new Long2LongOpenHashMap(), l_param0 -> l_param0.defaultReturnValue(Long.MAX_VALUE));
   private final Queue<LevelChunkTicks<T>> containersToTick = new PriorityQueue<>(CONTAINER_DRAIN_ORDER);
   private final Queue<ScheduledTick<T>> toRunThisTick = new ArrayDeque<>();
   private final List<ScheduledTick<T>> alreadyRunThisTick = new ArrayList<>();
   private final Set<ScheduledTick<?>> toRunThisTickSet = new ObjectOpenCustomHashSet<>(ScheduledTick.UNIQUE_TICK_HASH);
   private final BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>> chunkScheduleUpdater = (l_param0, l_param1) -> {
      if (l_param1.equals(l_param0.peek())) {
         this.updateContainerScheduling(l_param1);
      }
   };

   public LevelTicks(LongPredicate param0, Supplier<ProfilerFiller> param1) {
      this.tickCheck = param0;
      this.profiler = param1;
   }

   public void addContainer(ChunkPos param0, LevelChunkTicks<T> param1) {
      long var3 = param0.toLong();
      this.allContainers.put(var3, param1);
      ScheduledTick<T> var5 = param1.peek();
      if (var5 != null) {
         this.nextTickForContainer.put(var3, var5.triggerTick());
      }

      param1.setOnTickAdded(this.chunkScheduleUpdater);
   }

   public void removeContainer(ChunkPos param0) {
      long var2 = param0.toLong();
      LevelChunkTicks<T> var4 = this.allContainers.remove(var2);
      this.nextTickForContainer.remove(var2);
      if (var4 != null) {
         var4.setOnTickAdded(null);
      }
   }

   @Override
   public void schedule(ScheduledTick<T> param0) {
      long var2 = ChunkPos.asLong(param0.pos());
      LevelChunkTicks<T> var4 = this.allContainers.get(var2);
      if (var4 == null) {
         Util.pauseInIde(new IllegalStateException("Trying to schedule tick in not loaded position " + param0.pos()));
      } else {
         var4.schedule(param0);
      }
   }

   public void tick(long param0, int param2, BiConsumer<BlockPos, T> param3) {
      ProfilerFiller var5 = this.profiler.get();
      var5.push("collect");
      this.collectTicks(param0, param2, var5);
      var5.popPush("run");
      var5.incrementCounter("ticksToRun", this.toRunThisTick.size());
      this.runCollectedTicks(param3);
      var5.popPush("cleanup");
      this.cleanupAfterTick();
      var5.pop();
   }

   private void collectTicks(long param0, int param2, ProfilerFiller param3) {
      this.sortContainersToTick(param0);
      param3.incrementCounter("containersToTick", this.containersToTick.size());
      this.drainContainers(param0, param2);
      this.rescheduleLeftoverContainers();
   }

   private void sortContainersToTick(long param0) {
      ObjectIterator<Entry> var3 = Long2LongMaps.fastIterator(this.nextTickForContainer);

      while (var3.hasNext()) {
         Entry var4 = var3.next();
         long var5 = var4.getLongKey();
         long var7 = var4.getLongValue();
         if (var7 <= param0) {
            LevelChunkTicks<T> var9 = this.allContainers.get(var5);
            if (var9 == null) {
               var3.remove();
            } else {
               ScheduledTick<T> var10 = var9.peek();
               if (var10 == null) {
                  var3.remove();
               } else if (var10.triggerTick() > param0) {
                  var4.setValue(var10.triggerTick());
               } else if (this.tickCheck.test(var5)) {
                  var3.remove();
                  this.containersToTick.add(var9);
               }
            }
         }
      }
   }

   private void drainContainers(long param0, int param2) {
      LevelChunkTicks<T> var4;
      while (this.canScheduleMoreTicks(param2) && (var4 = this.containersToTick.poll()) != null) {
         ScheduledTick<T> var5 = var4.poll();
         this.scheduleForThisTick(var5);
         this.drainFromCurrentContainer(this.containersToTick, var4, param0, param2);
         ScheduledTick<T> var6 = var4.peek();
         if (var6 != null) {
            if (var6.triggerTick() <= param0 && this.canScheduleMoreTicks(param2)) {
               this.containersToTick.add(var4);
            } else {
               this.updateContainerScheduling(var6);
            }
         }
      }
   }

   private void rescheduleLeftoverContainers() {
      for (LevelChunkTicks<T> var2 : this.containersToTick) {
         this.updateContainerScheduling(var2.peek());
      }
   }

   private void updateContainerScheduling(ScheduledTick<T> param0) {
      this.nextTickForContainer.put(ChunkPos.asLong(param0.pos()), param0.triggerTick());
   }

   private void drainFromCurrentContainer(Queue<LevelChunkTicks<T>> param0, LevelChunkTicks<T> param1, long param2, int param4) {
      if (this.canScheduleMoreTicks(param4)) {
         LevelChunkTicks<T> var6 = param0.peek();
         ScheduledTick<T> var7 = var6 != null ? var6.peek() : null;

         while (this.canScheduleMoreTicks(param4)) {
            ScheduledTick<T> var8 = param1.peek();
            if (var8 == null || var8.triggerTick() > param2 || var7 != null && ScheduledTick.INTRA_TICK_DRAIN_ORDER.compare(var8, var7) > 0) {
               break;
            }

            param1.poll();
            this.scheduleForThisTick(var8);
         }
      }
   }

   private void scheduleForThisTick(ScheduledTick<T> param0) {
      this.toRunThisTick.add(param0);
   }

   private boolean canScheduleMoreTicks(int param0) {
      return this.toRunThisTick.size() < param0;
   }

   private void runCollectedTicks(BiConsumer<BlockPos, T> param0) {
      while (!this.toRunThisTick.isEmpty()) {
         ScheduledTick<T> var2 = this.toRunThisTick.poll();
         if (!this.toRunThisTickSet.isEmpty()) {
            this.toRunThisTickSet.remove(var2);
         }

         this.alreadyRunThisTick.add(var2);
         param0.accept(var2.pos(), var2.type());
      }
   }

   private void cleanupAfterTick() {
      this.toRunThisTick.clear();
      this.containersToTick.clear();
      this.alreadyRunThisTick.clear();
      this.toRunThisTickSet.clear();
   }

   @Override
   public boolean hasScheduledTick(BlockPos param0, T param1) {
      LevelChunkTicks<T> var3 = this.allContainers.get(ChunkPos.asLong(param0));
      return var3 != null && var3.hasScheduledTick(param0, param1);
   }

   @Override
   public boolean willTickThisTick(BlockPos param0, T param1) {
      this.calculateTickSetIfNeeded();
      return this.toRunThisTickSet.contains(ScheduledTick.probe(param1, param0));
   }

   private void calculateTickSetIfNeeded() {
      if (this.toRunThisTickSet.isEmpty() && !this.toRunThisTick.isEmpty()) {
         this.toRunThisTickSet.addAll(this.toRunThisTick);
      }
   }

   private void forContainersInArea(BoundingBox param0, LevelTicks.PosAndContainerConsumer<T> param1) {
      int var3 = SectionPos.posToSectionCoord(param0.minX());
      int var4 = SectionPos.posToSectionCoord(param0.minZ());
      int var5 = SectionPos.posToSectionCoord(param0.maxX());
      int var6 = SectionPos.posToSectionCoord(param0.maxZ());

      for (int var7 = var3; var7 <= var5; var7++) {
         for (int var8 = var4; var8 <= var6; var8++) {
            long var9 = ChunkPos.asLong(var7, var8);
            LevelChunkTicks<T> var11 = this.allContainers.get(var9);
            if (var11 != null) {
               param1.accept(var9, var11);
            }
         }
      }
   }

   public void clearArea(BoundingBox param0) {
      Predicate<ScheduledTick<T>> var2 = l_param1 -> param0.isInside(l_param1.pos());
      this.forContainersInArea(param0, (l_param1, l_param3) -> {
         ScheduledTick<T> l_var5 = l_param3.peek();
         l_param3.removeIf(var2);
         ScheduledTick<T> l_var6 = l_param3.peek();
         if (l_var6 != l_var5) {
            if (l_var6 != null) {
               this.updateContainerScheduling(l_var6);
            } else {
               this.nextTickForContainer.remove(l_param1);
            }
         }
      });
      this.alreadyRunThisTick.removeIf(var2);
      this.toRunThisTick.removeIf(var2);
   }

   public void copyArea(BoundingBox param0, Vec3i param1) {
      this.copyAreaFrom(this, param0, param1);
   }

   public void copyAreaFrom(LevelTicks<T> param0, BoundingBox param1, Vec3i param2) {
      List<ScheduledTick<T>> var4 = new ArrayList<>();
      Predicate<ScheduledTick<T>> var5 = l_param1 -> param1.isInside(l_param1.pos());
      param0.alreadyRunThisTick.stream().filter(var5).forEach(var4::add);
      param0.toRunThisTick.stream().filter(var5).forEach(var4::add);
      param0.forContainersInArea(param1, (l_param2, l_param4) -> l_param4.getAll().filter(var5).forEach(var4::add));
      LongSummaryStatistics var6 = var4.stream().mapToLong(ScheduledTick::subTickOrder).summaryStatistics();
      long var7 = var6.getMin();
      long var9 = var6.getMax();
      var4.forEach(
         l_param5 -> this.schedule(
            new ScheduledTick<>(
               l_param5.type(), l_param5.pos().offset(param2), l_param5.triggerTick(), l_param5.priority(), l_param5.subTickOrder() - var7 + var9 + 1L
            )
         )
      );
   }

   @Override
   public int count() {
      return this.allContainers.values().stream().mapToInt(TickAccess::count).sum();
   }

   @FunctionalInterface
   interface PosAndContainerConsumer<T> {
      void accept(long var1, LevelChunkTicks<T> var3);
   }
}

package net.minecraft.world.ticks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;

public class LevelChunkTicks<T> implements SerializableTickContainer<T>, TickContainerAccess<T> {
   private final Queue<ScheduledTick<T>> tickQueue = new PriorityQueue<>(ScheduledTick.DRAIN_ORDER);
   @Nullable
   private List<SavedTick<T>> pendingTicks;
   private final Set<ScheduledTick<?>> ticksPerPosition = new ObjectOpenCustomHashSet<>(ScheduledTick.UNIQUE_TICK_HASH);
   @Nullable
   private BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>> onTickAdded;

   public LevelChunkTicks() {
   }

   public LevelChunkTicks(List<SavedTick<T>> param0) {
      this.pendingTicks = param0;

      for (SavedTick<T> var3 : param0) {
         this.ticksPerPosition.add(ScheduledTick.probe(var3.type(), var3.pos()));
      }
   }

   public void setOnTickAdded(@Nullable BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>> param0) {
      this.onTickAdded = param0;
   }

   @Nullable
   public ScheduledTick<T> peek() {
      return this.tickQueue.peek();
   }

   @Nullable
   public ScheduledTick<T> poll() {
      ScheduledTick<T> var1 = this.tickQueue.poll();
      if (var1 != null) {
         this.ticksPerPosition.remove(var1);
      }

      return var1;
   }

   @Override
   public void schedule(ScheduledTick<T> param0) {
      if (this.ticksPerPosition.add(param0)) {
         this.scheduleUnchecked(param0);
      }
   }

   private void scheduleUnchecked(ScheduledTick<T> param0) {
      this.tickQueue.add(param0);
      if (this.onTickAdded != null) {
         this.onTickAdded.accept(this, param0);
      }
   }

   @Override
   public boolean hasScheduledTick(BlockPos param0, T param1) {
      return this.ticksPerPosition.contains(ScheduledTick.probe(param1, param0));
   }

   public void removeIf(Predicate<ScheduledTick<T>> param0) {
      Iterator<ScheduledTick<T>> var2 = this.tickQueue.iterator();

      while (var2.hasNext()) {
         ScheduledTick<T> var3 = var2.next();
         if (param0.test(var3)) {
            var2.remove();
            this.ticksPerPosition.remove(var3);
         }
      }
   }

   public Stream<ScheduledTick<T>> getAll() {
      return this.tickQueue.stream();
   }

   @Override
   public int count() {
      return this.tickQueue.size() + (this.pendingTicks != null ? this.pendingTicks.size() : 0);
   }

   public ListTag save(long param0, Function<T, String> param2) {
      ListTag var4 = new ListTag();
      if (this.pendingTicks != null) {
         for (SavedTick<T> var6 : this.pendingTicks) {
            var4.add(var6.save(param2));
         }
      }

      for (ScheduledTick<T> var6 : this.tickQueue) {
         var4.add(SavedTick.saveTick(var6, param2, param0));
      }

      return var4;
   }

   public void unpack(long param0) {
      if (this.pendingTicks != null) {
         int var3 = -this.pendingTicks.size();

         for (SavedTick<T> var5 : this.pendingTicks) {
            this.scheduleUnchecked(var5.unpack(param0, var3++));
         }
      }

      this.pendingTicks = null;
   }

   public static <T> LevelChunkTicks<T> load(ListTag param0, Function<String, Optional<T>> param1, ChunkPos param2) {
      Builder<SavedTick<T>> var3 = ImmutableList.builder();
      SavedTick.loadTickList(param0, param1, param2, var3::add);
      return new LevelChunkTicks<>(var3.build());
   }
}

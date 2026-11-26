package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;

public abstract class DistanceManager {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int ENTITY_TICKING_RANGE = 2;
   static final int PLAYER_TICKET_LEVEL = 33 + ChunkStatus.getDistance(ChunkStatus.FULL) - 2;
   private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
   private static final int ENTITY_TICKING_LEVEL_THRESHOLD = 32;
   private static final int BLOCK_TICKING_LEVEL_THRESHOLD = 33;
   final Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk = new Long2ObjectOpenHashMap<>();
   final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets = new Long2ObjectOpenHashMap<>();
   private final DistanceManager.ChunkTicketTracker ticketTracker = new DistanceManager.ChunkTicketTracker();
   private final DistanceManager.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter = new DistanceManager.FixedPlayerDistanceChunkTracker(8);
   private final TickingTracker tickingTicketsTracker = new TickingTracker();
   private final DistanceManager.PlayerTicketTracker playerTicketManager = new DistanceManager.PlayerTicketTracker(33);
   final Set<ChunkHolder> chunksToUpdateFutures = Sets.newHashSet();
   final ChunkTaskPriorityQueueSorter ticketThrottler;
   final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> ticketThrottlerInput;
   final ProcessorHandle<ChunkTaskPriorityQueueSorter.Release> ticketThrottlerReleaser;
   final LongSet ticketsToRelease = new LongOpenHashSet();
   final Executor mainThreadExecutor;
   private long ticketTickCounter;
   private int simulationDistance = 10;

   protected DistanceManager(Executor param0, Executor param1) {
      ProcessorHandle<Runnable> var3 = ProcessorHandle.of("player ticket throttler", param1::execute);
      ChunkTaskPriorityQueueSorter var4 = new ChunkTaskPriorityQueueSorter(ImmutableList.of(var3), param0, 4);
      this.ticketThrottler = var4;
      this.ticketThrottlerInput = var4.getProcessor(var3, true);
      this.ticketThrottlerReleaser = var4.getReleaseProcessor(var3);
      this.mainThreadExecutor = param1;
   }

   protected void purgeStaleTickets() {
      this.ticketTickCounter++;
      ObjectIterator<Entry<SortedArraySet<Ticket<?>>>> var1 = this.tickets.long2ObjectEntrySet().fastIterator();

      while (var1.hasNext()) {
         Entry<SortedArraySet<Ticket<?>>> var2 = var1.next();
         Iterator<Ticket<?>> var3 = var2.getValue().iterator();
         boolean var4 = false;

         while (var3.hasNext()) {
            Ticket<?> var5 = var3.next();
            if (var5.timedOut(this.ticketTickCounter)) {
               var3.remove();
               var4 = true;
               this.tickingTicketsTracker.removeTicket(var2.getLongKey(), var5);
            }
         }

         if (var4) {
            this.ticketTracker.update(var2.getLongKey(), getTicketLevelAt(var2.getValue()), false);
         }

         if (var2.getValue().isEmpty()) {
            var1.remove();
         }
      }
   }

   private static int getTicketLevelAt(SortedArraySet<Ticket<?>> param0) {
      return !param0.isEmpty() ? param0.first().getTicketLevel() : ChunkMap.MAX_CHUNK_DISTANCE + 1;
   }

   protected abstract boolean isChunkToRemove(long var1);

   @Nullable
   protected abstract ChunkHolder getChunk(long var1);

   @Nullable
   protected abstract ChunkHolder updateChunkScheduling(long var1, int var3, @Nullable ChunkHolder var4, int var5);

   public boolean runAllUpdates(ChunkMap param0) {
      this.naturalSpawnChunkCounter.runAllUpdates();
      this.tickingTicketsTracker.runAllUpdates();
      this.playerTicketManager.runAllUpdates();
      int var2 = Integer.MAX_VALUE - this.ticketTracker.runDistanceUpdates(Integer.MAX_VALUE);
      boolean var3 = var2 != 0;
      if (var3) {
      }

      if (!this.chunksToUpdateFutures.isEmpty()) {
         this.chunksToUpdateFutures.forEach(l_param1 -> l_param1.updateFutures(param0, this.mainThreadExecutor));
         this.chunksToUpdateFutures.clear();
         return true;
      }

      if (!this.ticketsToRelease.isEmpty()) {
         LongIterator var4 = this.ticketsToRelease.iterator();

         while (var4.hasNext()) {
            long var5 = var4.nextLong();
            if (this.getTickets(var5).stream().anyMatch(l_param0 -> l_param0.getType() == TicketType.PLAYER)) {
               ChunkHolder var7 = param0.getUpdatingChunkIfPresent(var5);
               if (var7 == null) {
                  throw new IllegalStateException();
               }

               CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> var8 = var7.getEntityTickingChunkFuture();
               var8.thenAccept(
                  l_param2 -> this.mainThreadExecutor
                     .execute(() -> this.ticketThrottlerReleaser.tell(ChunkTaskPriorityQueueSorter.release(() -> {}, var5, false)))
               );
            }
         }

         this.ticketsToRelease.clear();
      }

      return var3;
   }

   void addTicket(long param0, Ticket<?> param2) {
      SortedArraySet<Ticket<?>> var4 = this.getTickets(param0);
      int var5 = getTicketLevelAt(var4);
      Ticket<?> var6 = var4.addOrGet(param2);
      var6.setCreatedTick(this.ticketTickCounter);
      if (param2.getTicketLevel() < var5) {
         this.ticketTracker.update(param0, param2.getTicketLevel(), true);
      }
   }

   void removeTicket(long param0, Ticket<?> param2) {
      SortedArraySet<Ticket<?>> var4 = this.getTickets(param0);
      if (var4.remove(param2)) {
      }

      if (var4.isEmpty()) {
         this.tickets.remove(param0);
      }

      this.ticketTracker.update(param0, getTicketLevelAt(var4), false);
   }

   public <T> void addTicket(TicketType<T> param0, ChunkPos param1, int param2, T param3) {
      this.addTicket(param1.toLong(), new Ticket<>(param0, param2, param3));
   }

   public <T> void removeTicket(TicketType<T> param0, ChunkPos param1, int param2, T param3) {
      Ticket<T> var5 = new Ticket<>(param0, param2, param3);
      this.removeTicket(param1.toLong(), var5);
   }

   public <T> void addRegionTicket(TicketType<T> param0, ChunkPos param1, int param2, T param3) {
      Ticket<T> var5 = new Ticket<>(param0, 33 - param2, param3);
      long var6 = param1.toLong();
      this.addTicket(var6, var5);
      this.tickingTicketsTracker.addTicket(var6, var5);
   }

   public <T> void removeRegionTicket(TicketType<T> param0, ChunkPos param1, int param2, T param3) {
      Ticket<T> var5 = new Ticket<>(param0, 33 - param2, param3);
      long var6 = param1.toLong();
      this.removeTicket(var6, var5);
      this.tickingTicketsTracker.removeTicket(var6, var5);
   }

   private SortedArraySet<Ticket<?>> getTickets(long param0) {
      return this.tickets.computeIfAbsent(param0, l_param0 -> SortedArraySet.create(4));
   }

   protected void updateChunkForced(ChunkPos param0, boolean param1) {
      Ticket<ChunkPos> var3 = new Ticket<>(TicketType.FORCED, 31, param0);
      long var4 = param0.toLong();
      if (param1) {
         this.addTicket(var4, var3);
         this.tickingTicketsTracker.addTicket(var4, var3);
      } else {
         this.removeTicket(var4, var3);
         this.tickingTicketsTracker.removeTicket(var4, var3);
      }
   }

   public void addPlayer(SectionPos param0, ServerPlayer param1) {
      ChunkPos var3 = param0.chunk();
      long var4 = var3.toLong();
      this.playersPerChunk.computeIfAbsent(var4, l_param0 -> new ObjectOpenHashSet<>()).add(param1);
      this.naturalSpawnChunkCounter.update(var4, 0, true);
      this.playerTicketManager.update(var4, 0, true);
      this.tickingTicketsTracker.addTicket(TicketType.PLAYER, var3, this.getPlayerTicketLevel(), var3);
   }

   public void removePlayer(SectionPos param0, ServerPlayer param1) {
      ChunkPos var3 = param0.chunk();
      long var4 = var3.toLong();
      ObjectSet<ServerPlayer> var6 = this.playersPerChunk.get(var4);
      var6.remove(param1);
      if (var6.isEmpty()) {
         this.playersPerChunk.remove(var4);
         this.naturalSpawnChunkCounter.update(var4, Integer.MAX_VALUE, false);
         this.playerTicketManager.update(var4, Integer.MAX_VALUE, false);
         this.tickingTicketsTracker.removeTicket(TicketType.PLAYER, var3, this.getPlayerTicketLevel(), var3);
      }
   }

   private int getPlayerTicketLevel() {
      return Math.max(0, 31 - this.simulationDistance);
   }

   public boolean inEntityTickingRange(long param0) {
      return this.tickingTicketsTracker.getLevel(param0) < 32;
   }

   public boolean inBlockTickingRange(long param0) {
      return this.tickingTicketsTracker.getLevel(param0) < 33;
   }

   protected String getTicketDebugString(long param0) {
      SortedArraySet<Ticket<?>> var3 = this.tickets.get(param0);
      return var3 != null && !var3.isEmpty() ? var3.first().toString() : "no_ticket";
   }

   protected void updatePlayerTickets(int param0) {
      this.playerTicketManager.updateViewDistance(param0);
   }

   public void updateSimulationDistance(int param0) {
      if (param0 != this.simulationDistance) {
         this.simulationDistance = param0;
         this.tickingTicketsTracker.replacePlayerTicketsLevel(this.getPlayerTicketLevel());
      }
   }

   public int getNaturalSpawnChunkCount() {
      this.naturalSpawnChunkCounter.runAllUpdates();
      return this.naturalSpawnChunkCounter.chunks.size();
   }

   public boolean hasPlayersNearby(long param0) {
      this.naturalSpawnChunkCounter.runAllUpdates();
      return this.naturalSpawnChunkCounter.chunks.containsKey(param0);
   }

   public String getDebugStatus() {
      return this.ticketThrottler.getDebugStatus();
   }

   private void dumpTickets(String param0) {
      try (FileOutputStream var2 = new FileOutputStream(new File(param0))) {
         for (Entry<SortedArraySet<Ticket<?>>> var4 : this.tickets.long2ObjectEntrySet()) {
            ChunkPos var5 = new ChunkPos(var4.getLongKey());

            for (Ticket<?> var7 : var4.getValue()) {
               var2.write((var5.x + "\t" + var5.z + "\t" + var7.getType() + "\t" + var7.getTicketLevel() + "\t\n").getBytes(StandardCharsets.UTF_8));
            }
         }
      } catch (IOException var2) {
         LOGGER.error("Failed to dump tickets to {}", param0, var2);
      }
   }

   @VisibleForTesting
   TickingTracker tickingTracker() {
      return this.tickingTicketsTracker;
   }

   public void removeTicketsOnClosing() {
      ImmutableSet<TicketType<?>> var1 = ImmutableSet.of(TicketType.UNKNOWN, TicketType.POST_TELEPORT, TicketType.LIGHT);
      ObjectIterator<Entry<SortedArraySet<Ticket<?>>>> var2 = this.tickets.long2ObjectEntrySet().fastIterator();

      while (var2.hasNext()) {
         Entry<SortedArraySet<Ticket<?>>> var3 = var2.next();
         Iterator<Ticket<?>> var4 = var3.getValue().iterator();
         boolean var5 = false;

         while (var4.hasNext()) {
            Ticket<?> var6 = var4.next();
            if (!var1.contains(var6.getType())) {
               var4.remove();
               var5 = true;
               this.tickingTicketsTracker.removeTicket(var3.getLongKey(), var6);
            }
         }

         if (var5) {
            this.ticketTracker.update(var3.getLongKey(), getTicketLevelAt(var3.getValue()), false);
         }

         if (var3.getValue().isEmpty()) {
            var2.remove();
         }
      }
   }

   public boolean hasTickets() {
      return !this.tickets.isEmpty();
   }

   class ChunkTicketTracker extends ChunkTracker {
      public ChunkTicketTracker() {
         super(ChunkMap.MAX_CHUNK_DISTANCE + 2, 16, 256);
      }

      @Override
      protected int getLevelFromSource(long param0) {
         SortedArraySet<Ticket<?>> var3 = DistanceManager.this.tickets.get(param0);
         if (var3 == null) {
            return Integer.MAX_VALUE;
         } else {
            return var3.isEmpty() ? Integer.MAX_VALUE : var3.first().getTicketLevel();
         }
      }

      @Override
      protected int getLevel(long param0) {
         if (!DistanceManager.this.isChunkToRemove(param0)) {
            ChunkHolder var3 = DistanceManager.this.getChunk(param0);
            if (var3 != null) {
               return var3.getTicketLevel();
            }
         }

         return ChunkMap.MAX_CHUNK_DISTANCE + 1;
      }

      @Override
      protected void setLevel(long param0, int param2) {
         ChunkHolder var4 = DistanceManager.this.getChunk(param0);
         int var5 = var4 == null ? ChunkMap.MAX_CHUNK_DISTANCE + 1 : var4.getTicketLevel();
         if (var5 != param2) {
            var4 = DistanceManager.this.updateChunkScheduling(param0, param2, var4, var5);
            if (var4 != null) {
               DistanceManager.this.chunksToUpdateFutures.add(var4);
            }
         }
      }

      public int runDistanceUpdates(int param0) {
         return this.runUpdates(param0);
      }
   }

   class FixedPlayerDistanceChunkTracker extends ChunkTracker {
      protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
      protected final int maxDistance;

      protected FixedPlayerDistanceChunkTracker(int param1) {
         super(param1 + 2, 16, 256);
         this.maxDistance = param1;
         this.chunks.defaultReturnValue((byte)(param1 + 2));
      }

      @Override
      protected int getLevel(long param0) {
         return this.chunks.get(param0);
      }

      @Override
      protected void setLevel(long param0, int param2) {
         byte var4;
         if (param2 > this.maxDistance) {
            var4 = this.chunks.remove(param0);
         } else {
            var4 = this.chunks.put(param0, (byte)param2);
         }

         this.onLevelChange(param0, var4, param2);
      }

      protected void onLevelChange(long param0, int param2, int param3) {
      }

      @Override
      protected int getLevelFromSource(long param0) {
         return this.havePlayer(param0) ? 0 : Integer.MAX_VALUE;
      }

      private boolean havePlayer(long param0) {
         ObjectSet<ServerPlayer> var3 = DistanceManager.this.playersPerChunk.get(param0);
         return var3 != null && !var3.isEmpty();
      }

      public void runAllUpdates() {
         this.runUpdates(Integer.MAX_VALUE);
      }

      private void dumpChunks(String param0) {
         try (FileOutputStream var2 = new FileOutputStream(new File(param0))) {
            for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry var4 : this.chunks.long2ByteEntrySet()) {
               ChunkPos var5 = new ChunkPos(var4.getLongKey());
               String var6 = Byte.toString(var4.getByteValue());
               var2.write((var5.x + "\t" + var5.z + "\t" + var6 + "\n").getBytes(StandardCharsets.UTF_8));
            }
         } catch (IOException var2) {
            DistanceManager.LOGGER.error("Failed to dump chunks to {}", param0, var2);
         }
      }
   }

   class PlayerTicketTracker extends DistanceManager.FixedPlayerDistanceChunkTracker {
      private int viewDistance;
      private final Long2IntMap queueLevels = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
      private final LongSet toUpdate = new LongOpenHashSet();

      protected PlayerTicketTracker(int param1) {
         super(param1);
         this.viewDistance = 0;
         this.queueLevels.defaultReturnValue(param1 + 2);
      }

      @Override
      protected void onLevelChange(long param0, int param2, int param3) {
         this.toUpdate.add(param0);
      }

      public void updateViewDistance(int param0) {
         for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry var3 : this.chunks.long2ByteEntrySet()) {
            byte var4 = var3.getByteValue();
            long var5 = var3.getLongKey();
            this.onLevelChange(var5, var4, this.haveTicketFor(var4), var4 <= param0 - 2);
         }

         this.viewDistance = param0;
      }

      private void onLevelChange(long param0, int param2, boolean param3, boolean param4) {
         if (param3 != param4) {
            Ticket<?> var6 = new Ticket<>(TicketType.PLAYER, DistanceManager.PLAYER_TICKET_LEVEL, new ChunkPos(param0));
            if (param4) {
               DistanceManager.this.ticketThrottlerInput
                  .tell(ChunkTaskPriorityQueueSorter.message(() -> DistanceManager.this.mainThreadExecutor.execute(() -> {
                     if (this.haveTicketFor(this.getLevel(param0))) {
                        DistanceManager.this.addTicket(param0, var6);
                        DistanceManager.this.ticketsToRelease.add(param0);
                     } else {
                        DistanceManager.this.ticketThrottlerReleaser.tell(ChunkTaskPriorityQueueSorter.release(() -> {}, param0, false));
                     }
                  }), param0, () -> param2));
            } else {
               DistanceManager.this.ticketThrottlerReleaser
                  .tell(
                     ChunkTaskPriorityQueueSorter.release(
                        () -> DistanceManager.this.mainThreadExecutor.execute(() -> DistanceManager.this.removeTicket(param0, var6)), param0, true
                     )
                  );
            }
         }
      }

      @Override
      public void runAllUpdates() {
         super.runAllUpdates();
         if (!this.toUpdate.isEmpty()) {
            LongIterator var1 = this.toUpdate.iterator();

            while (var1.hasNext()) {
               long var2 = var1.nextLong();
               int var4 = this.queueLevels.get(var2);
               int var5 = this.getLevel(var2);
               if (var4 != var5) {
                  DistanceManager.this.ticketThrottler.onLevelChange(new ChunkPos(var2), () -> this.queueLevels.get(var2), var5, l_param2 -> {
                     if (l_param2 >= this.queueLevels.defaultReturnValue()) {
                        this.queueLevels.remove(var2);
                     } else {
                        this.queueLevels.put(var2, l_param2);
                     }
                  });
                  this.onLevelChange(var2, var5, this.haveTicketFor(var4), this.haveTicketFor(var5));
               }
            }

            this.toUpdate.clear();
         }
      }

      private boolean haveTicketFor(int param0) {
         return param0 <= this.viewDistance - 2;
      }
   }
}

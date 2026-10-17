package net.minecraft.server.level;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.util.DebugBuffer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class ChunkHolder {
   public static final Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> UNLOADED_CHUNK = Either.right(ChunkHolder.ChunkLoadingFailure.UNLOADED);
   public static final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> UNLOADED_CHUNK_FUTURE = CompletableFuture.completedFuture(
      UNLOADED_CHUNK
   );
   public static final Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> UNLOADED_LEVEL_CHUNK = Either.right(ChunkHolder.ChunkLoadingFailure.UNLOADED);
   private static final Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> NOT_DONE_YET = Either.right(ChunkHolder.ChunkLoadingFailure.UNLOADED);
   private static final CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> UNLOADED_LEVEL_CHUNK_FUTURE = CompletableFuture.completedFuture(
      UNLOADED_LEVEL_CHUNK
   );
   private static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.getStatusList();
   private static final ChunkHolder.FullChunkStatus[] FULL_CHUNK_STATUSES = ChunkHolder.FullChunkStatus.values();
   private static final int BLOCKS_BEFORE_RESEND_FUDGE = 64;
   private final AtomicReferenceArray<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> futures = new AtomicReferenceArray<>(
      CHUNK_STATUSES.size()
   );
   private final LevelHeightAccessor levelHeightAccessor;
   private volatile CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
   private volatile CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
   private volatile CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
   private CompletableFuture<ChunkAccess> chunkToSave = CompletableFuture.completedFuture(null);
   @Nullable
   private final DebugBuffer<ChunkHolder.ChunkSaveDebug> chunkToSaveHistory = null;
   private int oldTicketLevel;
   private int ticketLevel;
   private int queueLevel;
   final ChunkPos pos;
   private boolean hasChangedSections;
   private final ShortSet[] changedBlocksPerSection;
   private final BitSet blockChangedLightSectionFilter = new BitSet();
   private final BitSet skyChangedLightSectionFilter = new BitSet();
   private final LevelLightEngine lightEngine;
   private final ChunkHolder.LevelChangeListener onLevelChange;
   private final ChunkHolder.PlayerProvider playerProvider;
   private boolean wasAccessibleSinceLastSave;
   private boolean resendLight;
   private CompletableFuture<Void> pendingFullStateConfirmation = CompletableFuture.completedFuture(null);

   public ChunkHolder(
      ChunkPos param0,
      int param1,
      LevelHeightAccessor param2,
      LevelLightEngine param3,
      ChunkHolder.LevelChangeListener param4,
      ChunkHolder.PlayerProvider param5
   ) {
      this.pos = param0;
      this.levelHeightAccessor = param2;
      this.lightEngine = param3;
      this.onLevelChange = param4;
      this.playerProvider = param5;
      this.oldTicketLevel = ChunkMap.MAX_CHUNK_DISTANCE + 1;
      this.ticketLevel = this.oldTicketLevel;
      this.queueLevel = this.oldTicketLevel;
      this.setTicketLevel(param1);
      this.changedBlocksPerSection = new ShortSet[param2.getSectionsCount()];
   }

   public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getFutureIfPresentUnchecked(ChunkStatus param0) {
      CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var2 = this.futures.get(param0.getIndex());
      return var2 == null ? UNLOADED_CHUNK_FUTURE : var2;
   }

   public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getFutureIfPresent(ChunkStatus param0) {
      return getStatus(this.ticketLevel).isOrAfter(param0) ? this.getFutureIfPresentUnchecked(param0) : UNLOADED_CHUNK_FUTURE;
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> getTickingChunkFuture() {
      return this.tickingChunkFuture;
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> getEntityTickingChunkFuture() {
      return this.entityTickingChunkFuture;
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> getFullChunkFuture() {
      return this.fullChunkFuture;
   }

   @Nullable
   public LevelChunk getTickingChunk() {
      CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> var1 = this.getTickingChunkFuture();
      Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> var2 = var1.getNow(null);
      return var2 == null ? null : var2.left().orElse(null);
   }

   @Nullable
   public LevelChunk getFullChunk() {
      CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> var1 = this.getFullChunkFuture();
      Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> var2 = var1.getNow(null);
      return var2 == null ? null : var2.left().orElse(null);
   }

   @Nullable
   public ChunkStatus getLastAvailableStatus() {
      for (int var1 = CHUNK_STATUSES.size() - 1; var1 >= 0; var1--) {
         ChunkStatus var2 = CHUNK_STATUSES.get(var1);
         CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var3 = this.getFutureIfPresentUnchecked(var2);
         if (var3.getNow(UNLOADED_CHUNK).left().isPresent()) {
            return var2;
         }
      }

      return null;
   }

   @Nullable
   public ChunkAccess getLastAvailable() {
      for (int var1 = CHUNK_STATUSES.size() - 1; var1 >= 0; var1--) {
         ChunkStatus var2 = CHUNK_STATUSES.get(var1);
         CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var3 = this.getFutureIfPresentUnchecked(var2);
         if (!var3.isCompletedExceptionally()) {
            Optional<ChunkAccess> var4 = var3.getNow(UNLOADED_CHUNK).left();
            if (var4.isPresent()) {
               return var4.get();
            }
         }
      }

      return null;
   }

   public CompletableFuture<ChunkAccess> getChunkToSave() {
      return this.chunkToSave;
   }

   public void blockChanged(BlockPos param0) {
      LevelChunk var2 = this.getTickingChunk();
      if (var2 != null) {
         int var3 = this.levelHeightAccessor.getSectionIndex(param0.getY());
         if (this.changedBlocksPerSection[var3] == null) {
            this.hasChangedSections = true;
            this.changedBlocksPerSection[var3] = new ShortOpenHashSet();
         }

         this.changedBlocksPerSection[var3].add(SectionPos.sectionRelativePos(param0));
      }
   }

   public void sectionLightChanged(LightLayer param0, int param1) {
      Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> var3 = this.getFutureIfPresent(ChunkStatus.FEATURES).getNow(null);
      if (var3 != null) {
         ChunkAccess var4 = var3.left().orElse(null);
         if (var4 != null) {
            var4.setUnsaved(true);
            LevelChunk var5 = this.getTickingChunk();
            if (var5 != null) {
               int var6 = this.lightEngine.getMinLightSection();
               int var7 = this.lightEngine.getMaxLightSection();
               if (param1 >= var6 && param1 <= var7) {
                  int var8 = param1 - var6;
                  if (param0 == LightLayer.SKY) {
                     this.skyChangedLightSectionFilter.set(var8);
                  } else {
                     this.blockChangedLightSectionFilter.set(var8);
                  }
               }
            }
         }
      }
   }

   public void broadcastChanges(LevelChunk param0) {
      if (this.hasChangedSections || !this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
         Level var2 = param0.getLevel();
         int var3 = 0;

         for (int var4 = 0; var4 < this.changedBlocksPerSection.length; var4++) {
            var3 += this.changedBlocksPerSection[var4] != null ? this.changedBlocksPerSection[var4].size() : 0;
         }

         this.resendLight |= var3 >= 64;
         if (!this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
            this.broadcast(
               new ClientboundLightUpdatePacket(param0.getPos(), this.lightEngine, this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter, true),
               !this.resendLight
            );
            this.skyChangedLightSectionFilter.clear();
            this.blockChangedLightSectionFilter.clear();
         }

         for (int var4 = 0; var4 < this.changedBlocksPerSection.length; var4++) {
            ShortSet var5 = this.changedBlocksPerSection[var4];
            if (var5 != null) {
               int var6 = this.levelHeightAccessor.getSectionYFromSectionIndex(var4);
               SectionPos var7 = SectionPos.of(param0.getPos(), var6);
               if (var5.size() == 1) {
                  BlockPos var8 = var7.relativeToBlockPos(var5.iterator().nextShort());
                  BlockState var9 = var2.getBlockState(var8);
                  this.broadcast(new ClientboundBlockUpdatePacket(var8, var9), false);
                  this.broadcastBlockEntityIfNeeded(var2, var8, var9);
               } else {
                  LevelChunkSection var8 = param0.getSection(var4);
                  ClientboundSectionBlocksUpdatePacket var9 = new ClientboundSectionBlocksUpdatePacket(var7, var5, var8, this.resendLight);
                  this.broadcast(var9, false);
                  var9.runUpdates((l_param1, l_param2) -> this.broadcastBlockEntityIfNeeded(var2, l_param1, l_param2));
               }

               this.changedBlocksPerSection[var4] = null;
            }
         }

         this.hasChangedSections = false;
      }
   }

   private void broadcastBlockEntityIfNeeded(Level param0, BlockPos param1, BlockState param2) {
      if (param2.hasBlockEntity()) {
         this.broadcastBlockEntity(param0, param1);
      }
   }

   private void broadcastBlockEntity(Level param0, BlockPos param1) {
      BlockEntity var3 = param0.getBlockEntity(param1);
      if (var3 != null) {
         Packet<?> var4 = var3.getUpdatePacket();
         if (var4 != null) {
            this.broadcast(var4, false);
         }
      }
   }

   private void broadcast(Packet<?> param0, boolean param1) {
      this.playerProvider.getPlayers(this.pos, param1).forEach(l_param1 -> l_param1.connection.send(param0));
   }

   public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getOrScheduleFuture(ChunkStatus param0, ChunkMap param1) {
      int var3 = param0.getIndex();
      CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var4 = this.futures.get(var3);
      if (var4 != null) {
         Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> var5 = var4.getNow(NOT_DONE_YET);
         if (var5 == null) {
            String var6 = "value in future for status: " + param0 + " was incorrectly set to null at chunk: " + this.pos;
            throw param1.debugFuturesAndCreateReportedException(new IllegalStateException("null value previously set for chunk status"), var6);
         }

         if (var5 == NOT_DONE_YET || var5.right().isEmpty()) {
            return var4;
         }
      }

      if (getStatus(this.ticketLevel).isOrAfter(param0)) {
         CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var5 = param1.schedule(this, param0);
         this.updateChunkToSave(var5, "schedule " + param0);
         this.futures.set(var3, var5);
         return var5;
      } else {
         return var4 == null ? UNLOADED_CHUNK_FUTURE : var4;
      }
   }

   protected void addSaveDependency(String param0, CompletableFuture<?> param1) {
      if (this.chunkToSaveHistory != null) {
         this.chunkToSaveHistory.push(new ChunkHolder.ChunkSaveDebug(Thread.currentThread(), param1, param0));
      }

      this.chunkToSave = this.chunkToSave.thenCombine((CompletionStage<? extends Object>)param1, (l_param0, l_param1) -> (ChunkAccess)l_param0);
   }

   private void updateChunkToSave(CompletableFuture<? extends Either<? extends ChunkAccess, ChunkHolder.ChunkLoadingFailure>> param0, String param1) {
      if (this.chunkToSaveHistory != null) {
         this.chunkToSaveHistory.push(new ChunkHolder.ChunkSaveDebug(Thread.currentThread(), param0, param1));
      }

      this.chunkToSave = this.chunkToSave
         .thenCombine(param0, (l_param0, l_param1) -> l_param1.map(l2_param0 -> (ChunkAccess)l2_param0, l2_param1 -> (ChunkAccess)l_param0));
   }

   public ChunkHolder.FullChunkStatus getFullStatus() {
      return getFullChunkStatus(this.ticketLevel);
   }

   public ChunkPos getPos() {
      return this.pos;
   }

   public int getTicketLevel() {
      return this.ticketLevel;
   }

   public int getQueueLevel() {
      return this.queueLevel;
   }

   private void setQueueLevel(int param0) {
      this.queueLevel = param0;
   }

   public void setTicketLevel(int param0) {
      this.ticketLevel = param0;
   }

   private void scheduleFullChunkPromotion(
      ChunkMap param0, CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> param1, Executor param2, ChunkHolder.FullChunkStatus param3
   ) {
      this.pendingFullStateConfirmation.cancel(false);
      CompletableFuture<Void> var5 = new CompletableFuture<>();
      var5.thenRunAsync(() -> param0.onFullChunkStatusChange(this.pos, param3), param2);
      this.pendingFullStateConfirmation = var5;
      param1.thenAccept(l_param1 -> l_param1.ifLeft(l2_param1 -> var5.complete(null)));
   }

   private void demoteFullChunk(ChunkMap param0, ChunkHolder.FullChunkStatus param1) {
      this.pendingFullStateConfirmation.cancel(false);
      param0.onFullChunkStatusChange(this.pos, param1);
   }

   protected void updateFutures(ChunkMap param0, Executor param1) {
      ChunkStatus var3 = getStatus(this.oldTicketLevel);
      ChunkStatus var4 = getStatus(this.ticketLevel);
      boolean var5 = this.oldTicketLevel <= ChunkMap.MAX_CHUNK_DISTANCE;
      boolean var6 = this.ticketLevel <= ChunkMap.MAX_CHUNK_DISTANCE;
      ChunkHolder.FullChunkStatus var7 = getFullChunkStatus(this.oldTicketLevel);
      ChunkHolder.FullChunkStatus var8 = getFullChunkStatus(this.ticketLevel);
      if (var5) {
         Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> var9 = Either.right(new ChunkHolder.ChunkLoadingFailure() {
            @Override
            public String toString() {
               return "Unloaded ticket level " + ChunkHolder.this.pos;
            }
         });

         for (int var10 = var6 ? var4.getIndex() + 1 : 0; var10 <= var3.getIndex(); var10++) {
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var11 = this.futures.get(var10);
            if (var11 == null) {
               this.futures.set(var10, CompletableFuture.completedFuture(var9));
            }
         }
      }

      boolean var9 = var7.isOrAfter(ChunkHolder.FullChunkStatus.BORDER);
      boolean var10 = var8.isOrAfter(ChunkHolder.FullChunkStatus.BORDER);
      this.wasAccessibleSinceLastSave |= var10;
      if (!var9 && var10) {
         this.fullChunkFuture = param0.prepareAccessibleChunk(this);
         this.scheduleFullChunkPromotion(param0, this.fullChunkFuture, param1, ChunkHolder.FullChunkStatus.BORDER);
         this.updateChunkToSave(this.fullChunkFuture, "full");
      }

      if (var9 && !var10) {
         this.fullChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
         this.fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
      }

      boolean var11 = var7.isOrAfter(ChunkHolder.FullChunkStatus.TICKING);
      boolean var12 = var8.isOrAfter(ChunkHolder.FullChunkStatus.TICKING);
      if (!var11 && var12) {
         this.tickingChunkFuture = param0.prepareTickingChunk(this);
         this.scheduleFullChunkPromotion(param0, this.tickingChunkFuture, param1, ChunkHolder.FullChunkStatus.TICKING);
         this.updateChunkToSave(this.tickingChunkFuture, "ticking");
      }

      if (var11 && !var12) {
         this.tickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
         this.tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
      }

      boolean var13 = var7.isOrAfter(ChunkHolder.FullChunkStatus.ENTITY_TICKING);
      boolean var14 = var8.isOrAfter(ChunkHolder.FullChunkStatus.ENTITY_TICKING);
      if (!var13 && var14) {
         if (this.entityTickingChunkFuture != UNLOADED_LEVEL_CHUNK_FUTURE) {
            throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
         }

         this.entityTickingChunkFuture = param0.prepareEntityTickingChunk(this.pos);
         this.scheduleFullChunkPromotion(param0, this.entityTickingChunkFuture, param1, ChunkHolder.FullChunkStatus.ENTITY_TICKING);
         this.updateChunkToSave(this.entityTickingChunkFuture, "entity ticking");
      }

      if (var13 && !var14) {
         this.entityTickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
         this.entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
      }

      if (!var8.isOrAfter(var7)) {
         this.demoteFullChunk(param0, var8);
      }

      this.onLevelChange.onLevelChange(this.pos, this::getQueueLevel, this.ticketLevel, this::setQueueLevel);
      this.oldTicketLevel = this.ticketLevel;
   }

   public static ChunkStatus getStatus(int param0) {
      return param0 < 33 ? ChunkStatus.FULL : ChunkStatus.getStatusAroundFullChunk(param0 - 33);
   }

   public static ChunkHolder.FullChunkStatus getFullChunkStatus(int param0) {
      return FULL_CHUNK_STATUSES[Mth.clamp(33 - param0 + 1, 0, FULL_CHUNK_STATUSES.length - 1)];
   }

   public boolean wasAccessibleSinceLastSave() {
      return this.wasAccessibleSinceLastSave;
   }

   public void refreshAccessibility() {
      this.wasAccessibleSinceLastSave = getFullChunkStatus(this.ticketLevel).isOrAfter(ChunkHolder.FullChunkStatus.BORDER);
   }

   public void replaceProtoChunk(ImposterProtoChunk param0) {
      for (int var2 = 0; var2 < this.futures.length(); var2++) {
         CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var3 = this.futures.get(var2);
         if (var3 != null) {
            Optional<ChunkAccess> var4 = var3.getNow(UNLOADED_CHUNK).left();
            if (!var4.isEmpty() && var4.get() instanceof ProtoChunk) {
               this.futures.set(var2, CompletableFuture.completedFuture(Either.left(param0)));
            }
         }
      }

      this.updateChunkToSave(CompletableFuture.completedFuture(Either.left(param0.getWrapped())), "replaceProto");
   }

   public List<Pair<ChunkStatus, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>>> getAllFutures() {
      List<Pair<ChunkStatus, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>>> var1 = new ArrayList<>();

      for (int var2 = 0; var2 < CHUNK_STATUSES.size(); var2++) {
         var1.add(Pair.of(CHUNK_STATUSES.get(var2), this.futures.get(var2)));
      }

      return var1;
   }

   public interface ChunkLoadingFailure {
      ChunkHolder.ChunkLoadingFailure UNLOADED = new ChunkHolder.ChunkLoadingFailure() {
         @Override
         public String toString() {
            return "UNLOADED";
         }
      };
   }

   static final class ChunkSaveDebug {
      private final Thread thread;
      private final CompletableFuture<?> future;
      private final String source;

      ChunkSaveDebug(Thread param0, CompletableFuture<?> param1, String param2) {
         this.thread = param0;
         this.future = param1;
         this.source = param2;
      }
   }

   public enum FullChunkStatus {
      INACCESSIBLE,
      BORDER,
      TICKING,
      ENTITY_TICKING;

      public boolean isOrAfter(ChunkHolder.FullChunkStatus param0) {
         return this.ordinal() >= param0.ordinal();
      }
   }

   @FunctionalInterface
   public interface LevelChangeListener {
      void onLevelChange(ChunkPos var1, IntSupplier var2, int var3, IntConsumer var4);
   }

   public interface PlayerProvider {
      List<ServerPlayer> getPlayers(ChunkPos var1, boolean var2);
   }
}

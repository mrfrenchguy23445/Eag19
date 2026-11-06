package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformRuntime;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ChunkRenderDispatcher {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_WORKERS_32_BIT = 4;
   private static final VertexFormat VERTEX_FORMAT = DefaultVertexFormat.BLOCK;
   private static final int MAX_HIGH_PRIORITY_QUOTA = 2;
   // With no worker threads under WASM-GC the executor runs every task inline, so an unbounded
   // drain builds every dirty chunk in a single frame. Both stages below are capped per frame
   // and simply resume on the next one; a chunk arriving a frame late is invisible, a stall is not.
   private static final long MAX_COMPILE_MILLIS_PER_FRAME = 6L;
   private static final long MAX_UPLOAD_MILLIS_PER_FRAME = 4L;
   private static final int MAX_UPLOAD_COUNT_PER_FRAME = 16;
   private final PriorityBlockingQueue<ChunkRenderDispatcher.RenderChunk.ChunkCompileTask> toBatchHighPriority = Queues.newPriorityBlockingQueue();
   private final Queue<ChunkRenderDispatcher.RenderChunk.ChunkCompileTask> toBatchLowPriority = Queues.newLinkedBlockingDeque();
   private int highPriorityQuota = 2;
   private long compileWindowStart = Util.getMillis();
   private boolean compileDeferred;
   private final Queue<ChunkBufferBuilderPack> freeBuffers;
   private final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
   private volatile int toBatchCount;
   private volatile int freeBufferCount;
   final ChunkBufferBuilderPack fixedBuffers;
   private final ProcessorMailbox<Runnable> mailbox;
   private final Executor executor;
   ClientLevel level;
   final LevelRenderer renderer;
   private Vec3 camera = Vec3.ZERO;

   public ChunkRenderDispatcher(ClientLevel param0, LevelRenderer param1, Executor param2, boolean param3, ChunkBufferBuilderPack param4) {
      this.level = param0;
      this.renderer = param1;
      int var6 = Math.max(
   // Runtime.getRuntime().maxMemory() is absent from the WASM-GC TeaVM classlib. The
   // replacement is PlatformRuntime.maxMemory(), the same seam SystemReport's "Memory"
   // detail already uses, and the same fixed figure it reports: a WASM-GC module has one
   // fixed WebAssembly.Memory, so the heap ceiling and the heap size are one number rather
   // than a limit and a reading. See SystemReport's constructor for the full note.
         1, (int)(PlatformRuntime.maxMemory() * 0.3) / (RenderType.chunkBufferLayers().stream().mapToInt(RenderType::bufferSize).sum() * 4) - 1
      );
      int var7 = Runtime.getRuntime().availableProcessors();
      int var8 = param3 ? var7 : Math.min(var7, 4);
      int var9 = Math.max(1, Math.min(var8, var6));
      this.fixedBuffers = param4;
      List<ChunkBufferBuilderPack> var10 = Lists.newArrayListWithExpectedSize(var9);

      try {
         for (int var11 = 0; var11 < var9; var11++) {
            var10.add(new ChunkBufferBuilderPack());
         }
      } catch (OutOfMemoryError var11) {
         LOGGER.warn("Allocated only {}/{} buffers", var10.size(), var9);
         int var12 = Math.min(var10.size() * 2 / 3, var10.size() - 1);

         for (int var13 = 0; var13 < var12; var13++) {
            var10.remove(var10.size() - 1);
         }

         System.gc();
      }

      this.freeBuffers = Queues.newArrayDeque(var10);
      this.freeBufferCount = this.freeBuffers.size();
      this.executor = param2;
      this.mailbox = ProcessorMailbox.create(param2, "Chunk Renderer");
      this.mailbox.tell(this::runTask);
   }

   public void setLevel(ClientLevel param0) {
      this.level = param0;
   }

   private void runTask() {
      if (!this.freeBuffers.isEmpty()) {
         if (Util.getMillis() - this.compileWindowStart >= MAX_COMPILE_MILLIS_PER_FRAME) {
            this.compileDeferred = true;
            return;
         }

         ChunkRenderDispatcher.RenderChunk.ChunkCompileTask var1 = this.pollTask();
         if (var1 != null) {
            ChunkBufferBuilderPack var2 = this.freeBuffers.poll();
            this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
            this.freeBufferCount = this.freeBuffers.size();
            CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName(var1.name(), () -> var1.doTask(var2)), this.executor)
               .thenCompose(l_param0 -> (CompletionStage<ChunkRenderDispatcher.ChunkTaskResult>)l_param0)
               .whenComplete((l_param1, l_param2) -> {
                  if (l_param2 != null) {
                     Minecraft.getInstance().delayCrash(CrashReport.forThrowable(l_param2, "Batching chunks"));
                  } else {
                     this.mailbox.tell(() -> {
                        if (l_param1 == ChunkRenderDispatcher.ChunkTaskResult.SUCCESSFUL) {
                           var2.clearAll();
                        } else {
                           var2.discardAll();
                        }

                        this.freeBuffers.add(var2);
                        this.freeBufferCount = this.freeBuffers.size();
                        this.runTask();
                     });
                  }
               });
         }
      }
   }

   @Nullable
   private ChunkRenderDispatcher.RenderChunk.ChunkCompileTask pollTask() {
      if (this.highPriorityQuota <= 0) {
         ChunkRenderDispatcher.RenderChunk.ChunkCompileTask var1 = this.toBatchLowPriority.poll();
         if (var1 != null) {
            this.highPriorityQuota = 2;
            return var1;
         }
      }

      ChunkRenderDispatcher.RenderChunk.ChunkCompileTask var1 = this.toBatchHighPriority.poll();
      if (var1 != null) {
         this.highPriorityQuota--;
         return var1;
      } else {
         this.highPriorityQuota = 2;
         return this.toBatchLowPriority.poll();
      }
   }

   public String getStats() {
      return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.toBatchCount, this.toUpload.size(), this.freeBufferCount);
   }

   public int getToBatchCount() {
      return this.toBatchCount;
   }

   public int getToUpload() {
      return this.toUpload.size();
   }

   public int getFreeBufferCount() {
      return this.freeBufferCount;
   }

   public void setCamera(Vec3 param0) {
      this.camera = param0;
   }

   public Vec3 getCameraPosition() {
      return this.camera;
   }

   public void uploadAllPendingUploads() {
      // Called once per frame from LevelRenderer: this is the frame boundary that hands out a
      // fresh budget to both the compile stage above and the upload stage below.
      this.compileWindowStart = Util.getMillis();
      this.compileDeferred = false;
      long var1 = this.compileWindowStart + MAX_UPLOAD_MILLIS_PER_FRAME;
      Runnable var3;

      for (int var4 = 0; (var3 = this.toUpload.poll()) != null; var4++) {
         var3.run();
         if (var4 >= MAX_UPLOAD_COUNT_PER_FRAME || Util.getMillis() >= var1) {
            break;
         }
      }

      if (this.compileDeferred || this.toBatchHighPriority.size() + this.toBatchLowPriority.size() > 0) {
         this.mailbox.tell(this::runTask);
      }
   }

   public void rebuildChunkSync(ChunkRenderDispatcher.RenderChunk param0, RenderRegionCache param1) {
      param0.compileSync(param1);
   }

   public void blockUntilClear() {
      this.clearBatchQueue();
   }

   public void schedule(ChunkRenderDispatcher.RenderChunk.ChunkCompileTask param0) {
      this.mailbox.tell(() -> {
         if (param0.isHighPriority) {
            this.toBatchHighPriority.offer(param0);
         } else {
            this.toBatchLowPriority.offer(param0);
         }

         this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
         this.runTask();
      });
   }

   public CompletableFuture<Void> uploadChunkLayer(BufferBuilder.RenderedBuffer param0, VertexBuffer param1) {
      return CompletableFuture.runAsync(() -> {
         if (!param1.isInvalid()) {
            param1.bind();
            param1.upload(param0);
            VertexBuffer.unbind();
         }
      }, this.toUpload::add);
   }

   private void clearBatchQueue() {
      while (!this.toBatchHighPriority.isEmpty()) {
         ChunkRenderDispatcher.RenderChunk.ChunkCompileTask var1 = this.toBatchHighPriority.poll();
         if (var1 != null) {
            var1.cancel();
         }
      }

      while (!this.toBatchLowPriority.isEmpty()) {
         ChunkRenderDispatcher.RenderChunk.ChunkCompileTask var1 = this.toBatchLowPriority.poll();
         if (var1 != null) {
            var1.cancel();
         }
      }

      this.toBatchCount = 0;
   }

   public boolean isQueueEmpty() {
      return this.toBatchCount == 0 && this.toUpload.isEmpty();
   }

   public void dispose() {
      this.clearBatchQueue();
      this.mailbox.close();
      this.freeBuffers.clear();
   }

   enum ChunkTaskResult {
      SUCCESSFUL,
      CANCELLED;
   }

   public static class CompiledChunk {
      public static final ChunkRenderDispatcher.CompiledChunk UNCOMPILED = new ChunkRenderDispatcher.CompiledChunk() {
         @Override
         public boolean facesCanSeeEachother(Direction param0, Direction param1) {
            return false;
         }
      };
      final Set<RenderType> hasBlocks = new ObjectArraySet<>(RenderType.chunkBufferLayers().size());
      final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();
      VisibilitySet visibilitySet = new VisibilitySet();
      @Nullable
      BufferBuilder.SortState transparencyState;

      public boolean hasNoRenderableLayers() {
         return this.hasBlocks.isEmpty();
      }

      public boolean isEmpty(RenderType param0) {
         return !this.hasBlocks.contains(param0);
      }

      public List<BlockEntity> getRenderableBlockEntities() {
         return this.renderableBlockEntities;
      }

      public boolean facesCanSeeEachother(Direction param0, Direction param1) {
         return this.visibilitySet.visibilityBetween(param0, param1);
      }
   }

   public class RenderChunk {
      public static final int SIZE = 16;
      public final int index;
      public final AtomicReference<ChunkRenderDispatcher.CompiledChunk> compiled = new AtomicReference<>(ChunkRenderDispatcher.CompiledChunk.UNCOMPILED);
      final AtomicInteger initialCompilationCancelCount = new AtomicInteger(0);
      @Nullable
      private ChunkRenderDispatcher.RenderChunk.RebuildTask lastRebuildTask;
      @Nullable
      private ChunkRenderDispatcher.RenderChunk.ResortTransparencyTask lastResortTransparencyTask;
      private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
      private final Map<RenderType, VertexBuffer> buffers = RenderType.chunkBufferLayers()
         .stream()
         .collect(Collectors.toMap(l_param0 -> (RenderType)l_param0, l_param0 -> new VertexBuffer()));
      private AABB bb;
      private boolean dirty = true;
      final BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos(-1, -1, -1);
      private final BlockPos.MutableBlockPos[] relativeOrigins = Util.make(new BlockPos.MutableBlockPos[6], l_param0 -> {
         for (int l_var1 = 0; l_var1 < l_param0.length; l_var1++) {
            l_param0[l_var1] = new BlockPos.MutableBlockPos();
         }
      });
      private boolean playerChanged;

      public RenderChunk(int param1, int param2, int param3, int param4) {
         this.index = param1;
         this.setOrigin(param2, param3, param4);
      }

      private boolean doesChunkExistAt(BlockPos param0) {
         return ChunkRenderDispatcher.this.level
               .getChunk(SectionPos.blockToSectionCoord(param0.getX()), SectionPos.blockToSectionCoord(param0.getZ()), ChunkStatus.FULL, false)
            != null;
      }

      public boolean hasAllNeighbors() {
         int var1 = 24;
         return !(this.getDistToPlayerSqr() > 576.0)
            ? true
            : this.doesChunkExistAt(this.relativeOrigins[Direction.WEST.ordinal()])
               && this.doesChunkExistAt(this.relativeOrigins[Direction.NORTH.ordinal()])
               && this.doesChunkExistAt(this.relativeOrigins[Direction.EAST.ordinal()])
               && this.doesChunkExistAt(this.relativeOrigins[Direction.SOUTH.ordinal()]);
      }

      public AABB getBoundingBox() {
         return this.bb;
      }

      public VertexBuffer getBuffer(RenderType param0) {
         return this.buffers.get(param0);
      }

      public void setOrigin(int param0, int param1, int param2) {
         this.reset();
         this.origin.set(param0, param1, param2);
         this.bb = new AABB(param0, param1, param2, param0 + 16, param1 + 16, param2 + 16);

         for (Direction var7 : Direction.values()) {
            this.relativeOrigins[var7.ordinal()].set(this.origin).move(var7, 16);
         }
      }

      protected double getDistToPlayerSqr() {
         Camera var1 = Minecraft.getInstance().gameRenderer.getMainCamera();
         double var2 = this.bb.minX + 8.0 - var1.getPosition().x;
         double var4 = this.bb.minY + 8.0 - var1.getPosition().y;
         double var6 = this.bb.minZ + 8.0 - var1.getPosition().z;
         return var2 * var2 + var4 * var4 + var6 * var6;
      }

      void beginLayer(BufferBuilder param0) {
         param0.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
      }

      public ChunkRenderDispatcher.CompiledChunk getCompiledChunk() {
         return this.compiled.get();
      }

      private void reset() {
         this.cancelTasks();
         this.compiled.set(ChunkRenderDispatcher.CompiledChunk.UNCOMPILED);
         this.dirty = true;
      }

      public void releaseBuffers() {
         this.reset();
         this.buffers.values().forEach(VertexBuffer::close);
      }

      public BlockPos getOrigin() {
         return this.origin;
      }

      public void setDirty(boolean param0) {
         boolean var2 = this.dirty;
         this.dirty = true;
         this.playerChanged = param0 | (var2 && this.playerChanged);
      }

      public void setNotDirty() {
         this.dirty = false;
         this.playerChanged = false;
      }

      public boolean isDirty() {
         return this.dirty;
      }

      public boolean isDirtyFromPlayer() {
         return this.dirty && this.playerChanged;
      }

      public BlockPos getRelativeOrigin(Direction param0) {
         return this.relativeOrigins[param0.ordinal()];
      }

      public boolean resortTransparency(RenderType param0, ChunkRenderDispatcher param1) {
         ChunkRenderDispatcher.CompiledChunk var3 = this.getCompiledChunk();
         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
         }

         if (!var3.hasBlocks.contains(param0)) {
            return false;
         }

         this.lastResortTransparencyTask = new ChunkRenderDispatcher.RenderChunk.ResortTransparencyTask(this.getDistToPlayerSqr(), var3);
         param1.schedule(this.lastResortTransparencyTask);
         return true;
      }

      protected boolean cancelTasks() {
         boolean var1 = false;
         if (this.lastRebuildTask != null) {
            this.lastRebuildTask.cancel();
            this.lastRebuildTask = null;
            var1 = true;
         }

         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
            this.lastResortTransparencyTask = null;
         }

         return var1;
      }

      public ChunkRenderDispatcher.RenderChunk.ChunkCompileTask createCompileTask(RenderRegionCache param0) {
         boolean var2 = this.cancelTasks();
         BlockPos var3 = this.origin.immutable();
         int var4 = 1;
         RenderChunkRegion var5 = param0.createRegion(ChunkRenderDispatcher.this.level, var3.offset(-1, -1, -1), var3.offset(16, 16, 16), 1);
         boolean var6 = this.compiled.get() == ChunkRenderDispatcher.CompiledChunk.UNCOMPILED;
         if (var6 && var2) {
            this.initialCompilationCancelCount.incrementAndGet();
         }

         this.lastRebuildTask = new ChunkRenderDispatcher.RenderChunk.RebuildTask(
            this.getDistToPlayerSqr(), var5, !var6 || this.initialCompilationCancelCount.get() > 2
         );
         return this.lastRebuildTask;
      }

      public void rebuildChunkAsync(ChunkRenderDispatcher param0, RenderRegionCache param1) {
         ChunkRenderDispatcher.RenderChunk.ChunkCompileTask var3 = this.createCompileTask(param1);
         param0.schedule(var3);
      }

      void updateGlobalBlockEntities(Collection<BlockEntity> param0) {
         Set<BlockEntity> var2 = Sets.newHashSet(param0);
         Set<BlockEntity> var3;
         synchronized (this.globalBlockEntities) {
            var3 = Sets.newHashSet(this.globalBlockEntities);
            var2.removeAll(this.globalBlockEntities);
            var3.removeAll(param0);
            this.globalBlockEntities.clear();
            this.globalBlockEntities.addAll(param0);
         }

         ChunkRenderDispatcher.this.renderer.updateGlobalBlockEntities(var3, var2);
      }

      public void compileSync(RenderRegionCache param0) {
         ChunkRenderDispatcher.RenderChunk.ChunkCompileTask var2 = this.createCompileTask(param0);
         var2.doTask(ChunkRenderDispatcher.this.fixedBuffers);
      }

      abstract class ChunkCompileTask implements Comparable<ChunkRenderDispatcher.RenderChunk.ChunkCompileTask> {
         protected final double distAtCreation;
         protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
         protected final boolean isHighPriority;

         public ChunkCompileTask(double param1, boolean param3) {
            this.distAtCreation = param1;
            this.isHighPriority = param3;
         }

         public abstract CompletableFuture<ChunkRenderDispatcher.ChunkTaskResult> doTask(ChunkBufferBuilderPack var1);

         public abstract void cancel();

         protected abstract String name();

         public int compareTo(ChunkRenderDispatcher.RenderChunk.ChunkCompileTask param0) {
            return Doubles.compare(this.distAtCreation, param0.distAtCreation);
         }
      }

      class RebuildTask extends ChunkRenderDispatcher.RenderChunk.ChunkCompileTask {
         @Nullable
         protected RenderChunkRegion region;

         public RebuildTask(double param1, @Nullable RenderChunkRegion param3, boolean param4) {
            super(param1, param4);
            this.region = param3;
         }

         @Override
         protected String name() {
            return "rend_chk_rebuild";
         }

         @Override
         public CompletableFuture<ChunkRenderDispatcher.ChunkTaskResult> doTask(ChunkBufferBuilderPack param0) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(ChunkRenderDispatcher.ChunkTaskResult.CANCELLED);
            } else if (!RenderChunk.this.hasAllNeighbors()) {
               this.region = null;
               RenderChunk.this.setDirty(false);
               this.isCancelled.set(true);
               return CompletableFuture.completedFuture(ChunkRenderDispatcher.ChunkTaskResult.CANCELLED);
            } else if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(ChunkRenderDispatcher.ChunkTaskResult.CANCELLED);
            } else {
               Vec3 var2 = ChunkRenderDispatcher.this.getCameraPosition();
               float var3 = (float)var2.x;
               float var4 = (float)var2.y;
               float var5 = (float)var2.z;
               ChunkRenderDispatcher.RenderChunk.RebuildTask.CompileResults var6 = this.compile(var3, var4, var5, param0);
               RenderChunk.this.updateGlobalBlockEntities(var6.globalBlockEntities);
               if (this.isCancelled.get()) {
                  var6.renderedLayers.values().forEach(BufferBuilder.RenderedBuffer::release);
                  return CompletableFuture.completedFuture(ChunkRenderDispatcher.ChunkTaskResult.CANCELLED);
               } else {
                  ChunkRenderDispatcher.CompiledChunk var7 = new ChunkRenderDispatcher.CompiledChunk();
                  var7.visibilitySet = var6.visibilitySet;
                  var7.renderableBlockEntities.addAll(var6.blockEntities);
                  var7.transparencyState = var6.transparencyState;
                  List<CompletableFuture<Void>> var8 = Lists.newArrayList();
                  var6.renderedLayers.forEach((l_param2, l_param3) -> {
                     var8.add(ChunkRenderDispatcher.this.uploadChunkLayer(l_param3, RenderChunk.this.getBuffer(l_param2)));
                     var7.hasBlocks.add(l_param2);
                  });
                  return Util.sequenceFailFast(var8).handle((l_param1, l_param2) -> {
                     if (l_param2 != null && !(l_param2 instanceof CancellationException) && !(l_param2 instanceof InterruptedException)) {
                        Minecraft.getInstance().delayCrash(CrashReport.forThrowable(l_param2, "Rendering chunk"));
                     }

                     if (this.isCancelled.get()) {
                        return ChunkRenderDispatcher.ChunkTaskResult.CANCELLED;
                     }

                     RenderChunk.this.compiled.set(var7);
                     RenderChunk.this.initialCompilationCancelCount.set(0);
                     ChunkRenderDispatcher.this.renderer.addRecentlyCompiledChunk(RenderChunk.this);
                     return ChunkRenderDispatcher.ChunkTaskResult.SUCCESSFUL;
                  });
               }
            }
         }

         private ChunkRenderDispatcher.RenderChunk.RebuildTask.CompileResults compile(float param0, float param1, float param2, ChunkBufferBuilderPack param3) {
            ChunkRenderDispatcher.RenderChunk.RebuildTask.CompileResults var5 = new ChunkRenderDispatcher.RenderChunk.RebuildTask.CompileResults();
            int var6 = 1;
            BlockPos var7 = RenderChunk.this.origin.immutable();
            BlockPos var8 = var7.offset(15, 15, 15);
            VisGraph var9 = new VisGraph();
            RenderChunkRegion var10 = this.region;
            this.region = null;
            PoseStack var11 = new PoseStack();
            if (var10 != null) {
               ModelBlockRenderer.enableCaching();
               Set<RenderType> var12 = new ReferenceArraySet<>(RenderType.chunkBufferLayers().size());
               RandomSource var13 = RandomSource.create();
               BlockRenderDispatcher var14 = Minecraft.getInstance().getBlockRenderer();

               for (BlockPos var16 : BlockPos.betweenClosed(var7, var8)) {
                  BlockState var17 = var10.getBlockState(var16);
                  if (var17.isSolidRender(var10, var16)) {
                     var9.setOpaque(var16);
                  }

                  if (var17.hasBlockEntity()) {
                     BlockEntity var18 = var10.getBlockEntity(var16);
                     if (var18 != null) {
                        this.handleBlockEntity(var5, var18);
                     }
                  }

                  // Sodium-inspired cherry-pick (Phase D): the region lookup below repeated the
                  // identical fetch a few lines above for every one of the 4096 blocks; reuse it.
                  FluidState var19 = var17.getFluidState();
                  if (!var19.isEmpty()) {
                     RenderType var20 = ItemBlockRenderTypes.getRenderLayer(var19);
                     BufferBuilder var21 = param3.builder(var20);
                     if (var12.add(var20)) {
                        RenderChunk.this.beginLayer(var21);
                     }

                     var14.renderLiquid(var16, var10, var21, var17, var19);
                  }

                  if (var17.getRenderShape() != RenderShape.INVISIBLE) {
                     RenderType var20 = ItemBlockRenderTypes.getChunkRenderType(var17);
                     BufferBuilder var21 = param3.builder(var20);
                     if (var12.add(var20)) {
                        RenderChunk.this.beginLayer(var21);
                     }

                     var11.pushPose();
                     var11.translate(var16.getX() & 15, var16.getY() & 15, var16.getZ() & 15);
                     var14.renderBatched(var17, var16, var10, var11, var21, true, var13);
                     var11.popPose();
                  }
               }

               if (var12.contains(RenderType.translucent())) {
                  BufferBuilder var15 = param3.builder(RenderType.translucent());
                  if (!var15.isCurrentBatchEmpty()) {
                     var15.setQuadSortOrigin(param0 - var7.getX(), param1 - var7.getY(), param2 - var7.getZ());
                     var5.transparencyState = var15.getSortState();
                  }
               }

               for (RenderType var16 : var12) {
                  BufferBuilder.RenderedBuffer var17 = param3.builder(var16).endOrDiscardIfEmpty();
                  if (var17 != null) {
                     var5.renderedLayers.put(var16, var17);
                  }
               }

               ModelBlockRenderer.clearCache();
            }

            var5.visibilitySet = var9.resolve();
            return var5;
         }

         private <E extends BlockEntity> void handleBlockEntity(ChunkRenderDispatcher.RenderChunk.RebuildTask.CompileResults param0, E param1) {
            BlockEntityRenderer<E> var3 = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(param1);
            if (var3 != null) {
               param0.blockEntities.add(param1);
               if (var3.shouldRenderOffScreen(param1)) {
                  param0.globalBlockEntities.add(param1);
               }
            }
         }

         @Override
         public void cancel() {
            this.region = null;
            if (this.isCancelled.compareAndSet(false, true)) {
               RenderChunk.this.setDirty(false);
            }
         }

         static final class CompileResults {
            public final List<BlockEntity> globalBlockEntities = new ArrayList<>();
            public final List<BlockEntity> blockEntities = new ArrayList<>();
            public final Map<RenderType, BufferBuilder.RenderedBuffer> renderedLayers = new Reference2ObjectArrayMap<>();
            public VisibilitySet visibilitySet = new VisibilitySet();
            @Nullable
            public BufferBuilder.SortState transparencyState;
         }
      }

      class ResortTransparencyTask extends ChunkRenderDispatcher.RenderChunk.ChunkCompileTask {
         private final ChunkRenderDispatcher.CompiledChunk compiledChunk;

         public ResortTransparencyTask(double param1, ChunkRenderDispatcher.CompiledChunk param3) {
            super(param1, true);
            this.compiledChunk = param3;
         }

         @Override
         protected String name() {
            return "rend_chk_sort";
         }

         @Override
         public CompletableFuture<ChunkRenderDispatcher.ChunkTaskResult> doTask(ChunkBufferBuilderPack param0) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(ChunkRenderDispatcher.ChunkTaskResult.CANCELLED);
            }

            if (!RenderChunk.this.hasAllNeighbors()) {
               this.isCancelled.set(true);
               return CompletableFuture.completedFuture(ChunkRenderDispatcher.ChunkTaskResult.CANCELLED);
            }

            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(ChunkRenderDispatcher.ChunkTaskResult.CANCELLED);
            }

            Vec3 var2 = ChunkRenderDispatcher.this.getCameraPosition();
            float var3 = (float)var2.x;
            float var4 = (float)var2.y;
            float var5 = (float)var2.z;
            BufferBuilder.SortState var6 = this.compiledChunk.transparencyState;
            if (var6 != null && !this.compiledChunk.isEmpty(RenderType.translucent())) {
               BufferBuilder var7 = param0.builder(RenderType.translucent());
               RenderChunk.this.beginLayer(var7);
               var7.restoreSortState(var6);
               var7.setQuadSortOrigin(var3 - RenderChunk.this.origin.getX(), var4 - RenderChunk.this.origin.getY(), var5 - RenderChunk.this.origin.getZ());
               this.compiledChunk.transparencyState = var7.getSortState();
               BufferBuilder.RenderedBuffer var8 = var7.end();
               if (this.isCancelled.get()) {
                  var8.release();
                  return CompletableFuture.completedFuture(ChunkRenderDispatcher.ChunkTaskResult.CANCELLED);
               } else {
                  CompletableFuture<ChunkRenderDispatcher.ChunkTaskResult> var9 = ChunkRenderDispatcher.this.uploadChunkLayer(
                        var8, RenderChunk.this.getBuffer(RenderType.translucent())
                     )
                     .thenApply(l_param0 -> ChunkRenderDispatcher.ChunkTaskResult.CANCELLED);
                  return var9.handle((l_param0, l_param1) -> {
                     if (l_param1 != null && !(l_param1 instanceof CancellationException) && !(l_param1 instanceof InterruptedException)) {
                        Minecraft.getInstance().delayCrash(CrashReport.forThrowable(l_param1, "Rendering chunk"));
                     }

                     return this.isCancelled.get() ? ChunkRenderDispatcher.ChunkTaskResult.CANCELLED : ChunkRenderDispatcher.ChunkTaskResult.SUCCESSFUL;
                  });
               }
            } else {
               return CompletableFuture.completedFuture(ChunkRenderDispatcher.ChunkTaskResult.CANCELLED);
            }
         }

         @Override
         public void cancel() {
            this.isCancelled.set(true);
         }
      }
   }
}

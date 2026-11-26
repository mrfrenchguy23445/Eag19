package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class ChunkStatus {
   public static final int MAX_STRUCTURE_DISTANCE = 8;
   private static final EnumSet<Heightmap.Types> PRE_FEATURES = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);
   public static final EnumSet<Heightmap.Types> POST_FEATURES = EnumSet.of(
      Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE, Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES
   );
   private static final ChunkStatus.LoadingTask PASSTHROUGH_LOAD_TASK = (param0, param1, param2, param3, param4, param5) -> {
      if (param5 instanceof ProtoChunk var6 && !param5.getStatus().isOrAfter(param0)) {
         var6.setStatus(param0);
      }

      return CompletableFuture.completedFuture(Either.left(param5));
   };
   public static final ChunkStatus EMPTY = registerSimple(
      "empty", null, -1, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (param0, param1, param2, param3, param4) -> {}
   );
   public static final ChunkStatus STRUCTURE_STARTS = register(
      "structure_starts",
      EMPTY,
      0,
      PRE_FEATURES,
      ChunkStatus.ChunkType.PROTOCHUNK,
      (param0, param1, param2, param3, param4, param5, param6, param7, param8, param9) -> {
         if (!param8.getStatus().isOrAfter(param0)) {
            if (param2.getServer().getWorldData().worldGenOptions().generateStructures()) {
               param3.createStructures(param2.registryAccess(), param2.getChunkSource().getGeneratorState(), param2.structureManager(), param8, param4);
            }

            if (param8 instanceof ProtoChunk var10) {
               var10.setStatus(param0);
            }

            param2.onStructureStartsAvailable(param8);
         }

         return CompletableFuture.completedFuture(Either.left(param8));
      },
      (param0, param1, param2, param3, param4, param5) -> {
         if (!param5.getStatus().isOrAfter(param0)) {
            if (param5 instanceof ProtoChunk var6) {
               var6.setStatus(param0);
            }

            param1.onStructureStartsAvailable(param5);
         }

         return CompletableFuture.completedFuture(Either.left(param5));
      }
   );
   public static final ChunkStatus STRUCTURE_REFERENCES = registerSimple(
      "structure_references", STRUCTURE_STARTS, 8, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (param0, param1, param2, param3, param4) -> {
         WorldGenRegion var5 = new WorldGenRegion(param1, param3, param0, -1);
         param2.createReferences(var5, param1.structureManager().forWorldGenRegion(var5), param4);
      }
   );
   public static final ChunkStatus BIOMES = register(
      "biomes",
      STRUCTURE_REFERENCES,
      8,
      PRE_FEATURES,
      ChunkStatus.ChunkType.PROTOCHUNK,
      (param0, param1, param2, param3, param4, param5, param6, param7, param8, param9) -> {
         if (!param9 && param8.getStatus().isOrAfter(param0)) {
            return CompletableFuture.completedFuture(Either.left(param8));
         }

         WorldGenRegion var10 = new WorldGenRegion(param2, param7, param0, -1);
         return param3.createBiomes(
               param1, param2.getChunkSource().randomState(), Blender.of(var10), param2.structureManager().forWorldGenRegion(var10), param8
            )
            .thenApply(l_param1 -> {
               if (l_param1 instanceof ProtoChunk) {
                  ((ProtoChunk)l_param1).setStatus(param0);
               }

               return Either.left(l_param1);
            });
      }
   );
   public static final ChunkStatus NOISE = register(
      "noise",
      BIOMES,
      8,
      PRE_FEATURES,
      ChunkStatus.ChunkType.PROTOCHUNK,
      (param0, param1, param2, param3, param4, param5, param6, param7, param8, param9) -> {
         if (!param9 && param8.getStatus().isOrAfter(param0)) {
            return CompletableFuture.completedFuture(Either.left(param8));
         }

         WorldGenRegion var10 = new WorldGenRegion(param2, param7, param0, 0);
         return param3.fillFromNoise(
               param1, Blender.of(var10), param2.getChunkSource().randomState(), param2.structureManager().forWorldGenRegion(var10), param8
            )
            .thenApply(l_param1 -> {
               if (l_param1 instanceof ProtoChunk l_var2) {
                  BelowZeroRetrogen l_var3 = l_var2.getBelowZeroRetrogen();
                  if (l_var3 != null) {
                     BelowZeroRetrogen.replaceOldBedrock(l_var2);
                     if (l_var3.hasBedrockHoles()) {
                        l_var3.applyBedrockMask(l_var2);
                     }
                  }

                  l_var2.setStatus(param0);
               }

               return Either.left(l_param1);
            });
      }
   );
   public static final ChunkStatus SURFACE = registerSimple(
      "surface", NOISE, 8, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (param0, param1, param2, param3, param4) -> {
         WorldGenRegion var5 = new WorldGenRegion(param1, param3, param0, 0);
         param2.buildSurface(var5, param1.structureManager().forWorldGenRegion(var5), param1.getChunkSource().randomState(), param4);
      }
   );
   public static final ChunkStatus CARVERS = registerSimple(
      "carvers",
      SURFACE,
      8,
      PRE_FEATURES,
      ChunkStatus.ChunkType.PROTOCHUNK,
      (param0, param1, param2, param3, param4) -> {
         WorldGenRegion var5 = new WorldGenRegion(param1, param3, param0, 0);
         if (param4 instanceof ProtoChunk var6) {
            Blender.addAroundOldChunksCarvingMaskFilter(var5, var6);
         }

         param2.applyCarvers(
            var5,
            param1.getSeed(),
            param1.getChunkSource().randomState(),
            param1.getBiomeManager(),
            param1.structureManager().forWorldGenRegion(var5),
            param4,
            GenerationStep.Carving.AIR
         );
      }
   );
   public static final ChunkStatus LIQUID_CARVERS = registerSimple(
      "liquid_carvers", CARVERS, 8, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (param0, param1, param2, param3, param4) -> {}
   );
   public static final ChunkStatus FEATURES = register(
      "features",
      LIQUID_CARVERS,
      8,
      POST_FEATURES,
      ChunkStatus.ChunkType.PROTOCHUNK,
      (param0, param1, param2, param3, param4, param5, param6, param7, param8, param9) -> {
         ProtoChunk var10 = (ProtoChunk)param8;
         var10.setLightEngine(param5);
         if (param9 || !param8.getStatus().isOrAfter(param0)) {
            Heightmap.primeHeightmaps(
               param8,
               EnumSet.of(
                  Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE
               )
            );
            WorldGenRegion var11 = new WorldGenRegion(param2, param7, param0, 1);
            param3.applyBiomeDecoration(var11, param8, param2.structureManager().forWorldGenRegion(var11));
            Blender.generateBorderTicks(var11, param8);
            var10.setStatus(param0);
         }

         return param5.retainData(param8).thenApply(Either::left);
      },
      (param0, param1, param2, param3, param4, param5) -> param3.retainData(param5).thenApply(Either::left)
   );
   public static final ChunkStatus LIGHT = register(
      "light",
      FEATURES,
      1,
      POST_FEATURES,
      ChunkStatus.ChunkType.PROTOCHUNK,
      (param0, param1, param2, param3, param4, param5, param6, param7, param8, param9) -> lightChunk(param0, param5, param8),
      (param0, param1, param2, param3, param4, param5) -> lightChunk(param0, param3, param5)
   );
   public static final ChunkStatus SPAWN = registerSimple(
      "spawn", LIGHT, 0, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (param0, param1, param2, param3, param4) -> {
         if (!param4.isUpgrading()) {
            param2.spawnOriginalMobs(new WorldGenRegion(param1, param3, param0, -1));
         }
      }
   );
   public static final ChunkStatus HEIGHTMAPS = registerSimple(
      "heightmaps", SPAWN, 0, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (param0, param1, param2, param3, param4) -> {}
   );
   public static final ChunkStatus FULL = register(
      "full",
      HEIGHTMAPS,
      0,
      POST_FEATURES,
      ChunkStatus.ChunkType.LEVELCHUNK,
      (param0, param1, param2, param3, param4, param5, param6, param7, param8, param9) -> param6.apply(param8),
      (param0, param1, param2, param3, param4, param5) -> param4.apply(param5)
   );
   private static final List<ChunkStatus> STATUS_BY_RANGE = ImmutableList.of(
      FULL,
      FEATURES,
      LIQUID_CARVERS,
      BIOMES,
      STRUCTURE_STARTS,
      STRUCTURE_STARTS,
      STRUCTURE_STARTS,
      STRUCTURE_STARTS,
      STRUCTURE_STARTS,
      STRUCTURE_STARTS,
      STRUCTURE_STARTS,
      STRUCTURE_STARTS
   );
   private static final IntList RANGE_BY_STATUS = Util.make(new IntArrayList(getStatusList().size()), param0 -> {
      int var1 = 0;

      for (int var2 = getStatusList().size() - 1; var2 >= 0; var2--) {
         while (var1 + 1 < STATUS_BY_RANGE.size() && var2 <= STATUS_BY_RANGE.get(var1 + 1).getIndex()) {
            var1++;
         }

         param0.add(0, var1);
      }
   });
   private final String name;
   private final int index;
   private final ChunkStatus parent;
   private final ChunkStatus.GenerationTask generationTask;
   private final ChunkStatus.LoadingTask loadingTask;
   private final int range;
   private final ChunkStatus.ChunkType chunkType;
   private final EnumSet<Heightmap.Types> heightmapsAfter;

   private static CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> lightChunk(
      ChunkStatus param0, ThreadedLevelLightEngine param1, ChunkAccess param2
   ) {
      boolean var3 = isLighted(param0, param2);
      if (!param2.getStatus().isOrAfter(param0)) {
         ((ProtoChunk)param2).setStatus(param0);
      }

      return param1.lightChunk(param2, var3).thenApply(Either::left);
   }

   private static ChunkStatus registerSimple(
      String param0,
      @Nullable ChunkStatus param1,
      int param2,
      EnumSet<Heightmap.Types> param3,
      ChunkStatus.ChunkType param4,
      ChunkStatus.SimpleGenerationTask param5
   ) {
      return register(param0, param1, param2, param3, param4, param5);
   }

   private static ChunkStatus register(
      String param0, @Nullable ChunkStatus param1, int param2, EnumSet<Heightmap.Types> param3, ChunkStatus.ChunkType param4, ChunkStatus.GenerationTask param5
   ) {
      return register(param0, param1, param2, param3, param4, param5, PASSTHROUGH_LOAD_TASK);
   }

   private static ChunkStatus register(
      String param0,
      @Nullable ChunkStatus param1,
      int param2,
      EnumSet<Heightmap.Types> param3,
      ChunkStatus.ChunkType param4,
      ChunkStatus.GenerationTask param5,
      ChunkStatus.LoadingTask param6
   ) {
      return Registry.register(BuiltInRegistries.CHUNK_STATUS, param0, new ChunkStatus(param0, param1, param2, param3, param4, param5, param6));
   }

   public static List<ChunkStatus> getStatusList() {
      List<ChunkStatus> var0 = Lists.newArrayList();

      ChunkStatus var1;
      for (var1 = FULL; var1.getParent() != var1; var1 = var1.getParent()) {
         var0.add(var1);
      }

      var0.add(var1);
      Collections.reverse(var0);
      return var0;
   }

   private static boolean isLighted(ChunkStatus param0, ChunkAccess param1) {
      return param1.getStatus().isOrAfter(param0) && param1.isLightCorrect();
   }

   public static ChunkStatus getStatusAroundFullChunk(int param0) {
      if (param0 >= STATUS_BY_RANGE.size()) {
         return EMPTY;
      } else {
         return param0 < 0 ? FULL : STATUS_BY_RANGE.get(param0);
      }
   }

   public static int maxDistance() {
      return STATUS_BY_RANGE.size();
   }

   public static int getDistance(ChunkStatus param0) {
      return RANGE_BY_STATUS.getInt(param0.getIndex());
   }

   ChunkStatus(
      String param0,
      @Nullable ChunkStatus param1,
      int param2,
      EnumSet<Heightmap.Types> param3,
      ChunkStatus.ChunkType param4,
      ChunkStatus.GenerationTask param5,
      ChunkStatus.LoadingTask param6
   ) {
      this.name = param0;
      this.parent = param1 == null ? this : param1;
      this.generationTask = param5;
      this.loadingTask = param6;
      this.range = param2;
      this.chunkType = param4;
      this.heightmapsAfter = param3;
      this.index = param1 == null ? 0 : param1.getIndex() + 1;
   }

   public int getIndex() {
      return this.index;
   }

   public String getName() {
      return this.name;
   }

   public ChunkStatus getParent() {
      return this.parent;
   }

   public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> generate(
      Executor param0,
      ServerLevel param1,
      ChunkGenerator param2,
      StructureTemplateManager param3,
      ThreadedLevelLightEngine param4,
      Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> param5,
      List<ChunkAccess> param6,
      boolean param7
   ) {
      ChunkAccess var9 = param6.get(param6.size() / 2);
      ProfiledDuration var10 = JvmProfiler.INSTANCE.onChunkGenerate(var9.getPos(), param1.dimension(), this.name);
      long var11 = System.nanoTime();
      String var12 = this.name;
      CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> var13 = this.generationTask
         .doWork(this, param0, param1, param2, param3, param4, param5, param6, var9, param7);
      if (var10 != null) {
         var13 = var13.thenApply(l_param1 -> {
            var10.finish();
            return l_param1;
         });
      }
      // Wasm-gc build: single cooperative thread, so future-completion time
      // ≈ real work time. Accumulate per-stage totals so "Preparing spawn
      // area" slowness can be attributed to a stage (FEATURES, LIGHT, FULL,
      // ...) instead of guessed at. See ChunkGenProfiler.
      return var13.thenApply(l_param1 -> {
         ChunkGenProfiler.record(var12, System.nanoTime() - var11);
         return l_param1;
      });
   }

   public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> load(
      ServerLevel param0,
      StructureTemplateManager param1,
      ThreadedLevelLightEngine param2,
      Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> param3,
      ChunkAccess param4
   ) {
      return this.loadingTask.doWork(this, param0, param1, param2, param3, param4);
   }

   public int getRange() {
      return this.range;
   }

   public ChunkStatus.ChunkType getChunkType() {
      return this.chunkType;
   }

   public static ChunkStatus byName(String param0) {
      return BuiltInRegistries.CHUNK_STATUS.get(ResourceLocation.tryParse(param0));
   }

   public EnumSet<Heightmap.Types> heightmapsAfter() {
      return this.heightmapsAfter;
   }

   public boolean isOrAfter(ChunkStatus param0) {
      return this.getIndex() >= param0.getIndex();
   }

   @Override
   public String toString() {
      return BuiltInRegistries.CHUNK_STATUS.getKey(this).toString();
   }

   public enum ChunkType {
      PROTOCHUNK,
      LEVELCHUNK;
   }

   interface GenerationTask {
      CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(
         ChunkStatus var1,
         Executor var2,
         ServerLevel var3,
         ChunkGenerator var4,
         StructureTemplateManager var5,
         ThreadedLevelLightEngine var6,
         Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> var7,
         List<ChunkAccess> var8,
         ChunkAccess var9,
         boolean var10
      );
   }

   interface LoadingTask {
      CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(
         ChunkStatus var1,
         ServerLevel var2,
         StructureTemplateManager var3,
         ThreadedLevelLightEngine var4,
         Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> var5,
         ChunkAccess var6
      );
   }

   interface SimpleGenerationTask extends ChunkStatus.GenerationTask {
      @Override
      default CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(
         ChunkStatus param0,
         Executor param1,
         ServerLevel param2,
         ChunkGenerator param3,
         StructureTemplateManager param4,
         ThreadedLevelLightEngine param5,
         Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> param6,
         List<ChunkAccess> param7,
         ChunkAccess param8,
         boolean param9
      ) {
         if (param9 || !param8.getStatus().isOrAfter(param0)) {
            this.doWork(param0, param2, param3, param7, param8);
            if (param8 instanceof ProtoChunk var11) {
               var11.setStatus(param0);
            }
         }

         return CompletableFuture.completedFuture(Either.left(param8));
      }

      void doWork(ChunkStatus var1, ServerLevel var2, ChunkGenerator var3, List<ChunkAccess> var4, ChunkAccess var5);
   }
}

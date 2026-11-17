package net.minecraft.world.level.chunk;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public abstract class ChunkGenerator {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<ChunkGenerator> CODEC = BuiltInRegistries.CHUNK_GENERATOR.byNameCodec().dispatchStable(ChunkGenerator::codec, Function.identity());
   protected final BiomeSource biomeSource;
   private final Supplier<List<FeatureSorter.StepFeatureData>> featuresPerStep;
   private final Function<Holder<Biome>, BiomeGenerationSettings> generationSettingsGetter;

   public ChunkGenerator(BiomeSource param0) {
      this(param0, l_param0 -> l_param0.value().getGenerationSettings());
   }

   public ChunkGenerator(BiomeSource param0, Function<Holder<Biome>, BiomeGenerationSettings> param1) {
      this.biomeSource = param0;
      this.generationSettingsGetter = param1;
      this.featuresPerStep = Suppliers.<List<FeatureSorter.StepFeatureData>>memoize(
         () -> FeatureSorter.buildFeaturesPerStep(List.copyOf(param0.possibleBiomes()), l2_param1 -> param1.apply(l2_param1).features(), true)
      );
   }

   protected abstract Codec<? extends ChunkGenerator> codec();

   public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> param0, RandomState param1, long param2) {
      return ChunkGeneratorStructureState.createForNormal(param1, param2, this.biomeSource, param0);
   }

   public Optional<ResourceKey<Codec<? extends ChunkGenerator>>> getTypeNameForDataFixer() {
      return BuiltInRegistries.CHUNK_GENERATOR.getResourceKey(this.codec());
   }

   public CompletableFuture<ChunkAccess> createBiomes(Executor param0, RandomState param1, Blender param2, StructureManager param3, ChunkAccess param4) {
      return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
         param4.fillBiomesFromNoise(this.biomeSource, param1.sampler());
         return param4;
      }), Util.backgroundExecutor());
   }

   public abstract void applyCarvers(
      WorldGenRegion var1, long var2, RandomState var4, BiomeManager var5, StructureManager var6, ChunkAccess var7, GenerationStep.Carving var8
   );

   @Nullable
   public Pair<BlockPos, Holder<Structure>> findNearestMapStructure(
      ServerLevel param0, HolderSet<Structure> param1, BlockPos param2, int param3, boolean param4
   ) {
      ChunkGeneratorStructureState var6 = param0.getChunkSource().getGeneratorState();
      Map<StructurePlacement, Set<Holder<Structure>>> var7 = new Object2ObjectArrayMap<>();

      for (Holder<Structure> var9 : param1) {
         for (StructurePlacement var11 : var6.getPlacementsForStructure(var9)) {
            var7.computeIfAbsent(var11, l_param0 -> new ObjectArraySet<>()).add(var9);
         }
      }

      if (var7.isEmpty()) {
         return null;
      }

      Pair<BlockPos, Holder<Structure>> var8 = null;
      double var9 = Double.MAX_VALUE;
      StructureManager var11 = param0.structureManager();
      List<Entry<StructurePlacement, Set<Holder<Structure>>>> var12 = new ArrayList<>(var7.size());

      for (Entry<StructurePlacement, Set<Holder<Structure>>> var14 : var7.entrySet()) {
         StructurePlacement var15 = var14.getKey();
         if (var15 instanceof ConcentricRingsStructurePlacement var16) {
            Pair<BlockPos, Holder<Structure>> var17 = this.getNearestGeneratedStructure(var14.getValue(), param0, var11, param2, param4, var16);
            if (var17 != null) {
               BlockPos var18 = var17.getFirst();
               double var19 = param2.distSqr(var18);
               if (var19 < var9) {
                  var9 = var19;
                  var8 = var17;
               }
            }
         } else if (var15 instanceof RandomSpreadStructurePlacement) {
            var12.add(var14);
         }
      }

      if (!var12.isEmpty()) {
         int var13 = SectionPos.blockToSectionCoord(param2.getX());
         int var14 = SectionPos.blockToSectionCoord(param2.getZ());

         for (int var15 = 0; var15 <= param3; var15++) {
            boolean var16 = false;

            for (Entry<StructurePlacement, Set<Holder<Structure>>> var18 : var12) {
               RandomSpreadStructurePlacement var19 = (RandomSpreadStructurePlacement)var18.getKey();
               Pair<BlockPos, Holder<Structure>> var20 = getNearestGeneratedStructure(
                  var18.getValue(), param0, var11, var13, var14, var15, param4, var6.getLevelSeed(), var19
               );
               if (var20 != null) {
                  var16 = true;
                  double var21 = param2.distSqr(var20.getFirst());
                  if (var21 < var9) {
                     var9 = var21;
                     var8 = var20;
                  }
               }
            }

            if (var16) {
               return var8;
            }
         }
      }

      return var8;
   }

   @Nullable
   private Pair<BlockPos, Holder<Structure>> getNearestGeneratedStructure(
      Set<Holder<Structure>> param0, ServerLevel param1, StructureManager param2, BlockPos param3, boolean param4, ConcentricRingsStructurePlacement param5
   ) {
      List<ChunkPos> var7 = param1.getChunkSource().getGeneratorState().getRingPositionsFor(param5);
      if (var7 == null) {
         throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
      }

      Pair<BlockPos, Holder<Structure>> var8 = null;
      double var9 = Double.MAX_VALUE;
      BlockPos.MutableBlockPos var11 = new BlockPos.MutableBlockPos();

      for (ChunkPos var13 : var7) {
         var11.set(SectionPos.sectionToBlockCoord(var13.x, 8), 32, SectionPos.sectionToBlockCoord(var13.z, 8));
         double var14 = var11.distSqr(param3);
         boolean var16 = var8 == null || var14 < var9;
         if (var16) {
            Pair<BlockPos, Holder<Structure>> var17 = getStructureGeneratingAt(param0, param1, param2, param4, param5, var13);
            if (var17 != null) {
               var8 = var17;
               var9 = var14;
            }
         }
      }

      return var8;
   }

   @Nullable
   private static Pair<BlockPos, Holder<Structure>> getNearestGeneratedStructure(
      Set<Holder<Structure>> param0,
      LevelReader param1,
      StructureManager param2,
      int param3,
      int param4,
      int param5,
      boolean param6,
      long param7,
      RandomSpreadStructurePlacement param9
   ) {
      int var10 = param9.spacing();

      for (int var11 = -param5; var11 <= param5; var11++) {
         boolean var12 = var11 == -param5 || var11 == param5;

         for (int var13 = -param5; var13 <= param5; var13++) {
            boolean var14 = var13 == -param5 || var13 == param5;
            if (var12 || var14) {
               int var15 = param3 + var10 * var11;
               int var16 = param4 + var10 * var13;
               ChunkPos var17 = param9.getPotentialStructureChunk(param7, var15, var16);
               Pair<BlockPos, Holder<Structure>> var18 = getStructureGeneratingAt(param0, param1, param2, param6, param9, var17);
               if (var18 != null) {
                  return var18;
               }
            }
         }
      }

      return null;
   }

   @Nullable
   private static Pair<BlockPos, Holder<Structure>> getStructureGeneratingAt(
      Set<Holder<Structure>> param0, LevelReader param1, StructureManager param2, boolean param3, StructurePlacement param4, ChunkPos param5
   ) {
      for (Holder<Structure> var7 : param0) {
         StructureCheckResult var8 = param2.checkStructurePresence(param5, var7.value(), param3);
         if (var8 != StructureCheckResult.START_NOT_PRESENT) {
            if (!param3 && var8 == StructureCheckResult.START_PRESENT) {
               return Pair.of(param4.getLocatePos(param5), var7);
            }

            ChunkAccess var9 = param1.getChunk(param5.x, param5.z, ChunkStatus.STRUCTURE_STARTS);
            StructureStart var10 = param2.getStartForStructure(SectionPos.bottomOf(var9), var7.value(), var9);
            if (var10 != null && var10.isValid() && (!param3 || tryAddReference(param2, var10))) {
               return Pair.of(param4.getLocatePos(var10.getChunkPos()), var7);
            }
         }
      }

      return null;
   }

   private static boolean tryAddReference(StructureManager param0, StructureStart param1) {
      if (param1.canBeReferenced()) {
         param0.addReference(param1);
         return true;
      } else {
         return false;
      }
   }

   public void applyBiomeDecoration(WorldGenLevel param0, ChunkAccess param1, StructureManager param2) {
      ChunkPos var4 = param1.getPos();
      if (!SharedConstants.debugVoidTerrain(var4)) {
         SectionPos var5 = SectionPos.of(var4, param0.getMinSection());
         BlockPos var6 = var5.origin();
         Registry<Structure> var7 = param0.registryAccess().registryOrThrow(Registries.STRUCTURE);
         // Cooperative-port: registry-wide stream + groupingBy was rebuilt per chunk-op;
         // cheap HashMap grouping instead.
         Map<Integer, List<Structure>> var8 = new java.util.HashMap<>();
         for (Structure l_struct : var7) {
            int l_step = l_struct.step().ordinal();
            List<Structure> l_list = var8.get(l_step);
            if (l_list == null) {
               l_list = new ArrayList<>();
               var8.put(l_step, l_list);
            }
            l_list.add(l_struct);
         }
         List<FeatureSorter.StepFeatureData> var9 = this.featuresPerStep.get();
         WorldgenRandom var10 = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.generateUniqueSeed()));
         long var11 = var10.setDecorationSeed(param0.getSeed(), var6.getX(), var6.getZ());
         Set<Holder<Biome>> var13 = new ObjectArraySet<>();
         // Cooperative-port: ChunkPos.rangeClosed used a spliterator-backed stream per
         // chunk-op; plain loops over the 3x3 neighbourhood, same ordering.
         for (int l_dz = -1; l_dz <= 1; l_dz++) {
            for (int l_dx = -1; l_dx <= 1; l_dx++) {
               ChunkPos l_cp = new ChunkPos(var4.x + l_dx, var4.z + l_dz);
               ChunkAccess l_var3 = param0.getChunk(l_cp.x, l_cp.z);

               for (LevelChunkSection l_var7 : l_var3.getSections()) {
                  l_var7.getBiomes().getAll(var13::add);
               }
            }
         }
         var13.retainAll(this.biomeSource.possibleBiomes());
         int var14 = var9.size();

         try {
            Registry<PlacedFeature> var15 = param0.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
            int var16 = Math.max(GenerationStep.Decoration.values().length, var14);

            for (int var17 = 0; var17 < var16; var17++) {
               int var18 = 0;
               if (param2.shouldGenerateStructures()) {
                  // Cooperative-port: getOrDefault avoided (emptyList crafted per step per
                  // chunk-op); Supplier captures are the same but the loop body is flat.
                  List<Structure> l_structs = var8.get(var17);
                  if (l_structs != null) {
                     for (Structure var21 : l_structs) {
                        var10.setFeatureSeed(var11, var18, var17);
                        Supplier<String> var22 = () -> var7.getResourceKey(var21).map(Object::toString).orElseGet(var21::toString);

                        try {
                           param0.setCurrentlyGenerating(var22);
                           for (StructureStart l_start : param2.startsForStructure(var5, var21)) {
                              l_start.placeInChunk(param0, param2, this, var10, getWritableArea(param1), var4);
                           }
                        } catch (Exception var23) {
                           skipBrokenFeature(var4, "structure", var22, var23);
                        }

                        var18++;
                     }
                  }
               }

               if (var17 < var14) {
                  IntSet var19 = new IntArraySet();

                  for (Holder<Biome> var21 : var13) {
                     List<HolderSet<PlacedFeature>> var22 = this.generationSettingsGetter.apply(var21).features();
                     if (var17 < var22.size()) {
                        HolderSet<PlacedFeature> var23 = var22.get(var17);
                        FeatureSorter.StepFeatureData var24 = var9.get(var17);
                        // Cooperative-port: indexed walk instead of stream().map().forEach.
                        List<Holder<PlacedFeature>> l_holders = var23.unwrap().right().orElse(java.util.Collections.emptyList());
                        for (int l_hi = 0; l_hi < l_holders.size(); l_hi++) {
                           var19.add(var24.indexMapping().applyAsInt(l_holders.get(l_hi).value()));
                        }
                     }
                  }

                  int var20 = var19.size();
                  int[] var21 = var19.toIntArray();
                  Arrays.sort(var21);
                  FeatureSorter.StepFeatureData var22 = var9.get(var17);

                  for (int var23 = 0; var23 < var20; var23++) {
                     int var24 = var21[var23];
                     PlacedFeature var25 = var22.features().get(var24);
                     Supplier<String> var26 = () -> var15.getResourceKey(var25).map(Object::toString).orElseGet(var25::toString);
                     var10.setFeatureSeed(var11, var24, var17);

                     try {
                        param0.setCurrentlyGenerating(var26);
                        var25.placeWithBiomeCheck(param0, this, var10, var6);
                     } catch (Exception var27) {
                        skipBrokenFeature(var4, "feature", var26, var27);
                     }
                  }
               }
            }

            param0.setCurrentlyGenerating(null);
         } catch (Exception var15) {
            // See skipBrokenFeature: a decoration failure must never complete the
            // chunk future exceptionally or world generation deadlocks for good.
            skipBrokenFeature(var4, "biome decoration", () -> "chunk " + var4.x + "," + var4.z, var15);
         }
      }
   }

   /**
    * Reports a feature/structure placement failure and continues generating the
    * chunk without it.
    *
    * <p>Vanilla rethrows these as {@link ReportedException}; on this cooperative
    * wasm build the exceptional future is then stored in
    * {@code ChunkHolder.futures[status]} and every chunk that depends on it waits
    * forever. The symptom is a permanent "Preparing spawn area" freeze with all
    * task queues empty. Losing one decorative feature is vastly preferable.
    */
   private static void skipBrokenFeature(ChunkPos pos, String kind, Supplier<String> description, Throwable cause) {
      int n = FEATURE_FAILURES.incrementAndGet();
      if (n > 25 && n % 100 != 0) {
         return;
      }
      String what;
      try {
         what = description.get();
      } catch (Throwable ignored) {
         what = "<unknown>";
      }
      LOGGER.error("[worldgen] skipped " + kind + " " + what + " at chunk " + pos.x + "," + pos.z + " (failure #" + n + ")", cause);
   }

   private static final java.util.concurrent.atomic.AtomicInteger FEATURE_FAILURES = new java.util.concurrent.atomic.AtomicInteger();

   private static BoundingBox getWritableArea(ChunkAccess param0) {
      ChunkPos var1 = param0.getPos();
      int var2 = var1.getMinBlockX();
      int var3 = var1.getMinBlockZ();
      LevelHeightAccessor var4 = param0.getHeightAccessorForGeneration();
      int var5 = var4.getMinBuildHeight() + 1;
      int var6 = var4.getMaxBuildHeight() - 1;
      return new BoundingBox(var2, var5, var3, var2 + 15, var6, var3 + 15);
   }

   public abstract void buildSurface(WorldGenRegion var1, StructureManager var2, RandomState var3, ChunkAccess var4);

   public abstract void spawnOriginalMobs(WorldGenRegion var1);

   public int getSpawnHeight(LevelHeightAccessor param0) {
      return 64;
   }

   public BiomeSource getBiomeSource() {
      return this.biomeSource;
   }

   public abstract int getGenDepth();

   public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> param0, StructureManager param1, MobCategory param2, BlockPos param3) {
      Map<Structure, LongSet> var5 = param1.getAllStructuresAt(param3);

      for (Entry<Structure, LongSet> var7 : var5.entrySet()) {
         Structure var8 = var7.getKey();
         StructureSpawnOverride var9 = var8.spawnOverrides().get(param2);
         if (var9 != null) {
            MutableBoolean var10 = new MutableBoolean(false);
            Predicate<StructureStart> var11 = var9.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE
               ? l_param2 -> param1.structureHasPieceAt(param3, l_param2)
               : l_param1 -> l_param1.getBoundingBox().isInside(param3);
            param1.fillStartsForStructure(var8, var7.getValue(), l_param2 -> {
               if (var10.isFalse() && var11.test(l_param2)) {
                  var10.setTrue();
               }
            });
            if (var10.isTrue()) {
               return var9.spawns();
            }
         }
      }

      return param0.value().getMobSettings().getMobs(param2);
   }

   public void createStructures(
      RegistryAccess param0, ChunkGeneratorStructureState param1, StructureManager param2, ChunkAccess param3, StructureTemplateManager param4
   ) {
      ChunkPos var6 = param3.getPos();
      SectionPos var7 = SectionPos.bottomOf(param3);
      RandomState var8 = param1.randomState();
      param1.possibleStructureSets().forEach(l_param8 -> {
         StructurePlacement l_var10 = l_param8.value().placement();
         List<StructureSet.StructureSelectionEntry> l_var11 = l_param8.value().structures();

         for (StructureSet.StructureSelectionEntry l_var13 : l_var11) {
            StructureStart l_var14 = param2.getStartForStructure(var7, l_var13.structure().value(), param3);
            if (l_var14 != null && l_var14.isValid()) {
               return;
            }
         }

         if (l_var10.isStructureChunk(param1, var6.x, var6.z)) {
            if (l_var11.size() == 1) {
               this.tryGenerateStructure(l_var11.get(0), param2, param0, var8, param4, param1.getLevelSeed(), param3, var6, var7);
            } else {
               ArrayList<StructureSet.StructureSelectionEntry> l_var12 = new ArrayList<>(l_var11.size());
               l_var12.addAll(l_var11);
               WorldgenRandom l_var13 = new WorldgenRandom(new LegacyRandomSource(0L));
               l_var13.setLargeFeatureSeed(param1.getLevelSeed(), var6.x, var6.z);
               int l_var14 = 0;

               for (StructureSet.StructureSelectionEntry l_var16 : l_var12) {
                  l_var14 += l_var16.weight();
               }

               while (!l_var12.isEmpty()) {
                  int l_var15 = l_var13.nextInt(l_var14);
                  int l_var16 = 0;

                  for (StructureSet.StructureSelectionEntry l_var18 : l_var12) {
                     l_var15 -= l_var18.weight();
                     if (l_var15 < 0) {
                        break;
                     }

                     l_var16++;
                  }

                  StructureSet.StructureSelectionEntry l_var17 = l_var12.get(l_var16);
                  if (this.tryGenerateStructure(l_var17, param2, param0, var8, param4, param1.getLevelSeed(), param3, var6, var7)) {
                     return;
                  }

                  l_var12.remove(l_var16);
                  l_var14 -= l_var17.weight();
               }
            }
         }
      });
   }

   private boolean tryGenerateStructure(
      StructureSet.StructureSelectionEntry param0,
      StructureManager param1,
      RegistryAccess param2,
      RandomState param3,
      StructureTemplateManager param4,
      long param5,
      ChunkAccess param7,
      ChunkPos param8,
      SectionPos param9
   ) {
      Structure var11 = param0.structure().value();
      int var12 = fetchReferences(param1, param7, param9, var11);
      HolderSet<Biome> var13 = var11.biomes();
      Predicate<Holder<Biome>> var14 = var13::contains;
      StructureStart var15 = var11.generate(param2, this, this.biomeSource, param3, param4, param5, param8, var12, param7, var14);
      if (var15.isValid()) {
         param1.setStartForStructure(param9, var11, var15, param7);
         return true;
      } else {
         return false;
      }
   }

   private static int fetchReferences(StructureManager param0, ChunkAccess param1, SectionPos param2, Structure param3) {
      StructureStart var4 = param0.getStartForStructure(param2, param3, param1);
      return var4 != null ? var4.getReferences() : 0;
   }

   public void createReferences(WorldGenLevel param0, StructureManager param1, ChunkAccess param2) {
      int var4 = 8;
      ChunkPos var5 = param2.getPos();
      int var6 = var5.x;
      int var7 = var5.z;
      int var8 = var5.getMinBlockX();
      int var9 = var5.getMinBlockZ();
      SectionPos var10 = SectionPos.bottomOf(param2);

      for (int var11 = var6 - 8; var11 <= var6 + 8; var11++) {
         for (int var12 = var7 - 8; var12 <= var7 + 8; var12++) {
            long var13 = ChunkPos.asLong(var11, var12);

            for (StructureStart var16 : param0.getChunk(var11, var12).getAllStarts().values()) {
               try {
                  if (var16.isValid() && var16.getBoundingBox().intersects(var8, var9, var8 + 15, var9 + 15)) {
                     param1.addReferenceForStructure(var10, var16.getStructure(), var13, param2);
                     DebugPackets.sendStructurePacket(param0, var16);
                  }
               } catch (Exception var17) {
                  CrashReport var18 = CrashReport.forThrowable(var17, "Generating structure reference");
                  CrashReportCategory var19 = var18.addCategory("Structure");
                  Optional<? extends Registry<Structure>> var20 = param0.registryAccess().registry(Registries.STRUCTURE);
                  var19.setDetail("Id", () -> var20.<String>map(l2_param1 -> l2_param1.getKey(var16.getStructure()).toString()).orElse("UNKNOWN"));
                  var19.setDetail("Name", () -> BuiltInRegistries.STRUCTURE_TYPE.getKey(var16.getStructure().type()).toString());
                  var19.setDetail("Class", () -> var16.getStructure().getClass().getCanonicalName());
                  throw new ReportedException(var18);
               }
            }
         }
      }
   }

   public abstract CompletableFuture<ChunkAccess> fillFromNoise(Executor var1, Blender var2, RandomState var3, StructureManager var4, ChunkAccess var5);

   public abstract int getSeaLevel();

   public abstract int getMinY();

   public abstract int getBaseHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor var4, RandomState var5);

   public abstract NoiseColumn getBaseColumn(int var1, int var2, LevelHeightAccessor var3, RandomState var4);

   public int getFirstFreeHeight(int param0, int param1, Heightmap.Types param2, LevelHeightAccessor param3, RandomState param4) {
      return this.getBaseHeight(param0, param1, param2, param3, param4);
   }

   public int getFirstOccupiedHeight(int param0, int param1, Heightmap.Types param2, LevelHeightAccessor param3, RandomState param4) {
      return this.getBaseHeight(param0, param1, param2, param3, param4) - 1;
   }

   public abstract void addDebugScreenInfo(List<String> var1, RandomState var2, BlockPos var3);

   @Deprecated
   public BiomeGenerationSettings getBiomeGenerationSettings(Holder<Biome> param0) {
      return this.generationSettingsGetter.apply(param0);
   }
}

package net.minecraft.world.level.chunk;

import com.google.common.base.Stopwatch;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.slf4j.Logger;

public class ChunkGeneratorStructureState {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final RandomState randomState;
   private final BiomeSource biomeSource;
   private final long levelSeed;
   private final long concentricRingsSeed;
   private final Map<Structure, List<StructurePlacement>> placementsForStructure = new Object2ObjectOpenHashMap<>();
   private final Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>> ringPositions = new Object2ObjectArrayMap<>();
   private boolean hasGeneratedPositions;
   private final List<Holder<StructureSet>> possibleStructureSets;

   public static ChunkGeneratorStructureState createForFlat(RandomState param0, long param1, BiomeSource param3, Stream<Holder<StructureSet>> param4) {
      List<Holder<StructureSet>> var5 = param4.filter(l_param1 -> hasBiomesForStructureSet(l_param1.value(), param3)).toList();
      return new ChunkGeneratorStructureState(param0, param3, param1, 0L, var5);
   }

   public static ChunkGeneratorStructureState createForNormal(RandomState param0, long param1, BiomeSource param3, HolderLookup<StructureSet> param4) {
      List<Holder<StructureSet>> var5 = param4.listElements()
         .filter(l_param1 -> hasBiomesForStructureSet(l_param1.value(), param3))
         .collect(Collectors.toUnmodifiableList());
      return new ChunkGeneratorStructureState(param0, param3, param1, param1, var5);
   }

   private static boolean hasBiomesForStructureSet(StructureSet param0, BiomeSource param1) {
      Stream<Holder<Biome>> var2 = param0.structures().stream().flatMap(l_param0 -> {
         Structure l_var1 = l_param0.structure().value();
         return l_var1.biomes().stream();
      });
      return var2.anyMatch(param1.possibleBiomes()::contains);
   }

   private ChunkGeneratorStructureState(RandomState param0, BiomeSource param1, long param2, long param4, List<Holder<StructureSet>> param6) {
      this.randomState = param0;
      this.levelSeed = param2;
      this.biomeSource = param1;
      this.concentricRingsSeed = param4;
      this.possibleStructureSets = param6;
   }

   public List<Holder<StructureSet>> possibleStructureSets() {
      return this.possibleStructureSets;
   }

   private void generatePositions() {
      Set<Holder<Biome>> var1 = this.biomeSource.possibleBiomes();
      this.possibleStructureSets().forEach(l_param1 -> {
         StructureSet l_var3 = l_param1.value();
         boolean l_var4 = false;

         for (StructureSet.StructureSelectionEntry l_var6 : l_var3.structures()) {
            Structure l_var7 = l_var6.structure().value();
            if (l_var7.biomes().stream().anyMatch(var1::contains)) {
               this.placementsForStructure.computeIfAbsent(l_var7, l2_param0 -> new ArrayList<>()).add(l_var3.placement());
               l_var4 = true;
            }
         }

         if (l_var4 && l_var3.placement() instanceof ConcentricRingsStructurePlacement l_var5) {
            this.ringPositions.put(l_var5, this.generateRingPositions((Holder<StructureSet>)l_param1, l_var5));
         }
      });
   }

   private CompletableFuture<List<ChunkPos>> generateRingPositions(Holder<StructureSet> param0, ConcentricRingsStructurePlacement param1) {
      if (param1.count() == 0) {
         return CompletableFuture.completedFuture(List.of());
      }

      Stopwatch var3 = Stopwatch.createStarted(Util.TICKER);
      int var4 = param1.distance();
      int var5 = param1.count();
      List<CompletableFuture<ChunkPos>> var6 = new ArrayList<>(var5);
      int var7 = param1.spread();
      HolderSet<Biome> var8 = param1.preferredBiomes();
      RandomSource var9 = RandomSource.create();
      var9.setSeed(this.concentricRingsSeed);
      double var10 = var9.nextDouble() * Math.PI * 2.0;
      int var12 = 0;
      int var13 = 0;

      for (int var14 = 0; var14 < var5; var14++) {
         double var15 = 4 * var4 + var4 * var13 * 6 + (var9.nextDouble() - 0.5) * (var4 * 2.5);
         int var17 = (int)Math.round(Math.cos(var10) * var15);
         int var18 = (int)Math.round(Math.sin(var10) * var15);
         RandomSource var19 = var9.fork();
         var6.add(
            CompletableFuture.supplyAsync(
               () -> {
                  Pair<BlockPos, Holder<Biome>> l_var5 = this.biomeSource
                     .findBiomeHorizontal(
                        SectionPos.sectionToBlockCoord(var17, 8),
                        0,
                        SectionPos.sectionToBlockCoord(var18, 8),
                        112,
                        var8::contains,
                        var19,
                        this.randomState.sampler()
                     );
                  if (l_var5 != null) {
                     BlockPos l_var6 = l_var5.getFirst();
                     return new ChunkPos(SectionPos.blockToSectionCoord(l_var6.getX()), SectionPos.blockToSectionCoord(l_var6.getZ()));
                  } else {
                     return new ChunkPos(var17, var18);
                  }
               },
               Util.backgroundExecutor()
            )
         );
         var10 += (Math.PI * 2) / var7;
         if (++var12 == var7) {
            var13++;
            var12 = 0;
            var7 += 2 * var7 / (var13 + 1);
            var7 = Math.min(var7, var5 - var14);
            var10 += var9.nextDouble() * Math.PI * 2.0;
         }
      }

      return Util.sequence(var6).thenApply(l_param2 -> {
         double l_var3 = var3.stop().elapsed(TimeUnit.MILLISECONDS) / 1000.0;
         LOGGER.debug("Calculation for {} took {}s", param0, l_var3);
         return l_param2;
      });
   }

   public void ensureStructuresGenerated() {
      if (!this.hasGeneratedPositions) {
         this.generatePositions();
         this.hasGeneratedPositions = true;
      }
   }

   @Nullable
   public List<ChunkPos> getRingPositionsFor(ConcentricRingsStructurePlacement param0) {
      this.ensureStructuresGenerated();
      CompletableFuture<List<ChunkPos>> var2 = this.ringPositions.get(param0);
      return var2 != null ? var2.join() : null;
   }

   public List<StructurePlacement> getPlacementsForStructure(Holder<Structure> param0) {
      this.ensureStructuresGenerated();
      return this.placementsForStructure.getOrDefault(param0.value(), List.of());
   }

   public RandomState randomState() {
      return this.randomState;
   }

   public boolean hasStructureChunkInRange(Holder<StructureSet> param0, int param1, int param2, int param3) {
      StructurePlacement var5 = param0.value().placement();

      for (int var6 = param1 - param3; var6 <= param1 + param3; var6++) {
         for (int var7 = param2 - param3; var7 <= param2 + param3; var7++) {
            if (var5.isStructureChunk(this, var6, var7)) {
               return true;
            }
         }
      }

      return false;
   }

   public long getLevelSeed() {
      return this.levelSeed;
   }
}

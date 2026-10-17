package net.minecraft.world.level.levelgen.blending;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction8;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.data.worldgen.NoiseData;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.FluidState;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;

public class Blender {
   private static final Blender EMPTY = new Blender(new Long2ObjectOpenHashMap(), new Long2ObjectOpenHashMap()) {
      @Override
      public Blender.BlendingOutput blendOffsetAndFactor(int param0, int param1) {
         return new Blender.BlendingOutput(1.0, 0.0);
      }

      @Override
      public double blendDensity(DensityFunction.FunctionContext param0, double param1) {
         return param1;
      }

      @Override
      public BiomeResolver getBiomeResolver(BiomeResolver param0) {
         return param0;
      }
   };
   private static final NormalNoise SHIFT_NOISE = NormalNoise.create(new XoroshiroRandomSource(42L), NoiseData.DEFAULT_SHIFT);
   private static final int HEIGHT_BLENDING_RANGE_CELLS = QuartPos.fromSection(7) - 1;
   private static final int HEIGHT_BLENDING_RANGE_CHUNKS = QuartPos.toSection(HEIGHT_BLENDING_RANGE_CELLS + 3);
   private static final int DENSITY_BLENDING_RANGE_CELLS = 2;
   private static final int DENSITY_BLENDING_RANGE_CHUNKS = QuartPos.toSection(5);
   private static final double OLD_CHUNK_XZ_RADIUS = 8.0;
   private final Long2ObjectOpenHashMap<BlendingData> heightAndBiomeBlendingData;
   private final Long2ObjectOpenHashMap<BlendingData> densityBlendingData;

   public static Blender empty() {
      return EMPTY;
   }

   public static Blender of(@Nullable WorldGenRegion param0) {
      if (param0 == null) {
         return EMPTY;
      }

      ChunkPos var1 = param0.getCenter();
      if (!param0.isOldChunkAround(var1, HEIGHT_BLENDING_RANGE_CHUNKS)) {
         return EMPTY;
      }

      Long2ObjectOpenHashMap<BlendingData> var2 = new Long2ObjectOpenHashMap<>();
      Long2ObjectOpenHashMap<BlendingData> var3 = new Long2ObjectOpenHashMap<>();
      int var4 = Mth.square(HEIGHT_BLENDING_RANGE_CHUNKS + 1);

      for (int var5 = -HEIGHT_BLENDING_RANGE_CHUNKS; var5 <= HEIGHT_BLENDING_RANGE_CHUNKS; var5++) {
         for (int var6 = -HEIGHT_BLENDING_RANGE_CHUNKS; var6 <= HEIGHT_BLENDING_RANGE_CHUNKS; var6++) {
            if (var5 * var5 + var6 * var6 <= var4) {
               int var7 = var1.x + var5;
               int var8 = var1.z + var6;
               BlendingData var9 = BlendingData.getOrUpdateBlendingData(param0, var7, var8);
               if (var9 != null) {
                  var2.put(ChunkPos.asLong(var7, var8), var9);
                  if (var5 >= -DENSITY_BLENDING_RANGE_CHUNKS
                     && var5 <= DENSITY_BLENDING_RANGE_CHUNKS
                     && var6 >= -DENSITY_BLENDING_RANGE_CHUNKS
                     && var6 <= DENSITY_BLENDING_RANGE_CHUNKS) {
                     var3.put(ChunkPos.asLong(var7, var8), var9);
                  }
               }
            }
         }
      }

      return var2.isEmpty() && var3.isEmpty() ? EMPTY : new Blender(var2, var3);
   }

   Blender(Long2ObjectOpenHashMap<BlendingData> param0, Long2ObjectOpenHashMap<BlendingData> param1) {
      this.heightAndBiomeBlendingData = param0;
      this.densityBlendingData = param1;
   }

   public Blender.BlendingOutput blendOffsetAndFactor(int param0, int param1) {
      int var3 = QuartPos.fromBlock(param0);
      int var4 = QuartPos.fromBlock(param1);
      double var5 = this.getBlendingDataValue(var3, 0, var4, BlendingData::getHeight);
      if (var5 != Double.MAX_VALUE) {
         return new Blender.BlendingOutput(0.0, heightToOffset(var5));
      }

      MutableDouble var7 = new MutableDouble(0.0);
      MutableDouble var8 = new MutableDouble(0.0);
      MutableDouble var9 = new MutableDouble(Double.POSITIVE_INFINITY);
      this.heightAndBiomeBlendingData
         .forEach(
            (l_param5, l_param6) -> l_param6.iterateHeights(
               QuartPos.fromSection(ChunkPos.getX(l_param5)), QuartPos.fromSection(ChunkPos.getZ(l_param5)), (l2_param5, l2_param6, l2_param7) -> {
                  double l2_var9 = Mth.length(var3 - l2_param5, var4 - l2_param6);
                  if (!(l2_var9 > HEIGHT_BLENDING_RANGE_CELLS)) {
                     if (l2_var9 < var9.doubleValue()) {
                        var9.setValue(l2_var9);
                     }

                     double l2_var11 = 1.0 / (l2_var9 * l2_var9 * l2_var9 * l2_var9);
                     var8.add(l2_param7 * l2_var11);
                     var7.add(l2_var11);
                  }
               }
            )
         );
      if (var9.doubleValue() == Double.POSITIVE_INFINITY) {
         return new Blender.BlendingOutput(1.0, 0.0);
      }

      double var10 = var8.doubleValue() / var7.doubleValue();
      double var12 = Mth.clamp(var9.doubleValue() / (HEIGHT_BLENDING_RANGE_CELLS + 1), 0.0, 1.0);
      var12 = 3.0 * var12 * var12 - 2.0 * var12 * var12 * var12;
      return new Blender.BlendingOutput(var12, heightToOffset(var10));
   }

   private static double heightToOffset(double param0) {
      double var2 = 1.0;
      double var4 = param0 + 0.5;
      double var6 = Mth.positiveModulo(var4, 8.0);
      return 1.0 * (32.0 * (var4 - 128.0) - 3.0 * (var4 - 120.0) * var6 + 3.0 * var6 * var6) / (128.0 * (32.0 - 3.0 * var6));
   }

   public double blendDensity(DensityFunction.FunctionContext param0, double param1) {
      int var4 = QuartPos.fromBlock(param0.blockX());
      int var5 = param0.blockY() / 8;
      int var6 = QuartPos.fromBlock(param0.blockZ());
      double var7 = this.getBlendingDataValue(var4, var5, var6, BlendingData::getDensity);
      if (var7 != Double.MAX_VALUE) {
         return var7;
      }

      MutableDouble var9 = new MutableDouble(0.0);
      MutableDouble var10 = new MutableDouble(0.0);
      MutableDouble var11 = new MutableDouble(Double.POSITIVE_INFINITY);
      this.densityBlendingData
         .forEach(
            (l_param6, l_param7) -> l_param7.iterateDensities(
               QuartPos.fromSection(ChunkPos.getX(l_param6)),
               QuartPos.fromSection(ChunkPos.getZ(l_param6)),
               var5 - 1,
               var5 + 1,
               (l2_param6, l2_param7, l2_param8, l2_param9) -> {
                  double l2_var11 = Mth.length(var4 - l2_param6, (var5 - l2_param7) * 2, var6 - l2_param8);
                  if (!(l2_var11 > 2.0)) {
                     if (l2_var11 < var11.doubleValue()) {
                        var11.setValue(l2_var11);
                     }

                     double l2_var13 = 1.0 / (l2_var11 * l2_var11 * l2_var11 * l2_var11);
                     var10.add(l2_param9 * l2_var13);
                     var9.add(l2_var13);
                  }
               }
            )
         );
      if (var11.doubleValue() == Double.POSITIVE_INFINITY) {
         return param1;
      }

      double var12 = var10.doubleValue() / var9.doubleValue();
      double var14 = Mth.clamp(var11.doubleValue() / 3.0, 0.0, 1.0);
      return Mth.lerp(var14, var12, param1);
   }

   private double getBlendingDataValue(int param0, int param1, int param2, Blender.CellValueGetter param3) {
      int var5 = QuartPos.toSection(param0);
      int var6 = QuartPos.toSection(param2);
      boolean var7 = (param0 & 3) == 0;
      boolean var8 = (param2 & 3) == 0;
      double var9 = this.getBlendingDataValue(param3, var5, var6, param0, param1, param2);
      if (var9 == Double.MAX_VALUE) {
         if (var7 && var8) {
            var9 = this.getBlendingDataValue(param3, var5 - 1, var6 - 1, param0, param1, param2);
         }

         if (var9 == Double.MAX_VALUE) {
            if (var7) {
               var9 = this.getBlendingDataValue(param3, var5 - 1, var6, param0, param1, param2);
            }

            if (var9 == Double.MAX_VALUE && var8) {
               var9 = this.getBlendingDataValue(param3, var5, var6 - 1, param0, param1, param2);
            }
         }
      }

      return var9;
   }

   private double getBlendingDataValue(Blender.CellValueGetter param0, int param1, int param2, int param3, int param4, int param5) {
      BlendingData var7 = this.heightAndBiomeBlendingData.get(ChunkPos.asLong(param1, param2));
      return var7 != null ? param0.get(var7, param3 - QuartPos.fromSection(param1), param4, param5 - QuartPos.fromSection(param2)) : Double.MAX_VALUE;
   }

   public BiomeResolver getBiomeResolver(BiomeResolver param0) {
      return (l_param1, l_param2, l_param3, l_param4) -> {
         Holder<Biome> l_var6 = this.blendBiome(l_param1, l_param2, l_param3);
         return l_var6 == null ? param0.getNoiseBiome(l_param1, l_param2, l_param3, l_param4) : l_var6;
      };
   }

   @Nullable
   private Holder<Biome> blendBiome(int param0, int param1, int param2) {
      MutableDouble var4 = new MutableDouble(Double.POSITIVE_INFINITY);
      MutableObject<Holder<Biome>> var5 = new MutableObject<>();
      this.heightAndBiomeBlendingData
         .forEach(
            (l_param5, l_param6) -> l_param6.iterateBiomes(
               QuartPos.fromSection(ChunkPos.getX(l_param5)), param1, QuartPos.fromSection(ChunkPos.getZ(l_param5)), (l2_param4, l2_param5, l2_param6) -> {
                  double l2_var7 = Mth.length(param0 - l2_param4, param2 - l2_param5);
                  if (!(l2_var7 > HEIGHT_BLENDING_RANGE_CELLS)) {
                     if (l2_var7 < var4.doubleValue()) {
                        var5.setValue(l2_param6);
                        var4.setValue(l2_var7);
                     }
                  }
               }
            )
         );
      if (var4.doubleValue() == Double.POSITIVE_INFINITY) {
         return null;
      }

      double var6 = SHIFT_NOISE.getValue(param0, 0.0, param2) * 12.0;
      double var8 = Mth.clamp((var4.doubleValue() + var6) / (HEIGHT_BLENDING_RANGE_CELLS + 1), 0.0, 1.0);
      return var8 > 0.5 ? null : var5.getValue();
   }

   public static void generateBorderTicks(WorldGenRegion param0, ChunkAccess param1) {
      ChunkPos var2 = param1.getPos();
      boolean var3 = param1.isOldNoiseGeneration();
      BlockPos.MutableBlockPos var4 = new BlockPos.MutableBlockPos();
      BlockPos var5 = new BlockPos(var2.getMinBlockX(), 0, var2.getMinBlockZ());
      BlendingData var6 = param1.getBlendingData();
      if (var6 != null) {
         int var7 = var6.getAreaWithOldGeneration().getMinBuildHeight();
         int var8 = var6.getAreaWithOldGeneration().getMaxBuildHeight() - 1;
         if (var3) {
            for (int var9 = 0; var9 < 16; var9++) {
               for (int var10 = 0; var10 < 16; var10++) {
                  generateBorderTick(param1, var4.setWithOffset(var5, var9, var7 - 1, var10));
                  generateBorderTick(param1, var4.setWithOffset(var5, var9, var7, var10));
                  generateBorderTick(param1, var4.setWithOffset(var5, var9, var8, var10));
                  generateBorderTick(param1, var4.setWithOffset(var5, var9, var8 + 1, var10));
               }
            }
         }

         for (Direction var10 : Direction.Plane.HORIZONTAL) {
            if (param0.getChunk(var2.x + var10.getStepX(), var2.z + var10.getStepZ()).isOldNoiseGeneration() != var3) {
               int var11 = var10 == Direction.EAST ? 15 : 0;
               int var12 = var10 == Direction.WEST ? 0 : 15;
               int var13 = var10 == Direction.SOUTH ? 15 : 0;
               int var14 = var10 == Direction.NORTH ? 0 : 15;

               for (int var15 = var11; var15 <= var12; var15++) {
                  for (int var16 = var13; var16 <= var14; var16++) {
                     int var17 = Math.min(var8, param1.getHeight(Heightmap.Types.MOTION_BLOCKING, var15, var16)) + 1;

                     for (int var18 = var7; var18 < var17; var18++) {
                        generateBorderTick(param1, var4.setWithOffset(var5, var15, var18, var16));
                     }
                  }
               }
            }
         }
      }
   }

   private static void generateBorderTick(ChunkAccess param0, BlockPos param1) {
      BlockState var2 = param0.getBlockState(param1);
      if (var2.is(BlockTags.LEAVES)) {
         param0.markPosForPostprocessing(param1);
      }

      FluidState var3 = param0.getFluidState(param1);
      if (!var3.isEmpty()) {
         param0.markPosForPostprocessing(param1);
      }
   }

   public static void addAroundOldChunksCarvingMaskFilter(WorldGenLevel param0, ProtoChunk param1) {
      ChunkPos var2 = param1.getPos();
      Builder<Direction8, BlendingData> var3 = ImmutableMap.builder();

      for (Direction8 var7 : Direction8.values()) {
         int var8 = var2.x + var7.getStepX();
         int var9 = var2.z + var7.getStepZ();
         BlendingData var10 = param0.getChunk(var8, var9).getBlendingData();
         if (var10 != null) {
            var3.put(var7, var10);
         }
      }

      ImmutableMap<Direction8, BlendingData> var4 = var3.build();
      if (param1.isOldNoiseGeneration() || !var4.isEmpty()) {
         Blender.DistanceGetter var5 = makeOldChunkDistanceGetter(param1.getBlendingData(), var4);
         CarvingMask.Mask var6 = (l_param1, l_param2, l_param3) -> {
            double l_var4 = l_param1 + 0.5 + SHIFT_NOISE.getValue(l_param1, l_param2, l_param3) * 4.0;
            double l_var6 = l_param2 + 0.5 + SHIFT_NOISE.getValue(l_param2, l_param3, l_param1) * 4.0;
            double l_var8 = l_param3 + 0.5 + SHIFT_NOISE.getValue(l_param3, l_param1, l_param2) * 4.0;
            return var5.getDistance(l_var4, l_var6, l_var8) < 4.0;
         };
         Stream.of(GenerationStep.Carving.values()).map(param1::getOrCreateCarvingMask).forEach(l_param1 -> l_param1.setAdditionalMask(var6));
      }
   }

   public static Blender.DistanceGetter makeOldChunkDistanceGetter(@Nullable BlendingData param0, Map<Direction8, BlendingData> param1) {
      List<Blender.DistanceGetter> var2 = Lists.newArrayList();
      if (param0 != null) {
         var2.add(makeOffsetOldChunkDistanceGetter(null, param0));
      }

      param1.forEach((l_param1, l_param2) -> var2.add(makeOffsetOldChunkDistanceGetter(l_param1, l_param2)));
      return (l_param1, l_param3, l_param5) -> {
         double l_var7 = Double.POSITIVE_INFINITY;

         for (Blender.DistanceGetter l_var10 : var2) {
            double l_var11 = l_var10.getDistance(l_param1, l_param3, l_param5);
            if (l_var11 < l_var7) {
               l_var7 = l_var11;
            }
         }

         return l_var7;
      };
   }

   private static Blender.DistanceGetter makeOffsetOldChunkDistanceGetter(@Nullable Direction8 param0, BlendingData param1) {
      double var2 = 0.0;
      double var4 = 0.0;
      if (param0 != null) {
         for (Direction var7 : param0.getDirections()) {
            var2 += var7.getStepX() * 16;
            var4 += var7.getStepZ() * 16;
         }
      }

      double var6 = var2;
      double var8 = var4;
      double var10 = param1.getAreaWithOldGeneration().getHeight() / 2.0;
      double var12 = param1.getAreaWithOldGeneration().getMinBuildHeight() + var10;
      return (l_param8, l_param10, l_param12) -> distanceToCube(l_param8 - 8.0 - var6, l_param10 - var12, l_param12 - 8.0 - var8, 8.0, var10, 8.0);
   }

   private static double distanceToCube(double param0, double param2, double param4, double param6, double param8, double param10) {
      double var12 = Math.abs(param0) - param6;
      double var14 = Math.abs(param2) - param8;
      double var16 = Math.abs(param4) - param10;
      return Mth.length(Math.max(0.0, var12), Math.max(0.0, var14), Math.max(0.0, var16));
   }

   public record BlendingOutput(double alpha, double blendingOffset) {
   }

   interface CellValueGetter {
      double get(BlendingData var1, int var2, int var3, int var4);
   }

   public interface DistanceGetter {
      double getDistance(double var1, double var3, double var5);
   }
}

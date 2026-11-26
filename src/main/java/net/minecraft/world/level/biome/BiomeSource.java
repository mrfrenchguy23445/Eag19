package net.minecraft.world.level.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;

public abstract class BiomeSource implements BiomeResolver {
   public static final Codec<BiomeSource> CODEC = BuiltInRegistries.BIOME_SOURCE.byNameCodec().dispatchStable(BiomeSource::codec, Function.identity());
   private final Supplier<Set<Holder<Biome>>> possibleBiomes = Suppliers.<Set<Holder<Biome>>>memoize(
      () -> this.collectPossibleBiomes().distinct().collect(ImmutableSet.toImmutableSet())
   );

   protected BiomeSource() {
   }

   protected abstract Codec<? extends BiomeSource> codec();

   protected abstract Stream<Holder<Biome>> collectPossibleBiomes();

   public Set<Holder<Biome>> possibleBiomes() {
      return this.possibleBiomes.get();
   }

   public Set<Holder<Biome>> getBiomesWithin(int param0, int param1, int param2, int param3, Climate.Sampler param4) {
      int var6 = QuartPos.fromBlock(param0 - param3);
      int var7 = QuartPos.fromBlock(param1 - param3);
      int var8 = QuartPos.fromBlock(param2 - param3);
      int var9 = QuartPos.fromBlock(param0 + param3);
      int var10 = QuartPos.fromBlock(param1 + param3);
      int var11 = QuartPos.fromBlock(param2 + param3);
      int var12 = var9 - var6 + 1;
      int var13 = var10 - var7 + 1;
      int var14 = var11 - var8 + 1;
      Set<Holder<Biome>> var15 = Sets.newHashSet();

      for (int var16 = 0; var16 < var14; var16++) {
         for (int var17 = 0; var17 < var12; var17++) {
            for (int var18 = 0; var18 < var13; var18++) {
               int var19 = var6 + var17;
               int var20 = var7 + var18;
               int var21 = var8 + var16;
               var15.add(this.getNoiseBiome(var19, var20, var21, param4));
            }
         }
      }

      return var15;
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(
      int param0, int param1, int param2, int param3, Predicate<Holder<Biome>> param4, RandomSource param5, Climate.Sampler param6
   ) {
      return this.findBiomeHorizontal(param0, param1, param2, param3, 1, param4, param5, false, param6);
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findClosestBiome3d(
      BlockPos param0, int param1, int param2, int param3, Predicate<Holder<Biome>> param4, Climate.Sampler param5, LevelReader param6
   ) {
      Set<Holder<Biome>> var8 = this.possibleBiomes().stream().filter(param4).collect(Collectors.toUnmodifiableSet());
      if (var8.isEmpty()) {
         return null;
      }

      int var9 = Math.floorDiv(param1, param2);
      int[] var10 = Mth.outFromOrigin(param0.getY(), param6.getMinBuildHeight() + 1, param6.getMaxBuildHeight(), param3).toArray();

      for (BlockPos.MutableBlockPos var12 : BlockPos.spiralAround(BlockPos.ZERO, var9, Direction.EAST, Direction.SOUTH)) {
         int var13 = param0.getX() + var12.getX() * param2;
         int var14 = param0.getZ() + var12.getZ() * param2;
         int var15 = QuartPos.fromBlock(var13);
         int var16 = QuartPos.fromBlock(var14);

         for (int var20 : var10) {
            int var21 = QuartPos.fromBlock(var20);
            Holder<Biome> var22 = this.getNoiseBiome(var15, var21, var16, param5);
            if (var8.contains(var22)) {
               return Pair.of(new BlockPos(var13, var20, var14), var22);
            }
         }
      }

      return null;
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(
      int param0, int param1, int param2, int param3, int param4, Predicate<Holder<Biome>> param5, RandomSource param6, boolean param7, Climate.Sampler param8
   ) {
      int var10 = QuartPos.fromBlock(param0);
      int var11 = QuartPos.fromBlock(param2);
      int var12 = QuartPos.fromBlock(param3);
      int var13 = QuartPos.fromBlock(param1);
      Pair<BlockPos, Holder<Biome>> var14 = null;
      int var15 = 0;
      int var16 = param7 ? 0 : var12;
      int var17 = var16;

      while (var17 <= var12) {
         for (int var18 = SharedConstants.debugGenerateSquareTerrainWithoutNoise ? 0 : -var17; var18 <= var17; var18 += param4) {
            boolean var19 = Math.abs(var18) == var17;

            for (int var20 = -var17; var20 <= var17; var20 += param4) {
               if (param7) {
                  boolean var21 = Math.abs(var20) == var17;
                  if (!var21 && !var19) {
                     continue;
                  }
               }

               int var21 = var10 + var20;
               int var22 = var11 + var18;
               Holder<Biome> var23 = this.getNoiseBiome(var21, var13, var22, param8);
               if (param5.test(var23)) {
                  if (var14 == null || param6.nextInt(var15 + 1) == 0) {
                     BlockPos var24 = new BlockPos(QuartPos.toBlock(var21), param1, QuartPos.toBlock(var22));
                     if (param7) {
                        return Pair.of(var24, var23);
                     }

                     var14 = Pair.of(var24, var23);
                  }

                  var15++;
               }
            }
         }

         var17 += param4;
      }

      return var14;
   }

   @Override
   public abstract Holder<Biome> getNoiseBiome(int var1, int var2, int var3, Climate.Sampler var4);

   public void addDebugInfo(List<String> param0, BlockPos param1, Climate.Sampler param2) {
   }
}

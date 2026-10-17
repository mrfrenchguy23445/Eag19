package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.FluidState;

public class GeodeFeature extends Feature<GeodeConfiguration> {
   private static final Direction[] DIRECTIONS = Direction.values();

   public GeodeFeature(Codec<GeodeConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<GeodeConfiguration> param0) {
      GeodeConfiguration var2 = param0.config();
      RandomSource var3 = param0.random();
      BlockPos var4 = param0.origin();
      WorldGenLevel var5 = param0.level();
      int var6 = var2.minGenOffset;
      int var7 = var2.maxGenOffset;
      List<Pair<BlockPos, Integer>> var8 = Lists.newLinkedList();
      int var9 = var2.distributionPoints.sample(var3);
      WorldgenRandom var10 = new WorldgenRandom(new LegacyRandomSource(var5.getSeed()));
      NormalNoise var11 = NormalNoise.create(var10, -4, 1.0);
      List<BlockPos> var12 = Lists.newLinkedList();
      double var13 = (double)var9 / var2.outerWallDistance.getMaxValue();
      GeodeLayerSettings var15 = var2.geodeLayerSettings;
      GeodeBlockSettings var16 = var2.geodeBlockSettings;
      GeodeCrackSettings var17 = var2.geodeCrackSettings;
      double var18 = 1.0 / Math.sqrt(var15.filling);
      double var20 = 1.0 / Math.sqrt(var15.innerLayer + var13);
      double var22 = 1.0 / Math.sqrt(var15.middleLayer + var13);
      double var24 = 1.0 / Math.sqrt(var15.outerLayer + var13);
      double var26 = 1.0 / Math.sqrt(var17.baseCrackSize + var3.nextDouble() / 2.0 + (var9 > 3 ? var13 : 0.0));
      boolean var28 = var3.nextFloat() < var17.generateCrackChance;
      int var29 = 0;

      for (int var30 = 0; var30 < var9; var30++) {
         int var31 = var2.outerWallDistance.sample(var3);
         int var32 = var2.outerWallDistance.sample(var3);
         int var33 = var2.outerWallDistance.sample(var3);
         BlockPos var34 = var4.offset(var31, var32, var33);
         BlockState var35 = var5.getBlockState(var34);
         if (var35.isAir() || var35.is(BlockTags.GEODE_INVALID_BLOCKS)) {
            if (++var29 > var2.invalidBlocksThreshold) {
               return false;
            }
         }

         var8.add(Pair.of(var34, var2.pointOffset.sample(var3)));
      }

      if (var28) {
         int var30 = var3.nextInt(4);
         int var31 = var9 * 2 + 1;
         if (var30 == 0) {
            var12.add(var4.offset(var31, 7, 0));
            var12.add(var4.offset(var31, 5, 0));
            var12.add(var4.offset(var31, 1, 0));
         } else if (var30 == 1) {
            var12.add(var4.offset(0, 7, var31));
            var12.add(var4.offset(0, 5, var31));
            var12.add(var4.offset(0, 1, var31));
         } else if (var30 == 2) {
            var12.add(var4.offset(var31, 7, var31));
            var12.add(var4.offset(var31, 5, var31));
            var12.add(var4.offset(var31, 1, var31));
         } else {
            var12.add(var4.offset(0, 7, 0));
            var12.add(var4.offset(0, 5, 0));
            var12.add(var4.offset(0, 1, 0));
         }
      }

      List<BlockPos> var30 = Lists.newArrayList();
      Predicate<BlockState> var31 = isReplaceable(var2.geodeBlockSettings.cannotReplace);

      for (BlockPos var33 : BlockPos.betweenClosed(var4.offset(var6, var6, var6), var4.offset(var7, var7, var7))) {
         double var34 = var11.getValue(var33.getX(), var33.getY(), var33.getZ()) * var2.noiseMultiplier;
         double var36 = 0.0;
         double var38 = 0.0;

         for (Pair<BlockPos, Integer> var41 : var8) {
            var36 += Mth.invSqrt(var33.distSqr(var41.getFirst()) + var41.getSecond().intValue()) + var34;
         }

         for (BlockPos var41 : var12) {
            var38 += Mth.invSqrt(var33.distSqr(var41) + var17.crackPointOffset) + var34;
         }

         if (!(var36 < var24)) {
            if (var28 && var38 >= var26 && var36 < var18) {
               this.safeSetBlock(var5, var33, Blocks.AIR.defaultBlockState(), var31);

               for (Direction var43 : DIRECTIONS) {
                  BlockPos var44 = var33.relative(var43);
                  FluidState var45 = var5.getFluidState(var44);
                  if (!var45.isEmpty()) {
                     var5.scheduleTick(var44, var45.getType(), 0);
                  }
               }
            } else if (var36 >= var18) {
               this.safeSetBlock(var5, var33, var16.fillingProvider.getState(var3, var33), var31);
            } else if (var36 >= var20) {
               boolean var40 = var3.nextFloat() < var2.useAlternateLayer0Chance;
               if (var40) {
                  this.safeSetBlock(var5, var33, var16.alternateInnerLayerProvider.getState(var3, var33), var31);
               } else {
                  this.safeSetBlock(var5, var33, var16.innerLayerProvider.getState(var3, var33), var31);
               }

               if ((!var2.placementsRequireLayer0Alternate || var40) && var3.nextFloat() < var2.usePotentialPlacementsChance) {
                  var30.add(var33.immutable());
               }
            } else if (var36 >= var22) {
               this.safeSetBlock(var5, var33, var16.middleLayerProvider.getState(var3, var33), var31);
            } else if (var36 >= var24) {
               this.safeSetBlock(var5, var33, var16.outerLayerProvider.getState(var3, var33), var31);
            }
         }
      }

      List<BlockState> var32 = var16.innerPlacements;

      for (BlockPos var34 : var30) {
         BlockState var35 = Util.getRandom(var32, var3);

         for (Direction var39 : DIRECTIONS) {
            if (var35.hasProperty(BlockStateProperties.FACING)) {
               var35 = var35.setValue(BlockStateProperties.FACING, var39);
            }

            BlockPos var40 = var34.relative(var39);
            BlockState var41 = var5.getBlockState(var40);
            if (var35.hasProperty(BlockStateProperties.WATERLOGGED)) {
               var35 = var35.setValue(BlockStateProperties.WATERLOGGED, var41.getFluidState().isSource());
            }

            if (BuddingAmethystBlock.canClusterGrowAtState(var41)) {
               this.safeSetBlock(var5, var40, var35, var31);
               break;
            }
         }
      }

      return true;
   }
}

package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.Material;

@Deprecated
public class LakeFeature extends Feature<LakeFeature.Configuration> {
   private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

   public LakeFeature(Codec<LakeFeature.Configuration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<LakeFeature.Configuration> param0) {
      BlockPos var2 = param0.origin();
      WorldGenLevel var3 = param0.level();
      RandomSource var4 = param0.random();
      LakeFeature.Configuration var5 = param0.config();
      if (var2.getY() <= var3.getMinBuildHeight() + 4) {
         return false;
      }

      var2 = var2.below(4);
      boolean[] var6 = new boolean[2048];
      int var7 = var4.nextInt(4) + 4;

      for (int var8 = 0; var8 < var7; var8++) {
         double var9 = var4.nextDouble() * 6.0 + 3.0;
         double var11 = var4.nextDouble() * 4.0 + 2.0;
         double var13 = var4.nextDouble() * 6.0 + 3.0;
         double var15 = var4.nextDouble() * (16.0 - var9 - 2.0) + 1.0 + var9 / 2.0;
         double var17 = var4.nextDouble() * (8.0 - var11 - 4.0) + 2.0 + var11 / 2.0;
         double var19 = var4.nextDouble() * (16.0 - var13 - 2.0) + 1.0 + var13 / 2.0;

         for (int var21 = 1; var21 < 15; var21++) {
            for (int var22 = 1; var22 < 15; var22++) {
               for (int var23 = 1; var23 < 7; var23++) {
                  double var24 = (var21 - var15) / (var9 / 2.0);
                  double var26 = (var23 - var17) / (var11 / 2.0);
                  double var28 = (var22 - var19) / (var13 / 2.0);
                  double var30 = var24 * var24 + var26 * var26 + var28 * var28;
                  if (var30 < 1.0) {
                     var6[(var21 * 16 + var22) * 8 + var23] = true;
                  }
               }
            }
         }
      }

      BlockState var8 = var5.fluid().getState(var4, var2);

      for (int var9 = 0; var9 < 16; var9++) {
         for (int var10 = 0; var10 < 16; var10++) {
            for (int var11 = 0; var11 < 8; var11++) {
               boolean var12 = !var6[(var9 * 16 + var10) * 8 + var11]
                  && (
                     var9 < 15 && var6[((var9 + 1) * 16 + var10) * 8 + var11]
                        || var9 > 0 && var6[((var9 - 1) * 16 + var10) * 8 + var11]
                        || var10 < 15 && var6[(var9 * 16 + var10 + 1) * 8 + var11]
                        || var10 > 0 && var6[(var9 * 16 + (var10 - 1)) * 8 + var11]
                        || var11 < 7 && var6[(var9 * 16 + var10) * 8 + var11 + 1]
                        || var11 > 0 && var6[(var9 * 16 + var10) * 8 + (var11 - 1)]
                  );
               if (var12) {
                  Material var13 = var3.getBlockState(var2.offset(var9, var11, var10)).getMaterial();
                  if (var11 >= 4 && var13.isLiquid()) {
                     return false;
                  }

                  if (var11 < 4 && !var13.isSolid() && var3.getBlockState(var2.offset(var9, var11, var10)) != var8) {
                     return false;
                  }
               }
            }
         }
      }

      for (int var9 = 0; var9 < 16; var9++) {
         for (int var10 = 0; var10 < 16; var10++) {
            for (int var11 = 0; var11 < 8; var11++) {
               if (var6[(var9 * 16 + var10) * 8 + var11]) {
                  BlockPos var12 = var2.offset(var9, var11, var10);
                  if (this.canReplaceBlock(var3.getBlockState(var12))) {
                     boolean var13 = var11 >= 4;
                     var3.setBlock(var12, var13 ? AIR : var8, 2);
                     if (var13) {
                        var3.scheduleTick(var12, AIR.getBlock(), 0);
                        this.markAboveForPostProcessing(var3, var12);
                     }
                  }
               }
            }
         }
      }

      BlockState var9 = var5.barrier().getState(var4, var2);
      if (!var9.isAir()) {
         for (int var10 = 0; var10 < 16; var10++) {
            for (int var11 = 0; var11 < 16; var11++) {
               for (int var12 = 0; var12 < 8; var12++) {
                  boolean var13 = !var6[(var10 * 16 + var11) * 8 + var12]
                     && (
                        var10 < 15 && var6[((var10 + 1) * 16 + var11) * 8 + var12]
                           || var10 > 0 && var6[((var10 - 1) * 16 + var11) * 8 + var12]
                           || var11 < 15 && var6[(var10 * 16 + var11 + 1) * 8 + var12]
                           || var11 > 0 && var6[(var10 * 16 + (var11 - 1)) * 8 + var12]
                           || var12 < 7 && var6[(var10 * 16 + var11) * 8 + var12 + 1]
                           || var12 > 0 && var6[(var10 * 16 + var11) * 8 + (var12 - 1)]
                     );
                  if (var13 && (var12 < 4 || var4.nextInt(2) != 0)) {
                     BlockState var14 = var3.getBlockState(var2.offset(var10, var12, var11));
                     if (var14.getMaterial().isSolid() && !var14.is(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE)) {
                        BlockPos var15 = var2.offset(var10, var12, var11);
                        var3.setBlock(var15, var9, 2);
                        this.markAboveForPostProcessing(var3, var15);
                     }
                  }
               }
            }
         }
      }

      if (var8.getFluidState().is(FluidTags.WATER)) {
         for (int var10 = 0; var10 < 16; var10++) {
            for (int var11 = 0; var11 < 16; var11++) {
               int var12 = 4;
               BlockPos var13 = var2.offset(var10, 4, var11);
               if (var3.getBiome(var13).value().shouldFreeze(var3, var13, false) && this.canReplaceBlock(var3.getBlockState(var13))) {
                  var3.setBlock(var13, Blocks.ICE.defaultBlockState(), 2);
               }
            }
         }
      }

      return true;
   }

   private boolean canReplaceBlock(BlockState param0) {
      return !param0.is(BlockTags.FEATURES_CANNOT_REPLACE);
   }

   public record Configuration(BlockStateProvider fluid, BlockStateProvider barrier) implements FeatureConfiguration {
      public static final Codec<LakeFeature.Configuration> CODEC = RecordCodecBuilder.create(
         param0 -> param0.group(
               BlockStateProvider.CODEC.fieldOf("fluid").forGetter(LakeFeature.Configuration::fluid),
               BlockStateProvider.CODEC.fieldOf("barrier").forGetter(LakeFeature.Configuration::barrier)
            )
            .apply(param0, LakeFeature.Configuration::new)
      );
   }
}

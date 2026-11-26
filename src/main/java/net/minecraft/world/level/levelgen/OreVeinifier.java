package net.minecraft.world.level.levelgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class OreVeinifier {
   private static final float VEININESS_THRESHOLD = 0.4F;
   private static final int EDGE_ROUNDOFF_BEGIN = 20;
   private static final double MAX_EDGE_ROUNDOFF = 0.2;
   private static final float VEIN_SOLIDNESS = 0.7F;
   private static final float MIN_RICHNESS = 0.1F;
   private static final float MAX_RICHNESS = 0.3F;
   private static final float MAX_RICHNESS_THRESHOLD = 0.6F;
   private static final float CHANCE_OF_RAW_ORE_BLOCK = 0.02F;
   private static final float SKIP_ORE_IF_GAP_NOISE_IS_BELOW = -0.3F;

   private OreVeinifier() {
   }

   protected static NoiseChunk.BlockStateFiller create(DensityFunction param0, DensityFunction param1, DensityFunction param2, PositionalRandomFactory param3) {
      BlockState var4 = null;
      return l_param5 -> {
         double l_var6 = param0.compute(l_param5);
         int l_var8 = l_param5.blockY();
         OreVeinifier.VeinType l_var9 = l_var6 > 0.0 ? OreVeinifier.VeinType.COPPER : OreVeinifier.VeinType.IRON;
         double l_var10 = Math.abs(l_var6);
         int l_var12 = l_var9.maxY - l_var8;
         int l_var13 = l_var8 - l_var9.minY;
         if (l_var13 >= 0 && l_var12 >= 0) {
            int l_var14 = Math.min(l_var12, l_var13);
            double l_var15 = Mth.clampedMap(l_var14, 0.0, 20.0, -0.2, 0.0);
            if (l_var10 + l_var15 < 0.4F) {
               return var4;
            } else {
               RandomSource l_var17 = param3.at(l_param5.blockX(), l_var8, l_param5.blockZ());
               if (l_var17.nextFloat() > 0.7F) {
                  return var4;
               } else if (param1.compute(l_param5) >= 0.0) {
                  return var4;
               } else {
                  double l_var18 = Mth.clampedMap(l_var10, 0.4F, 0.6F, 0.1F, 0.3F);
                  if (l_var17.nextFloat() < l_var18 && param2.compute(l_param5) > -0.3F) {
                     return l_var17.nextFloat() < 0.02F ? l_var9.rawOreBlock : l_var9.ore;
                  } else {
                     return l_var9.filler;
                  }
               }
            }
         } else {
            return var4;
         }
      };
   }

   protected enum VeinType {
      COPPER(Blocks.COPPER_ORE.defaultBlockState(), Blocks.RAW_COPPER_BLOCK.defaultBlockState(), Blocks.GRANITE.defaultBlockState(), 0, 50),
      IRON(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(), Blocks.RAW_IRON_BLOCK.defaultBlockState(), Blocks.TUFF.defaultBlockState(), -60, -8);

      final BlockState ore;
      final BlockState rawOreBlock;
      final BlockState filler;
      protected final int minY;
      protected final int maxY;

      VeinType(BlockState param2, BlockState param3, BlockState param4, int param5, int param6) {
         this.ore = param2;
         this.rawOreBlock = param3;
         this.filler = param4;
         this.minY = param5;
         this.maxY = param6;
      }
   }
}

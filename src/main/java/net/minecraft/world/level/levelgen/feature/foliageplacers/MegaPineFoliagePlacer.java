package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class MegaPineFoliagePlacer extends FoliagePlacer {
   public static final Codec<MegaPineFoliagePlacer> CODEC = RecordCodecBuilder.create(
      param0 -> foliagePlacerParts(param0)
         .and(IntProvider.codec(0, 24).fieldOf("crown_height").forGetter(l_param0 -> l_param0.crownHeight))
         .apply(param0, MegaPineFoliagePlacer::new)
   );
   private final IntProvider crownHeight;

   public MegaPineFoliagePlacer(IntProvider param0, IntProvider param1, IntProvider param2) {
      super(param0, param1);
      this.crownHeight = param2;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.MEGA_PINE_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      LevelSimulatedReader param0,
      FoliagePlacer.FoliageSetter param1,
      RandomSource param2,
      TreeConfiguration param3,
      int param4,
      FoliagePlacer.FoliageAttachment param5,
      int param6,
      int param7,
      int param8
   ) {
      BlockPos var10 = param5.pos();
      int var11 = 0;

      for (int var12 = var10.getY() - param6 + param8; var12 <= var10.getY() + param8; var12++) {
         int var13 = var10.getY() - var12;
         int var14 = param7 + param5.radiusOffset() + Mth.floor((float)var13 / param6 * 3.5F);
         int var15;
         if (var13 > 0 && var14 == var11 && (var12 & 1) == 0) {
            var15 = var14 + 1;
         } else {
            var15 = var14;
         }

         this.placeLeavesRow(param0, param1, param2, param3, new BlockPos(var10.getX(), var12, var10.getZ()), var15, 0, param5.doubleTrunk());
         var11 = var14;
      }
   }

   @Override
   public int foliageHeight(RandomSource param0, int param1, TreeConfiguration param2) {
      return this.crownHeight.sample(param0);
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource param0, int param1, int param2, int param3, int param4, boolean param5) {
      return param1 + param3 >= 7 ? true : param1 * param1 + param3 * param3 > param4 * param4;
   }
}

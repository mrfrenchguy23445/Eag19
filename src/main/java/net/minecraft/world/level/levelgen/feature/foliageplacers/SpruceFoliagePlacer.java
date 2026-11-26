package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class SpruceFoliagePlacer extends FoliagePlacer {
   public static final Codec<SpruceFoliagePlacer> CODEC = RecordCodecBuilder.create(
      param0 -> foliagePlacerParts(param0)
         .and(IntProvider.codec(0, 24).fieldOf("trunk_height").forGetter(l_param0 -> l_param0.trunkHeight))
         .apply(param0, SpruceFoliagePlacer::new)
   );
   private final IntProvider trunkHeight;

   public SpruceFoliagePlacer(IntProvider param0, IntProvider param1, IntProvider param2) {
      super(param0, param1);
      this.trunkHeight = param2;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.SPRUCE_FOLIAGE_PLACER;
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
      int var11 = param2.nextInt(2);
      int var12 = 1;
      int var13 = 0;

      for (int var14 = param8; var14 >= -param6; var14--) {
         this.placeLeavesRow(param0, param1, param2, param3, var10, var11, var14, param5.doubleTrunk());
         if (var11 >= var12) {
            var11 = var13;
            var13 = 1;
            var12 = Math.min(var12 + 1, param7 + param5.radiusOffset());
         } else {
            var11++;
         }
      }
   }

   @Override
   public int foliageHeight(RandomSource param0, int param1, TreeConfiguration param2) {
      return Math.max(4, param1 - this.trunkHeight.sample(param0));
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource param0, int param1, int param2, int param3, int param4, boolean param5) {
      return param1 == param4 && param3 == param4 && param4 > 0;
   }
}

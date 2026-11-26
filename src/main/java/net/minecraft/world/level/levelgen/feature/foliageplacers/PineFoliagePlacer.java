package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class PineFoliagePlacer extends FoliagePlacer {
   public static final Codec<PineFoliagePlacer> CODEC = RecordCodecBuilder.create(
      param0 -> foliagePlacerParts(param0)
         .and(IntProvider.codec(0, 24).fieldOf("height").forGetter(l_param0 -> l_param0.height))
         .apply(param0, PineFoliagePlacer::new)
   );
   private final IntProvider height;

   public PineFoliagePlacer(IntProvider param0, IntProvider param1, IntProvider param2) {
      super(param0, param1);
      this.height = param2;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.PINE_FOLIAGE_PLACER;
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
      int var10 = 0;

      for (int var11 = param8; var11 >= param8 - param6; var11--) {
         this.placeLeavesRow(param0, param1, param2, param3, param5.pos(), var10, var11, param5.doubleTrunk());
         if (var10 >= 1 && var11 == param8 - param6 + 1) {
            var10--;
         } else if (var10 < param7 + param5.radiusOffset()) {
            var10++;
         }
      }
   }

   @Override
   public int foliageRadius(RandomSource param0, int param1) {
      return super.foliageRadius(param0, param1) + param0.nextInt(Math.max(param1 + 1, 1));
   }

   @Override
   public int foliageHeight(RandomSource param0, int param1, TreeConfiguration param2) {
      return this.height.sample(param0);
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource param0, int param1, int param2, int param3, int param4, boolean param5) {
      return param1 == param4 && param3 == param4 && param4 > 0;
   }
}

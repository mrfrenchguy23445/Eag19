package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class FancyFoliagePlacer extends BlobFoliagePlacer {
   public static final Codec<FancyFoliagePlacer> CODEC = RecordCodecBuilder.create(param0 -> blobParts(param0).apply(param0, FancyFoliagePlacer::new));

   public FancyFoliagePlacer(IntProvider param0, IntProvider param1, int param2) {
      super(param0, param1, param2);
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.FANCY_FOLIAGE_PLACER;
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
      for (int var10 = param8; var10 >= param8 - param6; var10--) {
         int var11 = param7 + (var10 != param8 && var10 != param8 - param6 ? 1 : 0);
         this.placeLeavesRow(param0, param1, param2, param3, param5.pos(), var11, var10, param5.doubleTrunk());
      }
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource param0, int param1, int param2, int param3, int param4, boolean param5) {
      return Mth.square(param1 + 0.5F) + Mth.square(param3 + 0.5F) > param4 * param4;
   }
}

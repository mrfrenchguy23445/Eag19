package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class RandomSpreadFoliagePlacer extends FoliagePlacer {
   public static final Codec<RandomSpreadFoliagePlacer> CODEC = RecordCodecBuilder.create(
      param0 -> foliagePlacerParts(param0)
         .and(
            param0.group(
               IntProvider.codec(1, 512).fieldOf("foliage_height").forGetter(l_param0 -> l_param0.foliageHeight),
               Codec.intRange(0, 256).fieldOf("leaf_placement_attempts").forGetter(l_param0 -> l_param0.leafPlacementAttempts)
            )
         )
         .apply(param0, RandomSpreadFoliagePlacer::new)
   );
   private final IntProvider foliageHeight;
   private final int leafPlacementAttempts;

   public RandomSpreadFoliagePlacer(IntProvider param0, IntProvider param1, IntProvider param2, int param3) {
      super(param0, param1);
      this.foliageHeight = param2;
      this.leafPlacementAttempts = param3;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.RANDOM_SPREAD_FOLIAGE_PLACER;
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
      BlockPos.MutableBlockPos var11 = var10.mutable();

      for (int var12 = 0; var12 < this.leafPlacementAttempts; var12++) {
         var11.setWithOffset(
            var10,
            param2.nextInt(param7) - param2.nextInt(param7),
            param2.nextInt(param6) - param2.nextInt(param6),
            param2.nextInt(param7) - param2.nextInt(param7)
         );
         tryPlaceLeaf(param0, param1, param2, param3, var11);
      }
   }

   @Override
   public int foliageHeight(RandomSource param0, int param1, TreeConfiguration param2) {
      return this.foliageHeight.sample(param0);
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource param0, int param1, int param2, int param3, int param4, boolean param5) {
      return false;
   }
}

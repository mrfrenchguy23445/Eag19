package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class MegaJungleFoliagePlacer extends FoliagePlacer {
   public static final Codec<MegaJungleFoliagePlacer> CODEC = RecordCodecBuilder.create(
      param0 -> foliagePlacerParts(param0)
         .and(Codec.intRange(0, 16).fieldOf("height").forGetter(l_param0 -> l_param0.height))
         .apply(param0, MegaJungleFoliagePlacer::new)
   );
   protected final int height;

   public MegaJungleFoliagePlacer(IntProvider param0, IntProvider param1, int param2) {
      super(param0, param1);
      this.height = param2;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.MEGA_JUNGLE_FOLIAGE_PLACER;
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
      int var10 = param5.doubleTrunk() ? param6 : 1 + param2.nextInt(2);

      for (int var11 = param8; var11 >= param8 - var10; var11--) {
         int var12 = param7 + param5.radiusOffset() + 1 - var11;
         this.placeLeavesRow(param0, param1, param2, param3, param5.pos(), var12, var11, param5.doubleTrunk());
      }
   }

   @Override
   public int foliageHeight(RandomSource param0, int param1, TreeConfiguration param2) {
      return this.height;
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource param0, int param1, int param2, int param3, int param4, boolean param5) {
      return param1 + param3 >= 7 ? true : param1 * param1 + param3 * param3 > param4 * param4;
   }
}

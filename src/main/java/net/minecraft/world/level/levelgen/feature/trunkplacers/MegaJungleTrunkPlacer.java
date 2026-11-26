package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class MegaJungleTrunkPlacer extends GiantTrunkPlacer {
   public static final Codec<MegaJungleTrunkPlacer> CODEC = RecordCodecBuilder.create(
      param0 -> trunkPlacerParts(param0).apply(param0, MegaJungleTrunkPlacer::new)
   );

   public MegaJungleTrunkPlacer(int param0, int param1, int param2) {
      super(param0, param1, param2);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.MEGA_JUNGLE_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader param0, BiConsumer<BlockPos, BlockState> param1, RandomSource param2, int param3, BlockPos param4, TreeConfiguration param5
   ) {
      List<FoliagePlacer.FoliageAttachment> var7 = Lists.newArrayList();
      var7.addAll(super.placeTrunk(param0, param1, param2, param3, param4, param5));

      for (int var8 = param3 - 2 - param2.nextInt(4); var8 > param3 / 2; var8 -= 2 + param2.nextInt(4)) {
         float var9 = param2.nextFloat() * (float) (Math.PI * 2);
         int var10 = 0;
         int var11 = 0;

         for (int var12 = 0; var12 < 5; var12++) {
            var10 = (int)(1.5F + Mth.cos(var9) * var12);
            var11 = (int)(1.5F + Mth.sin(var9) * var12);
            BlockPos var13 = param4.offset(var10, var8 - 3 + var12 / 2, var11);
            this.placeLog(param0, param1, param2, var13, param5);
         }

         var7.add(new FoliagePlacer.FoliageAttachment(param4.offset(var10, var8, var11), -2, false));
      }

      return var7;
   }
}

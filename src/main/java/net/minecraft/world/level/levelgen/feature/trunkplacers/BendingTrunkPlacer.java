package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class BendingTrunkPlacer extends TrunkPlacer {
   public static final Codec<BendingTrunkPlacer> CODEC = RecordCodecBuilder.create(
      param0 -> trunkPlacerParts(param0)
         .and(
            param0.group(
               ExtraCodecs.POSITIVE_INT.optionalFieldOf("min_height_for_leaves", 1).forGetter(l_param0 -> l_param0.minHeightForLeaves),
               IntProvider.codec(1, 64).fieldOf("bend_length").forGetter(l_param0 -> l_param0.bendLength)
            )
         )
         .apply(param0, BendingTrunkPlacer::new)
   );
   private final int minHeightForLeaves;
   private final IntProvider bendLength;

   public BendingTrunkPlacer(int param0, int param1, int param2, int param3, IntProvider param4) {
      super(param0, param1, param2);
      this.minHeightForLeaves = param3;
      this.bendLength = param4;
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.BENDING_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader param0, BiConsumer<BlockPos, BlockState> param1, RandomSource param2, int param3, BlockPos param4, TreeConfiguration param5
   ) {
      Direction var7 = Direction.Plane.HORIZONTAL.getRandomDirection(param2);
      int var8 = param3 - 1;
      BlockPos.MutableBlockPos var9 = param4.mutable();
      BlockPos var10 = var9.below();
      setDirtAt(param0, param1, param2, var10, param5);
      List<FoliagePlacer.FoliageAttachment> var11 = Lists.newArrayList();

      for (int var12 = 0; var12 <= var8; var12++) {
         if (var12 + 1 >= var8 + param2.nextInt(2)) {
            var9.move(var7);
         }

         if (TreeFeature.validTreePos(param0, var9)) {
            this.placeLog(param0, param1, param2, var9, param5);
         }

         if (var12 >= this.minHeightForLeaves) {
            var11.add(new FoliagePlacer.FoliageAttachment(var9.immutable(), 0, false));
         }

         var9.move(Direction.UP);
      }

      int var12 = this.bendLength.sample(param2);

      for (int var13 = 0; var13 <= var12; var13++) {
         if (TreeFeature.validTreePos(param0, var9)) {
            this.placeLog(param0, param1, param2, var9, param5);
         }

         var11.add(new FoliagePlacer.FoliageAttachment(var9.immutable(), 0, false));
         var9.move(var7);
      }

      return var11;
   }
}

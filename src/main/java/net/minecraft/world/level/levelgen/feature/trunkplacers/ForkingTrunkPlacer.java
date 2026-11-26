package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class ForkingTrunkPlacer extends TrunkPlacer {
   public static final Codec<ForkingTrunkPlacer> CODEC = RecordCodecBuilder.create(param0 -> trunkPlacerParts(param0).apply(param0, ForkingTrunkPlacer::new));

   public ForkingTrunkPlacer(int param0, int param1, int param2) {
      super(param0, param1, param2);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.FORKING_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader param0, BiConsumer<BlockPos, BlockState> param1, RandomSource param2, int param3, BlockPos param4, TreeConfiguration param5
   ) {
      setDirtAt(param0, param1, param2, param4.below(), param5);
      List<FoliagePlacer.FoliageAttachment> var7 = Lists.newArrayList();
      Direction var8 = Direction.Plane.HORIZONTAL.getRandomDirection(param2);
      int var9 = param3 - param2.nextInt(4) - 1;
      int var10 = 3 - param2.nextInt(3);
      BlockPos.MutableBlockPos var11 = new BlockPos.MutableBlockPos();
      int var12 = param4.getX();
      int var13 = param4.getZ();
      OptionalInt var14 = OptionalInt.empty();

      for (int var15 = 0; var15 < param3; var15++) {
         int var16 = param4.getY() + var15;
         if (var15 >= var9 && var10 > 0) {
            var12 += var8.getStepX();
            var13 += var8.getStepZ();
            var10--;
         }

         if (this.placeLog(param0, param1, param2, var11.set(var12, var16, var13), param5)) {
            var14 = OptionalInt.of(var16 + 1);
         }
      }

      if (var14.isPresent()) {
         var7.add(new FoliagePlacer.FoliageAttachment(new BlockPos(var12, var14.getAsInt(), var13), 1, false));
      }

      var12 = param4.getX();
      var13 = param4.getZ();
      Direction var15 = Direction.Plane.HORIZONTAL.getRandomDirection(param2);
      if (var15 != var8) {
         int var16 = var9 - param2.nextInt(2) - 1;
         int var17 = 1 + param2.nextInt(3);
         var14 = OptionalInt.empty();

         for (int var18 = var16; var18 < param3 && var17 > 0; var17--) {
            if (var18 >= 1) {
               int var19 = param4.getY() + var18;
               var12 += var15.getStepX();
               var13 += var15.getStepZ();
               if (this.placeLog(param0, param1, param2, var11.set(var12, var19, var13), param5)) {
                  var14 = OptionalInt.of(var19 + 1);
               }
            }

            var18++;
         }

         if (var14.isPresent()) {
            var7.add(new FoliagePlacer.FoliageAttachment(new BlockPos(var12, var14.getAsInt(), var13), 0, false));
         }
      }

      return var7;
   }
}

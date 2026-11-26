package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class DarkOakTrunkPlacer extends TrunkPlacer {
   public static final Codec<DarkOakTrunkPlacer> CODEC = RecordCodecBuilder.create(param0 -> trunkPlacerParts(param0).apply(param0, DarkOakTrunkPlacer::new));

   public DarkOakTrunkPlacer(int param0, int param1, int param2) {
      super(param0, param1, param2);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.DARK_OAK_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader param0, BiConsumer<BlockPos, BlockState> param1, RandomSource param2, int param3, BlockPos param4, TreeConfiguration param5
   ) {
      List<FoliagePlacer.FoliageAttachment> var7 = Lists.newArrayList();
      BlockPos var8 = param4.below();
      setDirtAt(param0, param1, param2, var8, param5);
      setDirtAt(param0, param1, param2, var8.east(), param5);
      setDirtAt(param0, param1, param2, var8.south(), param5);
      setDirtAt(param0, param1, param2, var8.south().east(), param5);
      Direction var9 = Direction.Plane.HORIZONTAL.getRandomDirection(param2);
      int var10 = param3 - param2.nextInt(4);
      int var11 = 2 - param2.nextInt(3);
      int var12 = param4.getX();
      int var13 = param4.getY();
      int var14 = param4.getZ();
      int var15 = var12;
      int var16 = var14;
      int var17 = var13 + param3 - 1;

      for (int var18 = 0; var18 < param3; var18++) {
         if (var18 >= var10 && var11 > 0) {
            var15 += var9.getStepX();
            var16 += var9.getStepZ();
            var11--;
         }

         int var19 = var13 + var18;
         BlockPos var20 = new BlockPos(var15, var19, var16);
         if (TreeFeature.isAirOrLeaves(param0, var20)) {
            this.placeLog(param0, param1, param2, var20, param5);
            this.placeLog(param0, param1, param2, var20.east(), param5);
            this.placeLog(param0, param1, param2, var20.south(), param5);
            this.placeLog(param0, param1, param2, var20.east().south(), param5);
         }
      }

      var7.add(new FoliagePlacer.FoliageAttachment(new BlockPos(var15, var17, var16), 0, true));

      for (int var18 = -1; var18 <= 2; var18++) {
         for (int var19 = -1; var19 <= 2; var19++) {
            if ((var18 < 0 || var18 > 1 || var19 < 0 || var19 > 1) && param2.nextInt(3) <= 0) {
               int var20 = param2.nextInt(3) + 2;

               for (int var21 = 0; var21 < var20; var21++) {
                  this.placeLog(param0, param1, param2, new BlockPos(var12 + var18, var17 - var21 - 1, var14 + var19), param5);
               }

               var7.add(new FoliagePlacer.FoliageAttachment(new BlockPos(var15 + var18, var17, var16 + var19), 0, false));
            }
         }
      }

      return var7;
   }
}

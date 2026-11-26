package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CoralClawFeature extends CoralFeature {
   public CoralClawFeature(Codec<NoneFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   protected boolean placeFeature(LevelAccessor param0, RandomSource param1, BlockPos param2, BlockState param3) {
      if (!this.placeCoralBlock(param0, param1, param2, param3)) {
         return false;
      }

      Direction var5 = Direction.Plane.HORIZONTAL.getRandomDirection(param1);
      int var6 = param1.nextInt(2) + 2;
      List<Direction> var7 = Util.toShuffledList(Stream.of(var5, var5.getClockWise(), var5.getCounterClockWise()), param1);

      for (Direction var10 : var7.subList(0, var6)) {
         BlockPos.MutableBlockPos var11 = param2.mutable();
         int var12 = param1.nextInt(2) + 1;
         var11.move(var10);
         int var13;
         Direction var14;
         if (var10 == var5) {
            var14 = var5;
            var13 = param1.nextInt(3) + 2;
         } else {
            var11.move(Direction.UP);
            Direction[] var15 = new Direction[]{var10, Direction.UP};
            var14 = Util.getRandom(var15, param1);
            var13 = param1.nextInt(3) + 3;
         }

         for (int var15 = 0; var15 < var12 && this.placeCoralBlock(param0, param1, var11, param3); var15++) {
            var11.move(var14);
         }

         var11.move(var14.getOpposite());
         var11.move(Direction.UP);

         for (int var15 = 0; var15 < var13; var15++) {
            var11.move(var5);
            if (!this.placeCoralBlock(param0, param1, var11, param3)) {
               break;
            }

            if (param1.nextFloat() < 0.25F) {
               var11.move(Direction.UP);
            }
         }
      }

      return true;
   }
}

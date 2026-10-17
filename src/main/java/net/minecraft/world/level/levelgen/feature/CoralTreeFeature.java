package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CoralTreeFeature extends CoralFeature {
   public CoralTreeFeature(Codec<NoneFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   protected boolean placeFeature(LevelAccessor param0, RandomSource param1, BlockPos param2, BlockState param3) {
      BlockPos.MutableBlockPos var5 = param2.mutable();
      int var6 = param1.nextInt(3) + 1;

      for (int var7 = 0; var7 < var6; var7++) {
         if (!this.placeCoralBlock(param0, param1, var5, param3)) {
            return true;
         }

         var5.move(Direction.UP);
      }

      BlockPos var7 = var5.immutable();
      int var8 = param1.nextInt(3) + 2;
      List<Direction> var9 = Direction.Plane.HORIZONTAL.shuffledCopy(param1);

      for (Direction var12 : var9.subList(0, var8)) {
         var5.set(var7);
         var5.move(var12);
         int var13 = param1.nextInt(5) + 2;
         int var14 = 0;

         for (int var15 = 0; var15 < var13 && this.placeCoralBlock(param0, param1, var5, param3); var15++) {
            var14++;
            var5.move(Direction.UP);
            if (var15 == 0 || var14 >= 2 && param1.nextFloat() < 0.25F) {
               var5.move(var12);
               var14 = 0;
            }
         }
      }

      return true;
   }
}

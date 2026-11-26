package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CoralMushroomFeature extends CoralFeature {
   public CoralMushroomFeature(Codec<NoneFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   protected boolean placeFeature(LevelAccessor param0, RandomSource param1, BlockPos param2, BlockState param3) {
      int var5 = param1.nextInt(3) + 3;
      int var6 = param1.nextInt(3) + 3;
      int var7 = param1.nextInt(3) + 3;
      int var8 = param1.nextInt(3) + 1;
      BlockPos.MutableBlockPos var9 = param2.mutable();

      for (int var10 = 0; var10 <= var6; var10++) {
         for (int var11 = 0; var11 <= var5; var11++) {
            for (int var12 = 0; var12 <= var7; var12++) {
               var9.set(var10 + param2.getX(), var11 + param2.getY(), var12 + param2.getZ());
               var9.move(Direction.DOWN, var8);
               if ((var10 != 0 && var10 != var6 || var11 != 0 && var11 != var5)
                  && (var12 != 0 && var12 != var7 || var11 != 0 && var11 != var5)
                  && (var10 != 0 && var10 != var6 || var12 != 0 && var12 != var7)
                  && (var10 == 0 || var10 == var6 || var11 == 0 || var11 == var5 || var12 == 0 || var12 == var7)
                  && !(param1.nextFloat() < 0.1F)
                  && !this.placeCoralBlock(param0, param1, var9, param3)) {
               }
            }
         }
      }

      return true;
   }
}

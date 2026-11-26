package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public interface ChangeOverTimeBlock<T extends Enum<T>> {
   int SCAN_DISTANCE = 4;

   Optional<BlockState> getNext(BlockState var1);

   float getChanceModifier();

   default void onRandomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      float var5 = 0.05688889F;
      if (param3.nextFloat() < 0.05688889F) {
         this.applyChangeOverTime(param0, param1, param2, param3);
      }
   }

   T getAge();

   default void applyChangeOverTime(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      int var5 = this.getAge().ordinal();
      int var6 = 0;
      int var7 = 0;

      for (BlockPos var9 : BlockPos.withinManhattan(param2, 4, 4, 4)) {
         int var10 = var9.distManhattan(param2);
         if (var10 > 4) {
            break;
         }

         if (!var9.equals(param2)) {
            BlockState var11 = param1.getBlockState(var9);
            Block var12 = var11.getBlock();
            if (var12 instanceof ChangeOverTimeBlock) {
               Enum<?> var13 = ((ChangeOverTimeBlock)var12).getAge();
               if (this.getAge().getClass() == var13.getClass()) {
                  int var14 = var13.ordinal();
                  if (var14 < var5) {
                     return;
                  }

                  if (var14 > var5) {
                     var7++;
                  } else {
                     var6++;
                  }
               }
            }
         }
      }

      float var8 = (float)(var7 + 1) / (var7 + var6 + 1);
      float var9 = var8 * var8 * this.getChanceModifier();
      if (param3.nextFloat() < var9) {
         this.getNext(param0).ifPresent(l_param2 -> param1.setBlockAndUpdate(param2, l_param2));
      }
   }
}

package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class RingBell {
   private static final float BELL_RING_CHANCE = 0.95F;
   public static final int RING_BELL_FROM_DISTANCE = 3;

   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(l_param0.present(MemoryModuleType.MEETING_POINT)).apply(l_param0, l2_param1 -> (l3_param2, l3_param3, l3_param4) -> {
            if (l3_param2.random.nextFloat() <= 0.95F) {
               return false;
            }

            BlockPos l3_var6 = l_param0.get(l2_param1).pos();
            if (l3_var6.closerThan(l3_param3.blockPosition(), 3.0)) {
               BlockState l3_var7 = l3_param2.getBlockState(l3_var6);
               if (l3_var7.is(Blocks.BELL)) {
                  BellBlock l3_var8 = (BellBlock)l3_var7.getBlock();
                  l3_var8.attemptToRing(l3_param3, l3_param2, l3_var6, null);
               }
            }

            return true;
         })
      );
   }
}

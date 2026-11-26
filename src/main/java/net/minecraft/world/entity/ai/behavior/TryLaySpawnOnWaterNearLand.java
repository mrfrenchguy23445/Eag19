package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;

public class TryLaySpawnOnWaterNearLand {
   public static BehaviorControl<LivingEntity> create(Block param0) {
      return BehaviorBuilder.create(
         l_param1 -> l_param1.group(
               l_param1.absent(MemoryModuleType.ATTACK_TARGET), l_param1.present(MemoryModuleType.WALK_TARGET), l_param1.present(MemoryModuleType.IS_PREGNANT)
            )
            .apply(
               l_param1,
               (l2_param1, l2_param2, l2_param3) -> (l3_param2, l3_param3, l3_param4) -> {
                  if (!l3_param3.isInWater() && l3_param3.isOnGround()) {
                     BlockPos l3_var6 = l3_param3.blockPosition().below();

                     for (Direction l3_var8 : Direction.Plane.HORIZONTAL) {
                        BlockPos l3_var9 = l3_var6.relative(l3_var8);
                        if (l3_param2.getBlockState(l3_var9).getCollisionShape(l3_param2, l3_var9).getFaceShape(Direction.UP).isEmpty()
                           && l3_param2.getFluidState(l3_var9).is(Fluids.WATER)) {
                           BlockPos l3_var10 = l3_var9.above();
                           if (l3_param2.getBlockState(l3_var10).isAir()) {
                              BlockState l3_var11 = param0.defaultBlockState();
                              l3_param2.setBlock(l3_var10, l3_var11, 3);
                              l3_param2.gameEvent(GameEvent.BLOCK_PLACE, l3_var10, GameEvent.Context.of(l3_param3, l3_var11));
                              l3_param2.playSound(null, l3_param3, SoundEvents.FROG_LAY_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                              l2_param3.erase();
                              return true;
                           }
                        }
                     }

                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }
}

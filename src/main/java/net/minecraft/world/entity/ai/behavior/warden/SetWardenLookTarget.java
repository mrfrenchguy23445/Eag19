package net.minecraft.world.entity.ai.behavior.warden;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class SetWardenLookTarget {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(
               l_param0.registered(MemoryModuleType.LOOK_TARGET),
               l_param0.registered(MemoryModuleType.DISTURBANCE_LOCATION),
               l_param0.registered(MemoryModuleType.ROAR_TARGET),
               l_param0.absent(MemoryModuleType.ATTACK_TARGET)
            )
            .apply(l_param0, (l2_param1, l2_param2, l2_param3, l2_param4) -> (l3_param4, l3_param5, l3_param6) -> {
               Optional<BlockPos> l3_var8 = l_param0.<LivingEntity>tryGet(l2_param3).map(Entity::blockPosition).or(() -> l_param0.tryGet(l2_param2));
               if (l3_var8.isEmpty()) {
                  return false;
               }

               l2_param1.set(new BlockPosTracker(l3_var8.get()));
               return true;
            })
      );
   }
}

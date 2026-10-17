package net.minecraft.world.entity.ai.behavior.warden;

import net.minecraft.util.Unit;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class TryToSniff {
   private static final IntProvider SNIFF_COOLDOWN = UniformInt.of(100, 200);

   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(
               l_param0.registered(MemoryModuleType.IS_SNIFFING),
               l_param0.registered(MemoryModuleType.WALK_TARGET),
               l_param0.absent(MemoryModuleType.SNIFF_COOLDOWN),
               l_param0.present(MemoryModuleType.NEAREST_ATTACKABLE),
               l_param0.absent(MemoryModuleType.DISTURBANCE_LOCATION)
            )
            .apply(l_param0, (l2_param0, l2_param1, l2_param2, l2_param3, l2_param4) -> (l3_param3, l3_param4, l3_param5) -> {
               l2_param0.set(Unit.INSTANCE);
               l2_param2.setWithExpiry(Unit.INSTANCE, SNIFF_COOLDOWN.sample(l3_param3.getRandom()));
               l2_param1.erase();
               l3_param4.setPose(Pose.SNIFFING);
               return true;
            })
      );
   }
}

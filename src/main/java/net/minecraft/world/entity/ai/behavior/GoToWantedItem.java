package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.K1;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;

public class GoToWantedItem {
   public static BehaviorControl<LivingEntity> create(float param0, boolean param1, int param2) {
      return create(l_param0 -> true, param0, param1, param2);
   }

   public static <E extends LivingEntity> BehaviorControl<E> create(Predicate<E> param0, float param1, boolean param2, int param3) {
      return BehaviorBuilder.create(
         l_param4 -> {
            BehaviorBuilder<E, ? extends MemoryAccessor<? extends K1, WalkTarget>> l_var5 = param2
               ? l_param4.registered(MemoryModuleType.WALK_TARGET)
               : l_param4.absent(MemoryModuleType.WALK_TARGET);
            return l_param4.group(
                  l_param4.registered(MemoryModuleType.LOOK_TARGET),
                  l_var5,
                  l_param4.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM),
                  l_param4.registered(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS)
               )
               .apply(
                  l_param4,
                  (l2_param4, l2_param5, l2_param6, l2_param7) -> (l3_param8, l3_param9, l3_param10) -> {
                     ItemEntity l3_var12 = l_param4.get(l2_param6);
                     if (l_param4.tryGet(l2_param7).isEmpty()
                        && param0.test(l3_param9)
                        && l3_var12.closerThan(l3_param9, param3)
                        && l3_param9.level.getWorldBorder().isWithinBounds(l3_var12.blockPosition())) {
                        WalkTarget l3_var13 = new WalkTarget(new EntityTracker(l3_var12, false), param1, 0);
                        l2_param4.set(new EntityTracker(l3_var12, true));
                        l2_param5.set(l3_var13);
                        return true;
                     } else {
                        return false;
                     }
                  }
               );
         }
      );
   }
}

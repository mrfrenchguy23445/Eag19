package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileWeaponItem;

public class MeleeAttack {
   public static OneShot<Mob> create(int param0) {
      return BehaviorBuilder.create(
         l_param1 -> l_param1.group(
               l_param1.registered(MemoryModuleType.LOOK_TARGET),
               l_param1.present(MemoryModuleType.ATTACK_TARGET),
               l_param1.absent(MemoryModuleType.ATTACK_COOLING_DOWN),
               l_param1.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            )
            .apply(
               l_param1,
               (l2_param2, l2_param3, l2_param4, l2_param5) -> (l3_param6, l3_param7, l3_param8) -> {
                  LivingEntity l3_var10 = l_param1.get(l2_param3);
                  if (!isHoldingUsableProjectileWeapon(l3_param7)
                     && l3_param7.isWithinMeleeAttackRange(l3_var10)
                     && l_param1.<NearestVisibleLivingEntities>get(l2_param5).contains(l3_var10)) {
                     l2_param2.set(new EntityTracker(l3_var10, true));
                     l3_param7.swing(InteractionHand.MAIN_HAND);
                     l3_param7.doHurtTarget(l3_var10);
                     l2_param4.setWithExpiry(true, param0);
                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }

   private static boolean isHoldingUsableProjectileWeapon(Mob param0) {
      return param0.isHolding(l_param1 -> {
         Item l_var2 = l_param1.getItem();
         return l_var2 instanceof ProjectileWeaponItem && param0.canFireProjectileWeapon((ProjectileWeaponItem)l_var2);
      });
   }
}

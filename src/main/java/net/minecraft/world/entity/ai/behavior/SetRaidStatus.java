package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class SetRaidStatus {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(l_param0 -> l_param0.point((l2_param0, l2_param1, l2_param2) -> {
         if (l2_param0.random.nextInt(20) != 0) {
            return false;
         }

         Brain<?> l2_var4 = l2_param1.getBrain();
         Raid l2_var5 = l2_param0.getRaidAt(l2_param1.blockPosition());
         if (l2_var5 != null) {
            if (l2_var5.hasFirstWaveSpawned() && !l2_var5.isBetweenWaves()) {
               l2_var4.setDefaultActivity(Activity.RAID);
               l2_var4.setActiveActivityIfPossible(Activity.RAID);
            } else {
               l2_var4.setDefaultActivity(Activity.PRE_RAID);
               l2_var4.setActiveActivityIfPossible(Activity.PRE_RAID);
            }
         }

         return true;
      }));
   }
}

package net.minecraft.world.entity.ai.behavior;

import java.util.function.BiPredicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class DismountOrSkipMounting {
   public static <E extends LivingEntity> BehaviorControl<E> create(int param0, BiPredicate<E, Entity> param1) {
      return BehaviorBuilder.create(
         l_param2 -> l_param2.group(l_param2.registered(MemoryModuleType.RIDE_TARGET)).apply(l_param2, l2_param3 -> (l3_param4, l3_param5, l3_param6) -> {
            Entity l3_var8 = l3_param5.getVehicle();
            Entity l3_var9 = l_param2.<Entity>tryGet(l2_param3).orElse(null);
            if (l3_var8 == null && l3_var9 == null) {
               return false;
            }

            Entity l3_var10 = l3_var8 == null ? l3_var9 : l3_var8;
            if (isVehicleValid(l3_param5, l3_var10, param0) && !param1.test(l3_param5, l3_var10)) {
               return false;
            }

            l3_param5.stopRiding();
            l2_param3.erase();
            return true;
         })
      );
   }

   private static boolean isVehicleValid(LivingEntity param0, Entity param1, int param2) {
      return param1.isAlive() && param1.closerThan(param0, param2) && param1.level == param0.level;
   }
}

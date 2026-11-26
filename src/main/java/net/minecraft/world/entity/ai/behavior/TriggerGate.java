package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;

public class TriggerGate {
   public static <E extends LivingEntity> OneShot<E> triggerOneShuffled(List<Pair<? extends Trigger<? super E>, Integer>> param0) {
      return triggerGate(param0, GateBehavior.OrderPolicy.SHUFFLED, GateBehavior.RunningPolicy.RUN_ONE);
   }

   public static <E extends LivingEntity> OneShot<E> triggerGate(
      List<Pair<? extends Trigger<? super E>, Integer>> param0, GateBehavior.OrderPolicy param1, GateBehavior.RunningPolicy param2
   ) {
      ShufflingList<Trigger<? super E>> var3 = new ShufflingList<>();
      param0.forEach(l_param1 -> var3.add(l_param1.getFirst(), l_param1.getSecond()));
      return BehaviorBuilder.create(l_param3 -> l_param3.point((l2_param3, l2_param4, l2_param5) -> {
         if (param1 == GateBehavior.OrderPolicy.SHUFFLED) {
            var3.shuffle();
         }

         for (Trigger<? super E> l2_var8 : var3) {
            if (l2_var8.trigger(l2_param3, l2_param4, l2_param5) && param2 == GateBehavior.RunningPolicy.RUN_ONE) {
               break;
            }
         }

         return true;
      }));
   }
}

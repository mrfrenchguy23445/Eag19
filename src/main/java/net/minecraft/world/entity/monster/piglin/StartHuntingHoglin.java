package net.minecraft.world.entity.monster.piglin;

import java.util.List;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

public class StartHuntingHoglin {
   public static OneShot<Piglin> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(
               l_param0.present(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN),
               l_param0.absent(MemoryModuleType.ANGRY_AT),
               l_param0.absent(MemoryModuleType.HUNTED_RECENTLY),
               l_param0.registered(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS)
            )
            .apply(
               l_param0,
               (l2_param1, l2_param2, l2_param3, l2_param4) -> (l3_param3, l3_param4, l3_param5) -> {
                  if (!l3_param4.isBaby()
                     && !l_param0.<List<AbstractPiglin>>tryGet(l2_param4).map(l4_param0 -> l4_param0.stream().anyMatch(StartHuntingHoglin::hasHuntedRecently)).isPresent()) {
                     Hoglin l3_var7 = l_param0.get(l2_param1);
                     PiglinAi.setAngerTarget(l3_param4, l3_var7);
                     PiglinAi.dontKillAnyMoreHoglinsForAWhile(l3_param4);
                     PiglinAi.broadcastAngerTarget(l3_param4, l3_var7);
                     l_param0.<List<AbstractPiglin>>tryGet(l2_param4).ifPresent(l4_param0 -> l4_param0.forEach(PiglinAi::dontKillAnyMoreHoglinsForAWhile));
                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }

   private static boolean hasHuntedRecently(AbstractPiglin param0) {
      return param0.getBrain().hasMemoryValue(MemoryModuleType.HUNTED_RECENTLY);
   }
}

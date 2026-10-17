package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;

public class ResetProfession {
   public static BehaviorControl<Villager> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(l_param0.absent(MemoryModuleType.JOB_SITE))
            .apply(
               l_param0,
               l2_param0 -> (l3_param0, l3_param1, l3_param2) -> {
                  VillagerData l3_var4 = l3_param1.getVillagerData();
                  if (l3_var4.getProfession() != VillagerProfession.NONE
                     && l3_var4.getProfession() != VillagerProfession.NITWIT
                     && l3_param1.getVillagerXp() == 0
                     && l3_var4.getLevel() <= 1) {
                     l3_param1.setVillagerData(l3_param1.getVillagerData().setProfession(VillagerProfession.NONE));
                     l3_param1.refreshBrain(l3_param0);
                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }
}

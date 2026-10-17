package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

public class PoiCompetitorScan {
   public static BehaviorControl<Villager> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(l_param0.present(MemoryModuleType.JOB_SITE), l_param0.present(MemoryModuleType.NEAREST_LIVING_ENTITIES))
            .apply(
               l_param0,
               (l2_param1, l2_param2) -> (l3_param3, l3_param4, l3_param5) -> {
                  GlobalPos l3_var7 = l_param0.get(l2_param1);
                  l3_param3.getPoiManager()
                     .getType(l3_var7.pos())
                     .ifPresent(
                        l4_param4 -> l_param0.<List<LivingEntity>>get(l2_param2)
                           .stream()
                           .filter(l5_param1 -> l5_param1 instanceof Villager && l5_param1 != l3_param4)
                           .map(l5_param0 -> (Villager)l5_param0)
                           .filter(LivingEntity::isAlive)
                           .filter(l5_param2 -> competesForSameJobsite(l3_var7, l4_param4, l5_param2))
                           .reduce(l3_param4, PoiCompetitorScan::selectWinner)
                     );
                  return true;
               }
            )
      );
   }

   private static Villager selectWinner(Villager param0, Villager param1) {
      Villager var2;
      Villager var3;
      if (param0.getVillagerXp() > param1.getVillagerXp()) {
         var2 = param0;
         var3 = param1;
      } else {
         var2 = param1;
         var3 = param0;
      }

      var3.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
      return var2;
   }

   private static boolean competesForSameJobsite(GlobalPos param0, Holder<PoiType> param1, Villager param2) {
      Optional<GlobalPos> var3 = param2.getBrain().getMemory(MemoryModuleType.JOB_SITE);
      return var3.isPresent() && param0.equals(var3.get()) && hasMatchingProfession(param1, param2.getVillagerData().getProfession());
   }

   private static boolean hasMatchingProfession(Holder<PoiType> param0, VillagerProfession param1) {
      return param1.heldJobSite().test(param0);
   }
}

package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

public class AssignProfessionFromJobSite {
   public static BehaviorControl<Villager> create() {
      return BehaviorBuilder.create(
         l_param0 -> l_param0.group(l_param0.present(MemoryModuleType.POTENTIAL_JOB_SITE), l_param0.registered(MemoryModuleType.JOB_SITE))
            .apply(
               l_param0,
               (l2_param1, l2_param2) -> (l3_param3, l3_param4, l3_param5) -> {
                  GlobalPos l3_var7 = l_param0.get(l2_param1);
                  if (!l3_var7.pos().closerToCenterThan(l3_param4.position(), 2.0) && !l3_param4.assignProfessionWhenSpawned()) {
                     return false;
                  }

                  l2_param1.erase();
                  l2_param2.set(l3_var7);
                  l3_param3.broadcastEntityEvent(l3_param4, (byte)14);
                  if (l3_param4.getVillagerData().getProfession() != VillagerProfession.NONE) {
                     return true;
                  }

                  MinecraftServer l3_var8 = l3_param3.getServer();
                  Optional.ofNullable(l3_var8.getLevel(l3_var7.dimension()))
                     .flatMap(l4_param1 -> l4_param1.getPoiManager().getType(l3_var7.pos()))
                     .flatMap(
                        l4_param0 -> BuiltInRegistries.VILLAGER_PROFESSION
                           .stream()
                           .filter(l5_param1 -> l5_param1.heldJobSite().test((Holder<PoiType>)l4_param0))
                           .findFirst()
                     )
                     .ifPresent(l4_param2 -> {
                        l3_param4.setVillagerData(l3_param4.getVillagerData().setProfession(l4_param2));
                        l3_param4.refreshBrain(l3_param3);
                     });
                  return true;
               }
            )
      );
   }
}

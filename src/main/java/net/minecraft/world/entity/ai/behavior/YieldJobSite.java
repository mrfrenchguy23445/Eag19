package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.pathfinder.Path;

public class YieldJobSite {
   public static BehaviorControl<Villager> create(float param0) {
      return BehaviorBuilder.create(
         l_param1 -> l_param1.group(
               l_param1.present(MemoryModuleType.POTENTIAL_JOB_SITE),
               l_param1.absent(MemoryModuleType.JOB_SITE),
               l_param1.present(MemoryModuleType.NEAREST_LIVING_ENTITIES),
               l_param1.registered(MemoryModuleType.WALK_TARGET),
               l_param1.registered(MemoryModuleType.LOOK_TARGET)
            )
            .apply(
               l_param1,
               (l2_param2, l2_param3, l2_param4, l2_param5, l2_param6) -> (l3_param6, l3_param7, l3_param8) -> {
                  if (l3_param7.isBaby()) {
                     return false;
                  }

                  if (l3_param7.getVillagerData().getProfession() != VillagerProfession.NONE) {
                     return false;
                  }

                  BlockPos l3_var10 = l_param1.<GlobalPos>get(l2_param2).pos();
                  Optional<Holder<PoiType>> l3_var11 = l3_param6.getPoiManager().getType(l3_var10);
                  if (l3_var11.isEmpty()) {
                     return true;
                  }

                  l_param1.<List<LivingEntity>>get(l2_param4)
                     .stream()
                     .filter(l4_param1 -> l4_param1 instanceof Villager && l4_param1 != l3_param7)
                     .map(l4_param0 -> (Villager)l4_param0)
                     .filter(LivingEntity::isAlive)
                     .filter(l4_param2 -> nearbyWantsJobsite(l3_var11.get(), l4_param2, l3_var10))
                     .findFirst()
                     .ifPresent(l4_param6 -> {
                        l2_param5.erase();
                        l2_param6.erase();
                        l2_param2.erase();
                        if (l4_param6.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty()) {
                           BehaviorUtils.setWalkAndLookTargetMemories(l4_param6, l3_var10, param0, 1);
                           l4_param6.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(l3_param6.dimension(), l3_var10));
                           DebugPackets.sendPoiTicketCountPacket(l3_param6, l3_var10);
                        }
                     });
                  return true;
               }
            )
      );
   }

   private static boolean nearbyWantsJobsite(Holder<PoiType> param0, Villager param1, BlockPos param2) {
      boolean var3 = param1.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isPresent();
      if (var3) {
         return false;
      } else {
         Optional<GlobalPos> var4 = param1.getBrain().getMemory(MemoryModuleType.JOB_SITE);
         VillagerProfession var5 = param1.getVillagerData().getProfession();
         if (var5.heldJobSite().test(param0)) {
            return var4.isEmpty() ? canReachPos(param1, param2, param0.value()) : var4.get().pos().equals(param2);
         } else {
            return false;
         }
      }
   }

   private static boolean canReachPos(PathfinderMob param0, BlockPos param1, PoiType param2) {
      Path var3 = param0.getNavigation().createPath(param1, param2.validRange());
      return var3 != null && var3.canReach();
   }
}

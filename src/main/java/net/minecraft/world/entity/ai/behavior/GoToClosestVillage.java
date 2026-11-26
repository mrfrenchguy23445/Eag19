package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

public class GoToClosestVillage {
   public static BehaviorControl<Villager> create(float param0, int param1) {
      return BehaviorBuilder.create(
         l_param2 -> l_param2.group(l_param2.absent(MemoryModuleType.WALK_TARGET)).apply(l_param2, l2_param2 -> (l3_param3, l3_param4, l3_param5) -> {
            if (l3_param3.isVillage(l3_param4.blockPosition())) {
               return false;
            }

            PoiManager l3_var7 = l3_param3.getPoiManager();
            int l3_var8 = l3_var7.sectionsToVillage(SectionPos.of(l3_param4.blockPosition()));
            Vec3 l3_var9 = null;

            for (int l3_var10 = 0; l3_var10 < 5; l3_var10++) {
               Vec3 l3_var11 = LandRandomPos.getPos(l3_param4, 15, 7, l4_param1 -> -l3_var7.sectionsToVillage(SectionPos.of(l4_param1)));
               if (l3_var11 != null) {
                  int l3_var12 = l3_var7.sectionsToVillage(SectionPos.of(BlockPos.containing(l3_var11)));
                  if (l3_var12 < l3_var8) {
                     l3_var9 = l3_var11;
                     break;
                  }

                  if (l3_var12 == l3_var8) {
                     l3_var9 = l3_var11;
                  }
               }
            }

            if (l3_var9 != null) {
               l2_param2.set(new WalkTarget(l3_var9, param0, param1));
            }

            return true;
         })
      );
   }
}

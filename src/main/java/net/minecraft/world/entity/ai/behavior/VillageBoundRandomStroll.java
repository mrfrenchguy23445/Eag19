package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class VillageBoundRandomStroll {
   private static final int MAX_XZ_DIST = 10;
   private static final int MAX_Y_DIST = 7;

   public static OneShot<PathfinderMob> create(float param0) {
      return create(param0, 10, 7);
   }

   public static OneShot<PathfinderMob> create(float param0, int param1, int param2) {
      return BehaviorBuilder.create(
         l_param3 -> l_param3.group(l_param3.absent(MemoryModuleType.WALK_TARGET)).apply(l_param3, l2_param3 -> (l3_param4, l3_param5, l3_param6) -> {
            BlockPos l3_var8 = l3_param5.blockPosition();
            Vec3 l3_var9;
            if (l3_param4.isVillage(l3_var8)) {
               l3_var9 = LandRandomPos.getPos(l3_param5, param1, param2);
            } else {
               SectionPos l3_var10 = SectionPos.of(l3_var8);
               SectionPos l3_var11 = BehaviorUtils.findSectionClosestToVillage(l3_param4, l3_var10, 2);
               if (l3_var11 != l3_var10) {
                  l3_var9 = DefaultRandomPos.getPosTowards(l3_param5, param1, param2, Vec3.atBottomCenterOf(l3_var11.center()), (float) (Math.PI / 2));
               } else {
                  l3_var9 = LandRandomPos.getPos(l3_param5, param1, param2);
               }
            }

            l2_param3.setOrErase(Optional.ofNullable(l3_var9).map(l4_param1 -> new WalkTarget(l4_param1, param0, 0)));
            return true;
         })
      );
   }
}

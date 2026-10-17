package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class SetWalkTargetAwayFrom {
   public static BehaviorControl<PathfinderMob> pos(MemoryModuleType<BlockPos> param0, float param1, int param2, boolean param3) {
      return create(param0, param1, param2, param3, Vec3::atBottomCenterOf);
   }

   public static OneShot<PathfinderMob> entity(MemoryModuleType<? extends Entity> param0, float param1, int param2, boolean param3) {
      return create(param0, param1, param2, param3, Entity::position);
   }

   private static <T> OneShot<PathfinderMob> create(MemoryModuleType<T> param0, float param1, int param2, boolean param3, Function<T, Vec3> param4) {
      return BehaviorBuilder.create(
         l_param5 -> l_param5.group(l_param5.registered(MemoryModuleType.WALK_TARGET), l_param5.present(param0))
            .apply(l_param5, (l2_param5, l2_param6) -> (l3_param7, l3_param8, l3_param9) -> {
               Optional<WalkTarget> l3_var11 = l_param5.tryGet(l2_param5);
               if (l3_var11.isPresent() && !param3) {
                  return false;
               }

               Vec3 l3_var12 = l3_param8.position();
               Vec3 l3_var13 = param4.apply(l_param5.get(l2_param6));
               if (!l3_var12.closerThan(l3_var13, param2)) {
                  return false;
               }

               if (l3_var11.isPresent() && l3_var11.get().getSpeedModifier() == param1) {
                  Vec3 l3_var14 = l3_var11.get().getTarget().currentPosition().subtract(l3_var12);
                  Vec3 l3_var15 = l3_var13.subtract(l3_var12);
                  if (l3_var14.dot(l3_var15) < 0.0) {
                     return false;
                  }
               }

               for (int l3_var14 = 0; l3_var14 < 10; l3_var14++) {
                  Vec3 l3_var15 = LandRandomPos.getPosAway(l3_param8, 16, 7, l3_var13);
                  if (l3_var15 != null) {
                     l2_param5.set(new WalkTarget(l3_var15, param1, 0));
                     break;
                  }
               }

               return true;
            })
      );
   }
}

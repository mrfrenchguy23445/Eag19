package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class RandomStroll {
   private static final int MAX_XZ_DIST = 10;
   private static final int MAX_Y_DIST = 7;
   private static final int[][] SWIM_XY_DISTANCE_TIERS = new int[][]{{1, 1}, {3, 3}, {5, 5}, {6, 5}, {7, 7}, {10, 7}};

   public static OneShot<PathfinderMob> stroll(float param0) {
      return stroll(param0, true);
   }

   public static OneShot<PathfinderMob> stroll(float param0, boolean param1) {
      return strollFlyOrSwim(param0, l_param0 -> LandRandomPos.getPos(l_param0, 10, 7), param1 ? l_param0 -> true : l_param0 -> !l_param0.isInWaterOrBubble());
   }

   public static BehaviorControl<PathfinderMob> stroll(float param0, int param1, int param2) {
      return strollFlyOrSwim(param0, l_param2 -> LandRandomPos.getPos(l_param2, param1, param2), l_param0 -> true);
   }

   public static BehaviorControl<PathfinderMob> fly(float param0) {
      return strollFlyOrSwim(param0, l_param0 -> getTargetFlyPos(l_param0, 10, 7), l_param0 -> true);
   }

   public static BehaviorControl<PathfinderMob> swim(float param0) {
      return strollFlyOrSwim(param0, RandomStroll::getTargetSwimPos, Entity::isInWaterOrBubble);
   }

   private static OneShot<PathfinderMob> strollFlyOrSwim(float param0, Function<PathfinderMob, Vec3> param1, Predicate<PathfinderMob> param2) {
      return BehaviorBuilder.create(
         l_param3 -> l_param3.group(l_param3.absent(MemoryModuleType.WALK_TARGET)).apply(l_param3, l2_param3 -> (l3_param4, l3_param5, l3_param6) -> {
            if (!param2.test(l3_param5)) {
               return false;
            }

            Optional<Vec3> l3_var8 = Optional.ofNullable(param1.apply(l3_param5));
            l2_param3.setOrErase(l3_var8.map(l4_param1 -> new WalkTarget(l4_param1, param0, 0)));
            return true;
         })
      );
   }

   @Nullable
   private static Vec3 getTargetSwimPos(PathfinderMob param0) {
      Vec3 var1 = null;
      Vec3 var2 = null;

      for (int[] var6 : SWIM_XY_DISTANCE_TIERS) {
         if (var1 == null) {
            var2 = BehaviorUtils.getRandomSwimmablePos(param0, var6[0], var6[1]);
         } else {
            var2 = param0.position().add(param0.position().vectorTo(var1).normalize().multiply(var6[0], var6[1], var6[0]));
         }

         if (var2 == null || param0.level.getFluidState(BlockPos.containing(var2)).isEmpty()) {
            return var1;
         }

         var1 = var2;
      }

      return var2;
   }

   @Nullable
   private static Vec3 getTargetFlyPos(PathfinderMob param0, int param1, int param2) {
      Vec3 var3 = param0.getViewVector(0.0F);
      return AirAndWaterRandomPos.getPos(param0, param1, param2, -2, var3.x, var3.z, (float) (Math.PI / 2));
   }
}

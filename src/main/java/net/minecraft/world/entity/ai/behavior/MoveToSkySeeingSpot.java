package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class MoveToSkySeeingSpot {
   public static OneShot<LivingEntity> create(float param0) {
      return BehaviorBuilder.create(
         l_param1 -> l_param1.group(l_param1.absent(MemoryModuleType.WALK_TARGET)).apply(l_param1, l2_param1 -> (l3_param2, l3_param3, l3_param4) -> {
            if (l3_param2.canSeeSky(l3_param3.blockPosition())) {
               return false;
            }

            Optional<Vec3> l3_var6 = Optional.ofNullable(getOutdoorPosition(l3_param2, l3_param3));
            l3_var6.ifPresent(l4_param2 -> l2_param1.set(new WalkTarget(l4_param2, param0, 0)));
            return true;
         })
      );
   }

   @Nullable
   private static Vec3 getOutdoorPosition(ServerLevel param0, LivingEntity param1) {
      RandomSource var2 = param1.getRandom();
      BlockPos var3 = param1.blockPosition();

      for (int var4 = 0; var4 < 10; var4++) {
         BlockPos var5 = var3.offset(var2.nextInt(20) - 10, var2.nextInt(6) - 3, var2.nextInt(20) - 10);
         if (hasNoBlocksAbove(param0, param1, var5)) {
            return Vec3.atBottomCenterOf(var5);
         }
      }

      return null;
   }

   public static boolean hasNoBlocksAbove(ServerLevel param0, LivingEntity param1, BlockPos param2) {
      return param0.canSeeSky(param2) && param0.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, param2).getY() <= param1.getY();
   }
}

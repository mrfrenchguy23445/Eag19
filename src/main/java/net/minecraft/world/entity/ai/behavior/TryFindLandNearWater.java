package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.apache.commons.lang3.mutable.MutableLong;

public class TryFindLandNearWater {
   public static BehaviorControl<PathfinderMob> create(int param0, float param1) {
      MutableLong var2 = new MutableLong(0L);
      return BehaviorBuilder.create(
         l_param3 -> l_param3.group(
               l_param3.absent(MemoryModuleType.ATTACK_TARGET),
               l_param3.absent(MemoryModuleType.WALK_TARGET),
               l_param3.registered(MemoryModuleType.LOOK_TARGET)
            )
            .apply(
               l_param3,
               (l2_param3, l2_param4, l2_param5) -> (l3_param5, l3_param6, l3_param7) -> {
                  if (l3_param5.getFluidState(l3_param6.blockPosition()).is(FluidTags.WATER)) {
                     return false;
                  }

                  if (l3_param7 < var2.getValue()) {
                     var2.setValue(l3_param7 + 40L);
                     return true;
                  }

                  CollisionContext l3_var9 = CollisionContext.of(l3_param6);
                  BlockPos l3_var10 = l3_param6.blockPosition();
                  BlockPos.MutableBlockPos l3_var11 = new BlockPos.MutableBlockPos();

                  label45:
                  for (BlockPos l3_var13 : BlockPos.withinManhattan(l3_var10, param0, param0, param0)) {
                     if ((l3_var13.getX() != l3_var10.getX() || l3_var13.getZ() != l3_var10.getZ())
                        && l3_param5.getBlockState(l3_var13).getCollisionShape(l3_param5, l3_var13, l3_var9).isEmpty()
                        && !l3_param5.getBlockState(l3_var11.setWithOffset(l3_var13, Direction.DOWN)).getCollisionShape(l3_param5, l3_var13, l3_var9).isEmpty()
                        )
                      {
                        for (Direction l3_var15 : Direction.Plane.HORIZONTAL) {
                           l3_var11.setWithOffset(l3_var13, l3_var15);
                           if (l3_param5.getBlockState(l3_var11).isAir() && l3_param5.getBlockState(l3_var11.move(Direction.DOWN)).is(Blocks.WATER)) {
                              l2_param5.set(new BlockPosTracker(l3_var13));
                              l2_param4.set(new WalkTarget(new BlockPosTracker(l3_var13), param1, 0));
                              break label45;
                           }
                        }
                     }
                  }

                  var2.setValue(l3_param7 + 40L);
                  return true;
               }
            )
      );
   }
}

package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableLong;

public class TryFindWater {
   public static BehaviorControl<PathfinderMob> create(int param0, float param1) {
      MutableLong var2 = new MutableLong(0L);
      return BehaviorBuilder.create(
         l_param3 -> l_param3.group(
               l_param3.absent(MemoryModuleType.ATTACK_TARGET),
               l_param3.absent(MemoryModuleType.WALK_TARGET),
               l_param3.registered(MemoryModuleType.LOOK_TARGET)
            )
            .apply(l_param3, (l2_param3, l2_param4, l2_param5) -> (l3_param5, l3_param6, l3_param7) -> {
               if (l3_param5.getFluidState(l3_param6.blockPosition()).is(FluidTags.WATER)) {
                  return false;
               }

               if (l3_param7 < var2.getValue()) {
                  var2.setValue(l3_param7 + 20L + 2L);
                  return true;
               }

               BlockPos l3_var9 = null;
               BlockPos l3_var10 = null;
               BlockPos l3_var11 = l3_param6.blockPosition();

               for (BlockPos l3_var14 : BlockPos.withinManhattan(l3_var11, param0, param0, param0)) {
                  if (l3_var14.getX() != l3_var11.getX() || l3_var14.getZ() != l3_var11.getZ()) {
                     BlockState l3_var15 = l3_param6.level.getBlockState(l3_var14.above());
                     BlockState l3_var16 = l3_param6.level.getBlockState(l3_var14);
                     if (l3_var16.is(Blocks.WATER)) {
                        if (l3_var15.isAir()) {
                           l3_var9 = l3_var14.immutable();
                           break;
                        }

                        if (l3_var10 == null && !l3_var14.closerToCenterThan(l3_param6.position(), 1.5)) {
                           l3_var10 = l3_var14.immutable();
                        }
                     }
                  }
               }

               if (l3_var9 == null) {
                  l3_var9 = l3_var10;
               }

               if (l3_var9 != null) {
                  l2_param5.set(new BlockPosTracker(l3_var9));
                  l2_param4.set(new WalkTarget(new BlockPosTracker(l3_var9), param1, 0));
               }

               var2.setValue(l3_param7 + 40L);
               return true;
            })
      );
   }
}

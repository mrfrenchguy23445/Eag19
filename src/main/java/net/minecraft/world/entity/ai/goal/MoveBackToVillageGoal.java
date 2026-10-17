package net.minecraft.world.entity.ai.goal;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class MoveBackToVillageGoal extends RandomStrollGoal {
   private static final int MAX_XZ_DIST = 10;
   private static final int MAX_Y_DIST = 7;

   public MoveBackToVillageGoal(PathfinderMob param0, double param1, boolean param3) {
      super(param0, param1, 10, param3);
   }

   @Override
   public boolean canUse() {
      ServerLevel var1 = (ServerLevel)this.mob.level;
      BlockPos var2 = this.mob.blockPosition();
      return var1.isVillage(var2) ? false : super.canUse();
   }

   @Nullable
   @Override
   protected Vec3 getPosition() {
      ServerLevel var1 = (ServerLevel)this.mob.level;
      BlockPos var2 = this.mob.blockPosition();
      SectionPos var3 = SectionPos.of(var2);
      SectionPos var4 = BehaviorUtils.findSectionClosestToVillage(var1, var3, 2);
      return var4 != var3 ? DefaultRandomPos.getPosTowards(this.mob, 10, 7, Vec3.atBottomCenterOf(var4.center()), (float) (Math.PI / 2)) : null;
   }
}

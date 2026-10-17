package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class StrollThroughVillageGoal extends Goal {
   private static final int DISTANCE_THRESHOLD = 10;
   private final PathfinderMob mob;
   private final int interval;
   @Nullable
   private BlockPos wantedPos;

   public StrollThroughVillageGoal(PathfinderMob param0, int param1) {
      this.mob = param0;
      this.interval = reducedTickDelay(param1);
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      if (this.mob.isVehicle()) {
         return false;
      }

      if (this.mob.level.isDay()) {
         return false;
      }

      if (this.mob.getRandom().nextInt(this.interval) != 0) {
         return false;
      }

      ServerLevel var1 = (ServerLevel)this.mob.level;
      BlockPos var2 = this.mob.blockPosition();
      if (!var1.isCloseToVillage(var2, 6)) {
         return false;
      }

      Vec3 var3 = LandRandomPos.getPos(this.mob, 15, 7, l_param1 -> -var1.sectionsToVillage(SectionPos.of(l_param1)));
      this.wantedPos = var3 == null ? null : BlockPos.containing(var3);
      return this.wantedPos != null;
   }

   @Override
   public boolean canContinueToUse() {
      return this.wantedPos != null && !this.mob.getNavigation().isDone() && this.mob.getNavigation().getTargetPos().equals(this.wantedPos);
   }

   @Override
   public void tick() {
      if (this.wantedPos != null) {
         PathNavigation var1 = this.mob.getNavigation();
         if (var1.isDone() && !this.wantedPos.closerToCenterThan(this.mob.position(), 10.0)) {
            Vec3 var2 = Vec3.atBottomCenterOf(this.wantedPos);
            Vec3 var3 = this.mob.position();
            Vec3 var4 = var3.subtract(var2);
            var2 = var4.scale(0.4).add(var2);
            Vec3 var5 = var2.subtract(var3).normalize().scale(10.0).add(var3);
            BlockPos var6 = BlockPos.containing(var5);
            var6 = this.mob.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, var6);
            if (!var1.moveTo(var6.getX(), var6.getY(), var6.getZ(), 1.0)) {
               this.moveRandomly();
            }
         }
      }
   }

   private void moveRandomly() {
      RandomSource var1 = this.mob.getRandom();
      BlockPos var2 = this.mob
         .level
         .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.mob.blockPosition().offset(-8 + var1.nextInt(16), 0, -8 + var1.nextInt(16)));
      this.mob.getNavigation().moveTo(var2.getX(), var2.getY(), var2.getZ(), 1.0);
   }
}

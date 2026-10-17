package net.minecraft.world.entity.ai.navigation;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

public class WallClimberNavigation extends GroundPathNavigation {
   @Nullable
   private BlockPos pathToPosition;

   public WallClimberNavigation(Mob param0, Level param1) {
      super(param0, param1);
   }

   @Override
   public Path createPath(BlockPos param0, int param1) {
      this.pathToPosition = param0;
      return super.createPath(param0, param1);
   }

   @Override
   public Path createPath(Entity param0, int param1) {
      this.pathToPosition = param0.blockPosition();
      return super.createPath(param0, param1);
   }

   @Override
   public boolean moveTo(Entity param0, double param1) {
      Path var4 = this.createPath(param0, 0);
      if (var4 != null) {
         return this.moveTo(var4, param1);
      }

      this.pathToPosition = param0.blockPosition();
      this.speedModifier = param1;
      return true;
   }

   @Override
   public void tick() {
      if (!this.isDone()) {
         super.tick();
      } else {
         if (this.pathToPosition != null) {
            if (!this.pathToPosition.closerToCenterThan(this.mob.position(), this.mob.getBbWidth())
               && (
                  !(this.mob.getY() > this.pathToPosition.getY())
                     || !BlockPos.containing(this.pathToPosition.getX(), this.mob.getY(), this.pathToPosition.getZ())
                        .closerToCenterThan(this.mob.position(), this.mob.getBbWidth())
               )) {
               this.mob
                  .getMoveControl()
                  .setWantedPosition(this.pathToPosition.getX(), this.pathToPosition.getY(), this.pathToPosition.getZ(), this.speedModifier);
            } else {
               this.pathToPosition = null;
            }
         }
      }
   }
}

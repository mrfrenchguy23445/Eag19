package net.minecraft.world.entity.ai.control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MoveControl implements Control {
   public static final float MIN_SPEED = 5.0E-4F;
   public static final float MIN_SPEED_SQR = 2.5000003E-7F;
   protected static final int MAX_TURN = 90;
   protected final Mob mob;
   protected double wantedX;
   protected double wantedY;
   protected double wantedZ;
   protected double speedModifier;
   protected float strafeForwards;
   protected float strafeRight;
   protected MoveControl.Operation operation = MoveControl.Operation.WAIT;

   public MoveControl(Mob param0) {
      this.mob = param0;
   }

   public boolean hasWanted() {
      return this.operation == MoveControl.Operation.MOVE_TO;
   }

   public double getSpeedModifier() {
      return this.speedModifier;
   }

   public void setWantedPosition(double param0, double param2, double param4, double param6) {
      this.wantedX = param0;
      this.wantedY = param2;
      this.wantedZ = param4;
      this.speedModifier = param6;
      if (this.operation != MoveControl.Operation.JUMPING) {
         this.operation = MoveControl.Operation.MOVE_TO;
      }
   }

   public void strafe(float param0, float param1) {
      this.operation = MoveControl.Operation.STRAFE;
      this.strafeForwards = param0;
      this.strafeRight = param1;
      this.speedModifier = 0.25;
   }

   public void tick() {
      if (this.operation == MoveControl.Operation.STRAFE) {
         float var1 = (float)this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
         float var2 = (float)this.speedModifier * var1;
         float var3 = this.strafeForwards;
         float var4 = this.strafeRight;
         float var5 = Mth.sqrt(var3 * var3 + var4 * var4);
         if (var5 < 1.0F) {
            var5 = 1.0F;
         }

         var5 = var2 / var5;
         var3 *= var5;
         var4 *= var5;
         float var6 = Mth.sin(this.mob.getYRot() * (float) (Math.PI / 180.0));
         float var7 = Mth.cos(this.mob.getYRot() * (float) (Math.PI / 180.0));
         float var8 = var3 * var7 - var4 * var6;
         float var9 = var4 * var7 + var3 * var6;
         if (!this.isWalkable(var8, var9)) {
            this.strafeForwards = 1.0F;
            this.strafeRight = 0.0F;
         }

         this.mob.setSpeed(var2);
         this.mob.setZza(this.strafeForwards);
         this.mob.setXxa(this.strafeRight);
         this.operation = MoveControl.Operation.WAIT;
      } else if (this.operation == MoveControl.Operation.MOVE_TO) {
         this.operation = MoveControl.Operation.WAIT;
         double var1 = this.wantedX - this.mob.getX();
         double var3 = this.wantedZ - this.mob.getZ();
         double var5 = this.wantedY - this.mob.getY();
         double var7 = var1 * var1 + var5 * var5 + var3 * var3;
         if (var7 < 2.5000003E-7F) {
            this.mob.setZza(0.0F);
            return;
         }

         float var9 = (float)(Mth.atan2(var3, var1) * 180.0F / (float)Math.PI) - 90.0F;
         this.mob.setYRot(this.rotlerp(this.mob.getYRot(), var9, 90.0F));
         this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
         BlockPos var10 = this.mob.blockPosition();
         BlockState var11 = this.mob.level.getBlockState(var10);
         VoxelShape var12 = var11.getCollisionShape(this.mob.level, var10);
         if (var5 > this.mob.maxUpStep() && var1 * var1 + var3 * var3 < Math.max(1.0F, this.mob.getBbWidth())
            || !var12.isEmpty() && this.mob.getY() < var12.max(Direction.Axis.Y) + var10.getY() && !var11.is(BlockTags.DOORS) && !var11.is(BlockTags.FENCES)) {
            this.mob.getJumpControl().jump();
            this.operation = MoveControl.Operation.JUMPING;
         }
      } else if (this.operation == MoveControl.Operation.JUMPING) {
         this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
         if (this.mob.isOnGround()) {
            this.operation = MoveControl.Operation.WAIT;
         }
      } else {
         this.mob.setZza(0.0F);
      }
   }

   private boolean isWalkable(float param0, float param1) {
      PathNavigation var3 = this.mob.getNavigation();
      if (var3 != null) {
         NodeEvaluator var4 = var3.getNodeEvaluator();
         if (var4 != null
            && var4.getBlockPathType(this.mob.level, Mth.floor(this.mob.getX() + param0), this.mob.getBlockY(), Mth.floor(this.mob.getZ() + param1))
               != BlockPathTypes.WALKABLE) {
            return false;
         }
      }

      return true;
   }

   protected float rotlerp(float param0, float param1, float param2) {
      float var4 = Mth.wrapDegrees(param1 - param0);
      if (var4 > param2) {
         var4 = param2;
      }

      if (var4 < -param2) {
         var4 = -param2;
      }

      float var5 = param0 + var4;
      if (var5 < 0.0F) {
         var5 += 360.0F;
      } else if (var5 > 360.0F) {
         var5 -= 360.0F;
      }

      return var5;
   }

   public double getWantedX() {
      return this.wantedX;
   }

   public double getWantedY() {
      return this.wantedY;
   }

   public double getWantedZ() {
      return this.wantedZ;
   }

   protected enum Operation {
      WAIT,
      MOVE_TO,
      STRAFE,
      JUMPING;
   }
}

package net.minecraft.client.renderer.item;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class CompassItemPropertyFunction implements ClampedItemPropertyFunction {
   public static final int DEFAULT_ROTATION = 0;
   private final CompassItemPropertyFunction.CompassWobble wobble = new CompassItemPropertyFunction.CompassWobble();
   private final CompassItemPropertyFunction.CompassWobble wobbleRandom = new CompassItemPropertyFunction.CompassWobble();
   public final CompassItemPropertyFunction.CompassTarget compassTarget;

   public CompassItemPropertyFunction(CompassItemPropertyFunction.CompassTarget param0) {
      this.compassTarget = param0;
   }

   @Override
   public float unclampedCall(ItemStack param0, @Nullable ClientLevel param1, @Nullable LivingEntity param2, int param3) {
      Entity var5 = param2 != null ? param2 : param0.getEntityRepresentation();
      if (var5 == null) {
         return 0.0F;
      }

      param1 = this.tryFetchLevelIfMissing(var5, param1);
      return param1 == null ? 0.0F : this.getCompassRotation(param0, param1, param3, var5);
   }

   private float getCompassRotation(ItemStack param0, ClientLevel param1, int param2, Entity param3) {
      GlobalPos var5 = this.compassTarget.getPos(param1, param0, param3);
      long var6 = param1.getGameTime();
      return !this.isValidCompassTargetPos(param3, var5)
         ? this.getRandomlySpinningRotation(param2, var6)
         : this.getRotationTowardsCompassTarget(param3, var6, var5.pos());
   }

   private float getRandomlySpinningRotation(int param0, long param1) {
      if (this.wobbleRandom.shouldUpdate(param1)) {
         this.wobbleRandom.update(param1, Math.random());
      }

      double var4 = this.wobbleRandom.rotation + this.hash(param0) / 2.1474836E9F;
      return Mth.positiveModulo((float)var4, 1.0F);
   }

   private float getRotationTowardsCompassTarget(Entity param0, long param1, BlockPos param3) {
      double var5 = this.getAngleFromEntityToPos(param0, param3);
      double var7 = this.getWrappedVisualRotationY(param0);
      double var9;
      if (param0 instanceof Player var11 && var11.isLocalPlayer()) {
         if (this.wobble.shouldUpdate(param1)) {
            this.wobble.update(param1, 0.5 - (var7 - 0.25));
         }

         var9 = var5 + this.wobble.rotation;
      } else {
         var9 = 0.5 - (var7 - 0.25 - var5);
      }

      return Mth.positiveModulo((float)var9, 1.0F);
   }

   @Nullable
   private ClientLevel tryFetchLevelIfMissing(Entity param0, @Nullable ClientLevel param1) {
      return param1 == null && param0.level instanceof ClientLevel ? (ClientLevel)param0.level : param1;
   }

   private boolean isValidCompassTargetPos(Entity param0, @Nullable GlobalPos param1) {
      return param1 != null && param1.dimension() == param0.level.dimension() && !(param1.pos().distToCenterSqr(param0.position()) < 1.0E-5F);
   }

   private double getAngleFromEntityToPos(Entity param0, BlockPos param1) {
      Vec3 var3 = Vec3.atCenterOf(param1);
      return Math.atan2(var3.z() - param0.getZ(), var3.x() - param0.getX()) / (float) (Math.PI * 2);
   }

   private double getWrappedVisualRotationY(Entity param0) {
      return Mth.positiveModulo(param0.getVisualRotationYInDegrees() / 360.0F, 1.0);
   }

   private int hash(int param0) {
      return param0 * 1327217883;
   }

   public interface CompassTarget {
      @Nullable
      GlobalPos getPos(ClientLevel var1, ItemStack var2, Entity var3);
   }

   static class CompassWobble {
      double rotation;
      private double deltaRotation;
      private long lastUpdateTick;

      boolean shouldUpdate(long param0) {
         return this.lastUpdateTick != param0;
      }

      void update(long param0, double param2) {
         this.lastUpdateTick = param0;
         double var5 = param2 - this.rotation;
         var5 = Mth.positiveModulo(var5 + 0.5, 1.0) - 0.5;
         this.deltaRotation += var5 * 0.1;
         this.deltaRotation *= 0.8;
         this.rotation = Mth.positiveModulo(this.rotation + this.deltaRotation, 1.0);
      }
   }
}

package net.minecraft.world.entity.projectile;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ProjectileUtil {
   public static HitResult getHitResult(Entity param0, Predicate<Entity> param1) {
      Vec3 var2 = param0.getDeltaMovement();
      Level var3 = param0.level;
      Vec3 var4 = param0.position();
      Vec3 var5 = var4.add(var2);
      HitResult var6 = var3.clip(new ClipContext(var4, var5, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, param0));
      if (var6.getType() != HitResult.Type.MISS) {
         var5 = var6.getLocation();
      }

      HitResult var7 = getEntityHitResult(var3, param0, var4, var5, param0.getBoundingBox().expandTowards(param0.getDeltaMovement()).inflate(1.0), param1);
      if (var7 != null) {
         var6 = var7;
      }

      return var6;
   }

   @Nullable
   public static EntityHitResult getEntityHitResult(Entity param0, Vec3 param1, Vec3 param2, AABB param3, Predicate<Entity> param4, double param5) {
      Level var7 = param0.level;
      double var8 = param5;
      Entity var10 = null;
      Vec3 var11 = null;

      for (Entity var13 : var7.getEntities(param0, param3, param4)) {
         AABB var14 = var13.getBoundingBox().inflate(var13.getPickRadius());
         Optional<Vec3> var15 = var14.clip(param1, param2);
         if (var14.contains(param1)) {
            if (var8 >= 0.0) {
               var10 = var13;
               var11 = var15.orElse(param1);
               var8 = 0.0;
            }
         } else if (var15.isPresent()) {
            Vec3 var16 = var15.get();
            double var17 = param1.distanceToSqr(var16);
            if (var17 < var8 || var8 == 0.0) {
               if (var13.getRootVehicle() == param0.getRootVehicle()) {
                  if (var8 == 0.0) {
                     var10 = var13;
                     var11 = var16;
                  }
               } else {
                  var10 = var13;
                  var11 = var16;
                  var8 = var17;
               }
            }
         }
      }

      return var10 == null ? null : new EntityHitResult(var10, var11);
   }

   @Nullable
   public static EntityHitResult getEntityHitResult(Level param0, Entity param1, Vec3 param2, Vec3 param3, AABB param4, Predicate<Entity> param5) {
      return getEntityHitResult(param0, param1, param2, param3, param4, param5, 0.3F);
   }

   @Nullable
   public static EntityHitResult getEntityHitResult(Level param0, Entity param1, Vec3 param2, Vec3 param3, AABB param4, Predicate<Entity> param5, float param6) {
      double var7 = Double.MAX_VALUE;
      Entity var9 = null;

      for (Entity var11 : param0.getEntities(param1, param4, param5)) {
         AABB var12 = var11.getBoundingBox().inflate(param6);
         Optional<Vec3> var13 = var12.clip(param2, param3);
         if (var13.isPresent()) {
            double var14 = param2.distanceToSqr(var13.get());
            if (var14 < var7) {
               var9 = var11;
               var7 = var14;
            }
         }
      }

      return var9 == null ? null : new EntityHitResult(var9);
   }

   public static void rotateTowardsMovement(Entity param0, float param1) {
      Vec3 var2 = param0.getDeltaMovement();
      if (var2.lengthSqr() != 0.0) {
         double var3 = var2.horizontalDistance();
         param0.setYRot((float)(Mth.atan2(var2.z, var2.x) * 180.0F / (float)Math.PI) + 90.0F);
         param0.setXRot((float)(Mth.atan2(var3, var2.y) * 180.0F / (float)Math.PI) - 90.0F);

         while (param0.getXRot() - param0.xRotO < -180.0F) {
            param0.xRotO -= 360.0F;
         }

         while (param0.getXRot() - param0.xRotO >= 180.0F) {
            param0.xRotO += 360.0F;
         }

         while (param0.getYRot() - param0.yRotO < -180.0F) {
            param0.yRotO -= 360.0F;
         }

         while (param0.getYRot() - param0.yRotO >= 180.0F) {
            param0.yRotO += 360.0F;
         }

         param0.setXRot(Mth.lerp(param1, param0.xRotO, param0.getXRot()));
         param0.setYRot(Mth.lerp(param1, param0.yRotO, param0.getYRot()));
      }
   }

   public static InteractionHand getWeaponHoldingHand(LivingEntity param0, Item param1) {
      return param0.getMainHandItem().is(param1) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
   }

   public static AbstractArrow getMobArrow(LivingEntity param0, ItemStack param1, float param2) {
      ArrowItem var3 = (ArrowItem)(param1.getItem() instanceof ArrowItem ? param1.getItem() : Items.ARROW);
      AbstractArrow var4 = var3.createArrow(param0.level, param1, param0);
      var4.setEnchantmentEffectsFromEntity(param0, param2);
      if (param1.is(Items.TIPPED_ARROW) && var4 instanceof Arrow) {
         ((Arrow)var4).setEffectsFromItem(param1);
      }

      return var4;
   }
}

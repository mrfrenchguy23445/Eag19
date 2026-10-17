package net.minecraft.world.entity.monster;

import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public interface CrossbowAttackMob extends RangedAttackMob {
   void setChargingCrossbow(boolean var1);

   void shootCrossbowProjectile(LivingEntity var1, ItemStack var2, Projectile var3, float var4);

   @Nullable
   LivingEntity getTarget();

   void onCrossbowAttackPerformed();

   default void performCrossbowAttack(LivingEntity param0, float param1) {
      InteractionHand var3 = ProjectileUtil.getWeaponHoldingHand(param0, Items.CROSSBOW);
      ItemStack var4 = param0.getItemInHand(var3);
      if (param0.isHolding(Items.CROSSBOW)) {
         CrossbowItem.performShooting(param0.level, param0, var3, var4, param1, 14 - param0.level.getDifficulty().getId() * 4);
      }

      this.onCrossbowAttackPerformed();
   }

   default void shootCrossbowProjectile(LivingEntity param0, LivingEntity param1, Projectile param2, float param3, float param4) {
      double var6 = param1.getX() - param0.getX();
      double var8 = param1.getZ() - param0.getZ();
      double var10 = Math.sqrt(var6 * var6 + var8 * var8);
      double var12 = param1.getY(0.3333333333333333) - param2.getY() + var10 * 0.2F;
      Vector3f var14 = this.getProjectileShotVector(param0, new Vec3(var6, var12, var8), param3);
      param2.shoot(var14.x(), var14.y(), var14.z(), param4, 14 - param0.level.getDifficulty().getId() * 4);
      param0.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (param0.getRandom().nextFloat() * 0.4F + 0.8F));
   }

   default Vector3f getProjectileShotVector(LivingEntity param0, Vec3 param1, float param2) {
      Vector3f var4 = param1.toVector3f().normalize();
      Vector3f var5 = new Vector3f(var4).cross(new Vector3f(0.0F, 1.0F, 0.0F));
      if (var5.lengthSquared() <= 1.0E-7) {
         Vec3 var6 = param0.getUpVector(1.0F);
         var5 = new Vector3f(var4).cross(var6.toVector3f());
      }

      Vector3f var6 = new Vector3f(var4).rotateAxis((float) (Math.PI / 2), var5.x, var5.y, var5.z);
      return new Vector3f(var4).rotateAxis(param2 * (float) (Math.PI / 180.0), var6.x, var6.y, var6.z);
   }
}

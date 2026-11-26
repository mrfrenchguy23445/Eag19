package net.minecraft.world.entity.monster.hoglin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public interface HoglinBase {
   int ATTACK_ANIMATION_DURATION = 10;

   int getAttackAnimationRemainingTicks();

   static boolean hurtAndThrowTarget(LivingEntity param0, LivingEntity param1) {
      float var3 = (float)param0.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float var2;
      if (!param0.isBaby() && (int)var3 > 0) {
         var2 = var3 / 2.0F + param0.level.random.nextInt((int)var3);
      } else {
         var2 = var3;
      }

      boolean var4 = param1.hurt(param0.damageSources().mobAttack(param0), var2);
      if (var4) {
         param0.doEnchantDamageEffects(param0, param1);
         if (!param0.isBaby()) {
            throwTarget(param0, param1);
         }
      }

      return var4;
   }

   static void throwTarget(LivingEntity param0, LivingEntity param1) {
      double var2 = param0.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
      double var4 = param1.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      double var6 = var2 - var4;
      if (!(var6 <= 0.0)) {
         double var8 = param1.getX() - param0.getX();
         double var10 = param1.getZ() - param0.getZ();
         float var12 = param0.level.random.nextInt(21) - 10;
         double var13 = var6 * (param0.level.random.nextFloat() * 0.5F + 0.2F);
         Vec3 var15 = new Vec3(var8, 0.0, var10).normalize().scale(var13).yRot(var12);
         double var16 = var6 * param0.level.random.nextFloat() * 0.5;
         param1.push(var15.x, var16, var15.z);
         param1.hurtMarked = true;
      }
   }
}

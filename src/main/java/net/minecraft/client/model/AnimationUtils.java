package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CrossbowItem;

public class AnimationUtils {
   public static void animateCrossbowHold(ModelPart param0, ModelPart param1, ModelPart param2, boolean param3) {
      ModelPart var4 = param3 ? param0 : param1;
      ModelPart var5 = param3 ? param1 : param0;
      var4.yRot = (param3 ? -0.3F : 0.3F) + param2.yRot;
      var5.yRot = (param3 ? 0.6F : -0.6F) + param2.yRot;
      var4.xRot = (float) (-Math.PI / 2) + param2.xRot + 0.1F;
      var5.xRot = -1.5F + param2.xRot;
   }

   public static void animateCrossbowCharge(ModelPart param0, ModelPart param1, LivingEntity param2, boolean param3) {
      ModelPart var4 = param3 ? param0 : param1;
      ModelPart var5 = param3 ? param1 : param0;
      var4.yRot = param3 ? -0.8F : 0.8F;
      var4.xRot = -0.97079635F;
      var5.xRot = var4.xRot;
      float var6 = CrossbowItem.getChargeDuration(param2.getUseItem());
      float var7 = Mth.clamp(param2.getTicksUsingItem(), 0.0F, var6);
      float var8 = var7 / var6;
      var5.yRot = Mth.lerp(var8, 0.4F, 0.85F) * (param3 ? 1 : -1);
      var5.xRot = Mth.lerp(var8, var5.xRot, (float) (-Math.PI / 2));
   }

   public static <T extends Mob> void swingWeaponDown(ModelPart param0, ModelPart param1, T param2, float param3, float param4) {
      float var5 = Mth.sin(param3 * (float) Math.PI);
      float var6 = Mth.sin((1.0F - (1.0F - param3) * (1.0F - param3)) * (float) Math.PI);
      param0.zRot = 0.0F;
      param1.zRot = 0.0F;
      param0.yRot = (float) (Math.PI / 20);
      param1.yRot = (float) (-Math.PI / 20);
      if (param2.getMainArm() == HumanoidArm.RIGHT) {
         param0.xRot = -1.8849558F + Mth.cos(param4 * 0.09F) * 0.15F;
         param1.xRot = -0.0F + Mth.cos(param4 * 0.19F) * 0.5F;
         param0.xRot += var5 * 2.2F - var6 * 0.4F;
         param1.xRot += var5 * 1.2F - var6 * 0.4F;
      } else {
         param0.xRot = -0.0F + Mth.cos(param4 * 0.19F) * 0.5F;
         param1.xRot = -1.8849558F + Mth.cos(param4 * 0.09F) * 0.15F;
         param0.xRot += var5 * 1.2F - var6 * 0.4F;
         param1.xRot += var5 * 2.2F - var6 * 0.4F;
      }

      bobArms(param0, param1, param4);
   }

   public static void bobModelPart(ModelPart param0, float param1, float param2) {
      param0.zRot = param0.zRot + param2 * (Mth.cos(param1 * 0.09F) * 0.05F + 0.05F);
      param0.xRot = param0.xRot + param2 * (Mth.sin(param1 * 0.067F) * 0.05F);
   }

   public static void bobArms(ModelPart param0, ModelPart param1, float param2) {
      bobModelPart(param0, param2, 1.0F);
      bobModelPart(param1, param2, -1.0F);
   }

   public static void animateZombieArms(ModelPart param0, ModelPart param1, boolean param2, float param3, float param4) {
      float var5 = Mth.sin(param3 * (float) Math.PI);
      float var6 = Mth.sin((1.0F - (1.0F - param3) * (1.0F - param3)) * (float) Math.PI);
      param1.zRot = 0.0F;
      param0.zRot = 0.0F;
      param1.yRot = -(0.1F - var5 * 0.6F);
      param0.yRot = 0.1F - var5 * 0.6F;
      float var7 = (float) -Math.PI / (param2 ? 1.5F : 2.25F);
      param1.xRot = var7;
      param0.xRot = var7;
      param1.xRot += var5 * 1.2F - var6 * 0.4F;
      param0.xRot += var5 * 1.2F - var6 * 0.4F;
      bobArms(param1, param0, param4);
   }
}

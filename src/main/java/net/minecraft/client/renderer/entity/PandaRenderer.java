package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.model.PandaModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.PandaHoldsItemLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Panda;

public class PandaRenderer extends MobRenderer<Panda, PandaModel<Panda>> {
   private static final Map<Panda.Gene, ResourceLocation> TEXTURES = Util.make(Maps.newEnumMap(Panda.Gene.class), param0 -> {
      param0.put(Panda.Gene.NORMAL, new ResourceLocation("textures/entity/panda/panda.png"));
      param0.put(Panda.Gene.LAZY, new ResourceLocation("textures/entity/panda/lazy_panda.png"));
      param0.put(Panda.Gene.WORRIED, new ResourceLocation("textures/entity/panda/worried_panda.png"));
      param0.put(Panda.Gene.PLAYFUL, new ResourceLocation("textures/entity/panda/playful_panda.png"));
      param0.put(Panda.Gene.BROWN, new ResourceLocation("textures/entity/panda/brown_panda.png"));
      param0.put(Panda.Gene.WEAK, new ResourceLocation("textures/entity/panda/weak_panda.png"));
      param0.put(Panda.Gene.AGGRESSIVE, new ResourceLocation("textures/entity/panda/aggressive_panda.png"));
   });

   public PandaRenderer(EntityRendererProvider.Context param0) {
      super(param0, new PandaModel<>(param0.bakeLayer(ModelLayers.PANDA)), 0.9F);
      this.addLayer(new PandaHoldsItemLayer(this, param0.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(Panda param0) {
      return TEXTURES.getOrDefault(param0.getVariant(), TEXTURES.get(Panda.Gene.NORMAL));
   }

   protected void setupRotations(Panda param0, PoseStack param1, float param2, float param3, float param4) {
      super.setupRotations(param0, param1, param2, param3, param4);
      if (param0.rollCounter > 0) {
         int var6 = param0.rollCounter;
         int var7 = var6 + 1;
         float var8 = 7.0F;
         float var9 = param0.isBaby() ? 0.3F : 0.8F;
         if (var6 < 8) {
            float var11 = 90 * var6 / 7.0F;
            float var12 = 90 * var7 / 7.0F;
            float var10 = this.getAngle(var11, var12, var7, param4, 8.0F);
            param1.translate(0.0F, (var9 + 0.2F) * (var10 / 90.0F), 0.0F);
            param1.mulPose(Axis.XP.rotationDegrees(-var10));
         } else if (var6 < 16) {
            float var11 = (var6 - 8.0F) / 7.0F;
            float var12 = 90.0F + 90.0F * var11;
            float var13 = 90.0F + 90.0F * (var7 - 8.0F) / 7.0F;
            float var10 = this.getAngle(var12, var13, var7, param4, 16.0F);
            param1.translate(0.0F, var9 + 0.2F + (var9 - 0.2F) * (var10 - 90.0F) / 90.0F, 0.0F);
            param1.mulPose(Axis.XP.rotationDegrees(-var10));
         } else if (var6 < 24.0F) {
            float var11 = (var6 - 16.0F) / 7.0F;
            float var12 = 180.0F + 90.0F * var11;
            float var13 = 180.0F + 90.0F * (var7 - 16.0F) / 7.0F;
            float var10 = this.getAngle(var12, var13, var7, param4, 24.0F);
            param1.translate(0.0F, var9 + var9 * (270.0F - var10) / 90.0F, 0.0F);
            param1.mulPose(Axis.XP.rotationDegrees(-var10));
         } else if (var6 < 32) {
            float var11 = (var6 - 24.0F) / 7.0F;
            float var12 = 270.0F + 90.0F * var11;
            float var13 = 270.0F + 90.0F * (var7 - 24.0F) / 7.0F;
            float var10 = this.getAngle(var12, var13, var7, param4, 32.0F);
            param1.translate(0.0F, var9 * ((360.0F - var10) / 90.0F), 0.0F);
            param1.mulPose(Axis.XP.rotationDegrees(-var10));
         }
      }

      float var6 = param0.getSitAmount(param4);
      if (var6 > 0.0F) {
         param1.translate(0.0F, 0.8F * var6, 0.0F);
         param1.mulPose(Axis.XP.rotationDegrees(Mth.lerp(var6, param0.getXRot(), param0.getXRot() + 90.0F)));
         param1.translate(0.0F, -1.0F * var6, 0.0F);
         if (param0.isScared()) {
            float var7 = (float)(Math.cos(param0.tickCount * 1.25) * Math.PI * 0.05F);
            param1.mulPose(Axis.YP.rotationDegrees(var7));
            if (param0.isBaby()) {
               param1.translate(0.0F, 0.8F, 0.55F);
            }
         }
      }

      float var7 = param0.getLieOnBackAmount(param4);
      if (var7 > 0.0F) {
         float var8 = param0.isBaby() ? 0.5F : 1.3F;
         param1.translate(0.0F, var8 * var7, 0.0F);
         param1.mulPose(Axis.XP.rotationDegrees(Mth.lerp(var7, param0.getXRot(), param0.getXRot() + 180.0F)));
      }
   }

   private float getAngle(float param0, float param1, int param2, float param3, float param4) {
      return param2 < param4 ? Mth.lerp(param3, param0, param1) : param0;
   }
}

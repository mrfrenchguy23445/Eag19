package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CapeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public CapeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> param0) {
      super(param0);
   }

   public void render(
      PoseStack param0,
      MultiBufferSource param1,
      int param2,
      AbstractClientPlayer param3,
      float param4,
      float param5,
      float param6,
      float param7,
      float param8,
      float param9
   ) {
      if (param3.isCapeLoaded() && !param3.isInvisible() && param3.isModelPartShown(PlayerModelPart.CAPE) && param3.getCloakTextureLocation() != null) {
         ItemStack var11 = param3.getItemBySlot(EquipmentSlot.CHEST);
         if (!var11.is(Items.ELYTRA)) {
            param0.pushPose();
            param0.translate(0.0F, 0.0F, 0.125F);
            double var12 = Mth.lerp(param6, param3.xCloakO, param3.xCloak) - Mth.lerp(param6, param3.xo, param3.getX());
            double var14 = Mth.lerp(param6, param3.yCloakO, param3.yCloak) - Mth.lerp(param6, param3.yo, param3.getY());
            double var16 = Mth.lerp(param6, param3.zCloakO, param3.zCloak) - Mth.lerp(param6, param3.zo, param3.getZ());
            float var18 = Mth.rotLerp(param6, param3.yBodyRotO, param3.yBodyRot);
            double var19 = Mth.sin(var18 * (float) (Math.PI / 180.0));
            double var21 = -Mth.cos(var18 * (float) (Math.PI / 180.0));
            float var23 = (float)var14 * 10.0F;
            var23 = Mth.clamp(var23, -6.0F, 32.0F);
            float var24 = (float)(var12 * var19 + var16 * var21) * 100.0F;
            var24 = Mth.clamp(var24, 0.0F, 150.0F);
            float var25 = (float)(var12 * var21 - var16 * var19) * 100.0F;
            var25 = Mth.clamp(var25, -20.0F, 20.0F);
            if (var24 < 0.0F) {
               var24 = 0.0F;
            }

            float var26 = Mth.lerp(param6, param3.oBob, param3.bob);
            var23 += Mth.sin(Mth.lerp(param6, param3.walkDistO, param3.walkDist) * 6.0F) * 32.0F * var26;
            if (param3.isCrouching()) {
               var23 += 25.0F;
            }

            param0.mulPose(Axis.XP.rotationDegrees(6.0F + var24 / 2.0F + var23));
            param0.mulPose(Axis.ZP.rotationDegrees(var25 / 2.0F));
            param0.mulPose(Axis.YP.rotationDegrees(180.0F - var25 / 2.0F));
            VertexConsumer var27 = param1.getBuffer(RenderType.entitySolid(param3.getCloakTextureLocation()));
            this.getParentModel().renderCloak(param0, var27, param2, OverlayTexture.NO_OVERLAY);
            param0.popPose();
         }
      }
   }
}

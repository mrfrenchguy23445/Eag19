package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.Mth;

public class Deadmau5EarsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public Deadmau5EarsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> param0) {
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
      if ("deadmau5".equals(param3.getName().getString()) && param3.isSkinLoaded() && !param3.isInvisible()) {
         VertexConsumer var11 = param1.getBuffer(RenderType.entitySolid(param3.getSkinTextureLocation()));
         int var12 = LivingEntityRenderer.getOverlayCoords(param3, 0.0F);

         for (int var13 = 0; var13 < 2; var13++) {
            float var14 = Mth.lerp(param6, param3.yRotO, param3.getYRot()) - Mth.lerp(param6, param3.yBodyRotO, param3.yBodyRot);
            float var15 = Mth.lerp(param6, param3.xRotO, param3.getXRot());
            param0.pushPose();
            param0.mulPose(Axis.YP.rotationDegrees(var14));
            param0.mulPose(Axis.XP.rotationDegrees(var15));
            param0.translate(0.375F * (var13 * 2 - 1), 0.0F, 0.0F);
            param0.translate(0.0F, -0.375F, 0.0F);
            param0.mulPose(Axis.XP.rotationDegrees(-var15));
            param0.mulPose(Axis.YP.rotationDegrees(-var14));
            float var16 = 1.3333334F;
            param0.scale(1.3333334F, 1.3333334F, 1.3333334F);
            this.getParentModel().renderEars(param0, var11, param2, var12);
            param0.popPose();
         }
      }
   }
}

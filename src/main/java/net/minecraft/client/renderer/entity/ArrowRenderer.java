package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public abstract class ArrowRenderer<T extends AbstractArrow> extends EntityRenderer<T> {
   public ArrowRenderer(EntityRendererProvider.Context param0) {
      super(param0);
   }

   public void render(T param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      param3.pushPose();
      param3.mulPose(Axis.YP.rotationDegrees(Mth.lerp(param2, param0.yRotO, param0.getYRot()) - 90.0F));
      param3.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(param2, param0.xRotO, param0.getXRot())));
      int var7 = 0;
      float var8 = 0.0F;
      float var9 = 0.5F;
      float var10 = 0.0F;
      float var11 = 0.15625F;
      float var12 = 0.0F;
      float var13 = 0.15625F;
      float var14 = 0.15625F;
      float var15 = 0.3125F;
      float var16 = 0.05625F;
      float var17 = param0.shakeTime - param2;
      if (var17 > 0.0F) {
         float var18 = -Mth.sin(var17 * 3.0F) * var17;
         param3.mulPose(Axis.ZP.rotationDegrees(var18));
      }

      param3.mulPose(Axis.XP.rotationDegrees(45.0F));
      param3.scale(0.05625F, 0.05625F, 0.05625F);
      param3.translate(-4.0F, 0.0F, 0.0F);
      VertexConsumer var18 = param4.getBuffer(RenderType.entityCutout(this.getTextureLocation(param0)));
      PoseStack.Pose var19 = param3.last();
      Matrix4f var20 = var19.pose();
      Matrix3f var21 = var19.normal();
      this.vertex(var20, var21, var18, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, param5);
      this.vertex(var20, var21, var18, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, param5);
      this.vertex(var20, var21, var18, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, param5);
      this.vertex(var20, var21, var18, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, param5);
      this.vertex(var20, var21, var18, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, param5);
      this.vertex(var20, var21, var18, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, param5);
      this.vertex(var20, var21, var18, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, param5);
      this.vertex(var20, var21, var18, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, param5);

      for (int var22 = 0; var22 < 4; var22++) {
         param3.mulPose(Axis.XP.rotationDegrees(90.0F));
         this.vertex(var20, var21, var18, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, param5);
         this.vertex(var20, var21, var18, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, param5);
         this.vertex(var20, var21, var18, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, param5);
         this.vertex(var20, var21, var18, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, param5);
      }

      param3.popPose();
      super.render(param0, param1, param2, param3, param4, param5);
   }

   public void vertex(
      Matrix4f param0,
      Matrix3f param1,
      VertexConsumer param2,
      int param3,
      int param4,
      int param5,
      float param6,
      float param7,
      int param8,
      int param9,
      int param10,
      int param11
   ) {
      param2.vertex(param0, param3, param4, param5)
         .color(255, 255, 255, 255)
         .uv(param6, param7)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(param11)
         .normal(param1, param8, param10, param9)
         .endVertex();
   }
}

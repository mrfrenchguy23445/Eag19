package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class FishingHookRenderer extends EntityRenderer<FishingHook> {
   private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("textures/entity/fishing_hook.png");
   private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE_LOCATION);
   private static final double VIEW_BOBBING_SCALE = 960.0;

   public FishingHookRenderer(EntityRendererProvider.Context param0) {
      super(param0);
   }

   public void render(FishingHook param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      Player var7 = param0.getPlayerOwner();
      if (var7 != null) {
         param3.pushPose();
         param3.pushPose();
         param3.scale(0.5F, 0.5F, 0.5F);
         param3.mulPose(this.entityRenderDispatcher.cameraOrientation());
         param3.mulPose(Axis.YP.rotationDegrees(180.0F));
         PoseStack.Pose var8 = param3.last();
         Matrix4f var9 = var8.pose();
         Matrix3f var10 = var8.normal();
         VertexConsumer var11 = param4.getBuffer(RENDER_TYPE);
         vertex(var11, var9, var10, param5, 0.0F, 0, 0, 1);
         vertex(var11, var9, var10, param5, 1.0F, 0, 1, 1);
         vertex(var11, var9, var10, param5, 1.0F, 1, 1, 0);
         vertex(var11, var9, var10, param5, 0.0F, 1, 0, 0);
         param3.popPose();
         int var12 = var7.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
         ItemStack var13 = var7.getMainHandItem();
         if (!var13.is(Items.FISHING_ROD)) {
            var12 = -var12;
         }

         float var14 = var7.getAttackAnim(param2);
         float var15 = Mth.sin(Mth.sqrt(var14) * (float) Math.PI);
         float var16 = Mth.lerp(param2, var7.yBodyRotO, var7.yBodyRot) * (float) (Math.PI / 180.0);
         double var17 = Mth.sin(var16);
         double var19 = Mth.cos(var16);
         double var21 = var12 * 0.35;
         double var23 = 0.8;
         double var25;
         double var27;
         double var29;
         float var31;
         if ((this.entityRenderDispatcher.options == null || this.entityRenderDispatcher.options.getCameraType().isFirstPerson())
            && var7 == Minecraft.getInstance().player) {
            double var32 = 960.0 / this.entityRenderDispatcher.options.fov().get().intValue();
            Vec3 var34 = this.entityRenderDispatcher.camera.getNearPlane().getPointOnPlane(var12 * 0.525F, -0.1F);
            var34 = var34.scale(var32);
            var34 = var34.yRot(var15 * 0.5F);
            var34 = var34.xRot(-var15 * 0.7F);
            var25 = Mth.lerp(param2, var7.xo, var7.getX()) + var34.x;
            var27 = Mth.lerp(param2, var7.yo, var7.getY()) + var34.y;
            var29 = Mth.lerp(param2, var7.zo, var7.getZ()) + var34.z;
            var31 = var7.getEyeHeight();
         } else {
            var25 = Mth.lerp(param2, var7.xo, var7.getX()) - var19 * var21 - var17 * 0.8;
            var27 = var7.yo + var7.getEyeHeight() + (var7.getY() - var7.yo) * param2 - 0.45;
            var29 = Mth.lerp(param2, var7.zo, var7.getZ()) - var17 * var21 + var19 * 0.8;
            var31 = var7.isCrouching() ? -0.1875F : 0.0F;
         }

         double var32 = Mth.lerp(param2, param0.xo, param0.getX());
         double var34 = Mth.lerp(param2, param0.yo, param0.getY()) + 0.25;
         double var36 = Mth.lerp(param2, param0.zo, param0.getZ());
         float var38 = (float)(var25 - var32);
         float var39 = (float)(var27 - var34) + var31;
         float var40 = (float)(var29 - var36);
         VertexConsumer var41 = param4.getBuffer(RenderType.lineStrip());
         PoseStack.Pose var42 = param3.last();
         int var43 = 16;

         for (int var44 = 0; var44 <= 16; var44++) {
            stringVertex(var38, var39, var40, var41, var42, fraction(var44, 16), fraction(var44 + 1, 16));
         }

         param3.popPose();
         super.render(param0, param1, param2, param3, param4, param5);
      }
   }

   private static float fraction(int param0, int param1) {
      return (float)param0 / param1;
   }

   private static void vertex(VertexConsumer param0, Matrix4f param1, Matrix3f param2, int param3, float param4, int param5, int param6, int param7) {
      param0.vertex(param1, param4 - 0.5F, param5 - 0.5F, 0.0F)
         .color(255, 255, 255, 255)
         .uv(param6, param7)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(param3)
         .normal(param2, 0.0F, 1.0F, 0.0F)
         .endVertex();
   }

   private static void stringVertex(float param0, float param1, float param2, VertexConsumer param3, PoseStack.Pose param4, float param5, float param6) {
      float var7 = param0 * param5;
      float var8 = param1 * (param5 * param5 + param5) * 0.5F + 0.25F;
      float var9 = param2 * param5;
      float var10 = param0 * param6 - var7;
      float var11 = param1 * (param6 * param6 + param6) * 0.5F + 0.25F - var8;
      float var12 = param2 * param6 - var9;
      float var13 = Mth.sqrt(var10 * var10 + var11 * var11 + var12 * var12);
      var10 /= var13;
      var11 /= var13;
      var12 /= var13;
      param3.vertex(param4.pose(), var7, var8, var9).color(0, 0, 0, 255).normal(param4.normal(), var10, var11, var12).endVertex();
   }

   public ResourceLocation getTextureLocation(FishingHook param0) {
      return TEXTURE_LOCATION;
   }
}

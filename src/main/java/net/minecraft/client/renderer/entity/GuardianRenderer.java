package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class GuardianRenderer extends MobRenderer<Guardian, GuardianModel> {
   private static final ResourceLocation GUARDIAN_LOCATION = new ResourceLocation("textures/entity/guardian.png");
   private static final ResourceLocation GUARDIAN_BEAM_LOCATION = new ResourceLocation("textures/entity/guardian_beam.png");
   private static final RenderType BEAM_RENDER_TYPE = RenderType.entityCutoutNoCull(GUARDIAN_BEAM_LOCATION);

   public GuardianRenderer(EntityRendererProvider.Context param0) {
      this(param0, 0.5F, ModelLayers.GUARDIAN);
   }

   protected GuardianRenderer(EntityRendererProvider.Context param0, float param1, ModelLayerLocation param2) {
      super(param0, new GuardianModel(param0.bakeLayer(param2)), param1);
   }

   public boolean shouldRender(Guardian param0, Frustum param1, double param2, double param4, double param6) {
      if (super.shouldRender(param0, param1, param2, param4, param6)) {
         return true;
      }

      if (param0.hasActiveAttackTarget()) {
         LivingEntity var9 = param0.getActiveAttackTarget();
         if (var9 != null) {
            Vec3 var10 = this.getPosition(var9, var9.getBbHeight() * 0.5, 1.0F);
            Vec3 var11 = this.getPosition(param0, param0.getEyeHeight(), 1.0F);
            return param1.isVisible(new AABB(var11.x, var11.y, var11.z, var10.x, var10.y, var10.z));
         }
      }

      return false;
   }

   private Vec3 getPosition(LivingEntity param0, double param1, float param3) {
      double var5 = Mth.lerp(param3, param0.xOld, param0.getX());
      double var7 = Mth.lerp(param3, param0.yOld, param0.getY()) + param1;
      double var9 = Mth.lerp(param3, param0.zOld, param0.getZ());
      return new Vec3(var5, var7, var9);
   }

   public void render(Guardian param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      super.render(param0, param1, param2, param3, param4, param5);
      LivingEntity var7 = param0.getActiveAttackTarget();
      if (var7 != null) {
         float var8 = param0.getAttackAnimationScale(param2);
         float var9 = param0.getClientSideAttackTime() + param2;
         float var10 = var9 * 0.5F % 1.0F;
         float var11 = param0.getEyeHeight();
         param3.pushPose();
         param3.translate(0.0F, var11, 0.0F);
         Vec3 var12 = this.getPosition(var7, var7.getBbHeight() * 0.5, param2);
         Vec3 var13 = this.getPosition(param0, var11, param2);
         Vec3 var14 = var12.subtract(var13);
         float var15 = (float)(var14.length() + 1.0);
         var14 = var14.normalize();
         float var16 = (float)Math.acos(var14.y);
         float var17 = (float)Math.atan2(var14.z, var14.x);
         param3.mulPose(Axis.YP.rotationDegrees(((float) (Math.PI / 2) - var17) * (180.0F / (float)Math.PI)));
         param3.mulPose(Axis.XP.rotationDegrees(var16 * (180.0F / (float)Math.PI)));
         int var18 = 1;
         float var19 = var9 * 0.05F * -1.5F;
         float var20 = var8 * var8;
         int var21 = 64 + (int)(var20 * 191.0F);
         int var22 = 32 + (int)(var20 * 191.0F);
         int var23 = 128 - (int)(var20 * 64.0F);
         float var24 = 0.2F;
         float var25 = 0.282F;
         float var26 = Mth.cos(var19 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
         float var27 = Mth.sin(var19 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
         float var28 = Mth.cos(var19 + (float) (Math.PI / 4)) * 0.282F;
         float var29 = Mth.sin(var19 + (float) (Math.PI / 4)) * 0.282F;
         float var30 = Mth.cos(var19 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
         float var31 = Mth.sin(var19 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
         float var32 = Mth.cos(var19 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
         float var33 = Mth.sin(var19 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
         float var34 = Mth.cos(var19 + (float) Math.PI) * 0.2F;
         float var35 = Mth.sin(var19 + (float) Math.PI) * 0.2F;
         float var36 = Mth.cos(var19 + 0.0F) * 0.2F;
         float var37 = Mth.sin(var19 + 0.0F) * 0.2F;
         float var38 = Mth.cos(var19 + (float) (Math.PI / 2)) * 0.2F;
         float var39 = Mth.sin(var19 + (float) (Math.PI / 2)) * 0.2F;
         float var40 = Mth.cos(var19 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
         float var41 = Mth.sin(var19 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
         float var42 = var15;
         float var43 = 0.0F;
         float var44 = 0.4999F;
         float var45 = -1.0F + var10;
         float var46 = var15 * 2.5F + var45;
         VertexConsumer var47 = param4.getBuffer(BEAM_RENDER_TYPE);
         PoseStack.Pose var48 = param3.last();
         Matrix4f var49 = var48.pose();
         Matrix3f var50 = var48.normal();
         vertex(var47, var49, var50, var34, var42, var35, var21, var22, var23, 0.4999F, var46);
         vertex(var47, var49, var50, var34, 0.0F, var35, var21, var22, var23, 0.4999F, var45);
         vertex(var47, var49, var50, var36, 0.0F, var37, var21, var22, var23, 0.0F, var45);
         vertex(var47, var49, var50, var36, var42, var37, var21, var22, var23, 0.0F, var46);
         vertex(var47, var49, var50, var38, var42, var39, var21, var22, var23, 0.4999F, var46);
         vertex(var47, var49, var50, var38, 0.0F, var39, var21, var22, var23, 0.4999F, var45);
         vertex(var47, var49, var50, var40, 0.0F, var41, var21, var22, var23, 0.0F, var45);
         vertex(var47, var49, var50, var40, var42, var41, var21, var22, var23, 0.0F, var46);
         float var51 = 0.0F;
         if (param0.tickCount % 2 == 0) {
            var51 = 0.5F;
         }

         vertex(var47, var49, var50, var26, var42, var27, var21, var22, var23, 0.5F, var51 + 0.5F);
         vertex(var47, var49, var50, var28, var42, var29, var21, var22, var23, 1.0F, var51 + 0.5F);
         vertex(var47, var49, var50, var32, var42, var33, var21, var22, var23, 1.0F, var51);
         vertex(var47, var49, var50, var30, var42, var31, var21, var22, var23, 0.5F, var51);
         param3.popPose();
      }
   }

   private static void vertex(
      VertexConsumer param0,
      Matrix4f param1,
      Matrix3f param2,
      float param3,
      float param4,
      float param5,
      int param6,
      int param7,
      int param8,
      float param9,
      float param10
   ) {
      param0.vertex(param1, param3, param4, param5)
         .color(param6, param7, param8, 255)
         .uv(param9, param10)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(15728880)
         .normal(param2, 0.0F, 1.0F, 0.0F)
         .endVertex();
   }

   public ResourceLocation getTextureLocation(Guardian param0) {
      return GUARDIAN_LOCATION;
   }
}

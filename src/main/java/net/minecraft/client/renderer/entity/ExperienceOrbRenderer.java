package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class ExperienceOrbRenderer extends EntityRenderer<ExperienceOrb> {
   private static final ResourceLocation EXPERIENCE_ORB_LOCATION = new ResourceLocation("textures/entity/experience_orb.png");
   private static final RenderType RENDER_TYPE = RenderType.itemEntityTranslucentCull(EXPERIENCE_ORB_LOCATION);

   public ExperienceOrbRenderer(EntityRendererProvider.Context param0) {
      super(param0);
      this.shadowRadius = 0.15F;
      this.shadowStrength = 0.75F;
   }

   protected int getBlockLightLevel(ExperienceOrb param0, BlockPos param1) {
      return Mth.clamp(super.getBlockLightLevel(param0, param1) + 7, 0, 15);
   }

   public void render(ExperienceOrb param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      param3.pushPose();
      int var7 = param0.getIcon();
      float var8 = (var7 % 4 * 16 + 0) / 64.0F;
      float var9 = (var7 % 4 * 16 + 16) / 64.0F;
      float var10 = (var7 / 4 * 16 + 0) / 64.0F;
      float var11 = (var7 / 4 * 16 + 16) / 64.0F;
      float var12 = 1.0F;
      float var13 = 0.5F;
      float var14 = 0.25F;
      float var15 = 255.0F;
      float var16 = (param0.tickCount + param2) / 2.0F;
      int var17 = (int)((Mth.sin(var16 + 0.0F) + 1.0F) * 0.5F * 255.0F);
      int var18 = 255;
      int var19 = (int)((Mth.sin(var16 + (float) (Math.PI * 4.0 / 3.0)) + 1.0F) * 0.1F * 255.0F);
      param3.translate(0.0F, 0.1F, 0.0F);
      param3.mulPose(this.entityRenderDispatcher.cameraOrientation());
      param3.mulPose(Axis.YP.rotationDegrees(180.0F));
      float var20 = 0.3F;
      param3.scale(0.3F, 0.3F, 0.3F);
      VertexConsumer var21 = param4.getBuffer(RENDER_TYPE);
      PoseStack.Pose var22 = param3.last();
      Matrix4f var23 = var22.pose();
      Matrix3f var24 = var22.normal();
      vertex(var21, var23, var24, -0.5F, -0.25F, var17, 255, var19, var8, var11, param5);
      vertex(var21, var23, var24, 0.5F, -0.25F, var17, 255, var19, var9, var11, param5);
      vertex(var21, var23, var24, 0.5F, 0.75F, var17, 255, var19, var9, var10, param5);
      vertex(var21, var23, var24, -0.5F, 0.75F, var17, 255, var19, var8, var10, param5);
      param3.popPose();
      super.render(param0, param1, param2, param3, param4, param5);
   }

   private static void vertex(
      VertexConsumer param0,
      Matrix4f param1,
      Matrix3f param2,
      float param3,
      float param4,
      int param5,
      int param6,
      int param7,
      float param8,
      float param9,
      int param10
   ) {
      param0.vertex(param1, param3, param4, 0.0F)
         .color(param5, param6, param7, 128)
         .uv(param8, param9)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(param10)
         .normal(param2, 0.0F, 1.0F, 0.0F)
         .endVertex();
   }

   public ResourceLocation getTextureLocation(ExperienceOrb param0) {
      return EXPERIENCE_ORB_LOCATION;
   }
}

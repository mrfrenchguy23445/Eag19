package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public abstract class EntityRenderer<T extends Entity> {
   protected static final float NAMETAG_SCALE = 0.025F;
   protected final EntityRenderDispatcher entityRenderDispatcher;
   private final Font font;
   protected float shadowRadius;
   protected float shadowStrength = 1.0F;

   protected EntityRenderer(EntityRendererProvider.Context param0) {
      this.entityRenderDispatcher = param0.getEntityRenderDispatcher();
      this.font = param0.getFont();
   }

   public final int getPackedLightCoords(T param0, float param1) {
      BlockPos var3 = BlockPos.containing(param0.getLightProbePosition(param1));
      return LightTexture.pack(this.getBlockLightLevel(param0, var3), this.getSkyLightLevel(param0, var3));
   }

   protected int getSkyLightLevel(T param0, BlockPos param1) {
      return param0.level.getBrightness(LightLayer.SKY, param1);
   }

   protected int getBlockLightLevel(T param0, BlockPos param1) {
      return param0.isOnFire() ? 15 : param0.level.getBrightness(LightLayer.BLOCK, param1);
   }

   public boolean shouldRender(T param0, Frustum param1, double param2, double param4, double param6) {
      if (!param0.shouldRender(param2, param4, param6)) {
         return false;
      }

      if (param0.noCulling) {
         return true;
      }

      AABB var9 = param0.getBoundingBoxForCulling().inflate(0.5);
      if (var9.hasNaN() || var9.getSize() == 0.0) {
         var9 = new AABB(param0.getX() - 2.0, param0.getY() - 2.0, param0.getZ() - 2.0, param0.getX() + 2.0, param0.getY() + 2.0, param0.getZ() + 2.0);
      }

      return param1.isVisible(var9);
   }

   public Vec3 getRenderOffset(T param0, float param1) {
      return Vec3.ZERO;
   }

   public void render(T param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      if (this.shouldShowName(param0)) {
         this.renderNameTag(param0, param0.getDisplayName(), param3, param4, param5);
      }
   }

   protected boolean shouldShowName(T param0) {
      return param0.shouldShowName() && param0.hasCustomName();
   }

   public abstract ResourceLocation getTextureLocation(T var1);

   public Font getFont() {
      return this.font;
   }

   protected void renderNameTag(T param0, Component param1, PoseStack param2, MultiBufferSource param3, int param4) {
      double var6 = this.entityRenderDispatcher.distanceToSqr(param0);
      if (!(var6 > 4096.0)) {
         boolean var8 = !param0.isDiscrete();
         float var9 = param0.getBbHeight() + 0.5F;
         int var10 = "deadmau5".equals(param1.getString()) ? -10 : 0;
         param2.pushPose();
         param2.translate(0.0F, var9, 0.0F);
         param2.mulPose(this.entityRenderDispatcher.cameraOrientation());
         param2.scale(-0.025F, -0.025F, 0.025F);
         Matrix4f var11 = param2.last().pose();
         float var12 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
         int var13 = (int)(var12 * 255.0F) << 24;
         Font var14 = this.getFont();
         float var15 = -var14.width(param1) / 2;
         var14.drawInBatch(param1, var15, var10, 553648127, false, var11, param3, var8 ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, var13, param4);
         if (var8) {
            var14.drawInBatch(param1, var15, var10, -1, false, var11, param3, Font.DisplayMode.NORMAL, 0, param4);
         }

         param2.popPose();
      }
   }
}

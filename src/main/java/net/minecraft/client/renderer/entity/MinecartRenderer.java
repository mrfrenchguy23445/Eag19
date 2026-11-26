package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.MinecartModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MinecartRenderer<T extends AbstractMinecart> extends EntityRenderer<T> {
   private static final ResourceLocation MINECART_LOCATION = new ResourceLocation("textures/entity/minecart.png");
   protected final EntityModel<T> model;
   private final BlockRenderDispatcher blockRenderer;

   public MinecartRenderer(EntityRendererProvider.Context param0, ModelLayerLocation param1) {
      super(param0);
      this.shadowRadius = 0.7F;
      this.model = new MinecartModel<>(param0.bakeLayer(param1));
      this.blockRenderer = param0.getBlockRenderDispatcher();
   }

   public void render(T param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      super.render(param0, param1, param2, param3, param4, param5);
      param3.pushPose();
      long var7 = param0.getId() * 493286711L;
      var7 = var7 * var7 * 4392167121L + var7 * 98761L;
      float var9 = (((float)(var7 >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      float var10 = (((float)(var7 >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      float var11 = (((float)(var7 >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      param3.translate(var9, var10, var11);
      double var12 = Mth.lerp(param2, param0.xOld, param0.getX());
      double var14 = Mth.lerp(param2, param0.yOld, param0.getY());
      double var16 = Mth.lerp(param2, param0.zOld, param0.getZ());
      double var18 = 0.3F;
      Vec3 var20 = param0.getPos(var12, var14, var16);
      float var21 = Mth.lerp(param2, param0.xRotO, param0.getXRot());
      if (var20 != null) {
         Vec3 var22 = param0.getPosOffs(var12, var14, var16, 0.3F);
         Vec3 var23 = param0.getPosOffs(var12, var14, var16, -0.3F);
         if (var22 == null) {
            var22 = var20;
         }

         if (var23 == null) {
            var23 = var20;
         }

         param3.translate(var20.x - var12, (var22.y + var23.y) / 2.0 - var14, var20.z - var16);
         Vec3 var24 = var23.add(-var22.x, -var22.y, -var22.z);
         if (var24.length() != 0.0) {
            var24 = var24.normalize();
            param1 = (float)(Math.atan2(var24.z, var24.x) * 180.0 / Math.PI);
            var21 = (float)(Math.atan(var24.y) * 73.0);
         }
      }

      param3.translate(0.0F, 0.375F, 0.0F);
      param3.mulPose(Axis.YP.rotationDegrees(180.0F - param1));
      param3.mulPose(Axis.ZP.rotationDegrees(-var21));
      float var22 = param0.getHurtTime() - param2;
      float var23 = param0.getDamage() - param2;
      if (var23 < 0.0F) {
         var23 = 0.0F;
      }

      if (var22 > 0.0F) {
         param3.mulPose(Axis.XP.rotationDegrees(Mth.sin(var22) * var22 * var23 / 10.0F * param0.getHurtDir()));
      }

      int var24 = param0.getDisplayOffset();
      BlockState var25 = param0.getDisplayBlockState();
      if (var25.getRenderShape() != RenderShape.INVISIBLE) {
         param3.pushPose();
         float var26 = 0.75F;
         param3.scale(0.75F, 0.75F, 0.75F);
         param3.translate(-0.5F, (var24 - 8) / 16.0F, 0.5F);
         param3.mulPose(Axis.YP.rotationDegrees(90.0F));
         this.renderMinecartContents(param0, param2, var25, param3, param4, param5);
         param3.popPose();
      }

      param3.scale(-1.0F, -1.0F, 1.0F);
      this.model.setupAnim(param0, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      VertexConsumer var26 = param4.getBuffer(this.model.renderType(this.getTextureLocation(param0)));
      this.model.renderToBuffer(param3, var26, param5, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
      param3.popPose();
   }

   public ResourceLocation getTextureLocation(T param0) {
      return MINECART_LOCATION;
   }

   protected void renderMinecartContents(T param0, float param1, BlockState param2, PoseStack param3, MultiBufferSource param4, int param5) {
      this.blockRenderer.renderSingleBlock(param2, param3, param4, param5, OverlayTexture.NO_OVERLAY);
   }
}

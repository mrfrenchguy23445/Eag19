package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public abstract class MobRenderer<T extends Mob, M extends EntityModel<T>> extends LivingEntityRenderer<T, M> {
   public static final int LEASH_RENDER_STEPS = 24;

   public MobRenderer(EntityRendererProvider.Context param0, M param1, float param2) {
      super(param0, param1, param2);
   }

   protected boolean shouldShowName(T param0) {
      return super.shouldShowName(param0) && (param0.shouldShowName() || param0.hasCustomName() && param0 == this.entityRenderDispatcher.crosshairPickEntity);
   }

   public boolean shouldRender(T param0, Frustum param1, double param2, double param4, double param6) {
      if (super.shouldRender(param0, param1, param2, param4, param6)) {
         return true;
      }

      Entity var9 = param0.getLeashHolder();
      return var9 != null ? param1.isVisible(var9.getBoundingBoxForCulling()) : false;
   }

   public void render(T param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      super.render(param0, param1, param2, param3, param4, param5);
      Entity var7 = param0.getLeashHolder();
      if (var7 != null) {
         this.renderLeash(param0, param2, param3, param4, var7);
      }
   }

   private <E extends Entity> void renderLeash(T param0, float param1, PoseStack param2, MultiBufferSource param3, E param4) {
      param2.pushPose();
      Vec3 var6 = param4.getRopeHoldPosition(param1);
      double var7 = Mth.lerp(param1, param0.yBodyRotO, param0.yBodyRot) * (float) (Math.PI / 180.0) + (Math.PI / 2);
      Vec3 var9 = param0.getLeashOffset(param1);
      double var10 = Math.cos(var7) * var9.z + Math.sin(var7) * var9.x;
      double var12 = Math.sin(var7) * var9.z - Math.cos(var7) * var9.x;
      double var14 = Mth.lerp(param1, param0.xo, param0.getX()) + var10;
      double var16 = Mth.lerp(param1, param0.yo, param0.getY()) + var9.y;
      double var18 = Mth.lerp(param1, param0.zo, param0.getZ()) + var12;
      param2.translate(var10, var9.y, var12);
      float var20 = (float)(var6.x - var14);
      float var21 = (float)(var6.y - var16);
      float var22 = (float)(var6.z - var18);
      float var23 = 0.025F;
      VertexConsumer var24 = param3.getBuffer(RenderType.leash());
      Matrix4f var25 = param2.last().pose();
      float var26 = Mth.invSqrt(var20 * var20 + var22 * var22) * 0.025F / 2.0F;
      float var27 = var22 * var26;
      float var28 = var20 * var26;
      BlockPos var29 = BlockPos.containing(param0.getEyePosition(param1));
      BlockPos var30 = BlockPos.containing(param4.getEyePosition(param1));
      int var31 = this.getBlockLightLevel(param0, var29);
      int var32 = this.entityRenderDispatcher.getRenderer(param4).getBlockLightLevel(param4, var30);
      int var33 = param0.level.getBrightness(LightLayer.SKY, var29);
      int var34 = param0.level.getBrightness(LightLayer.SKY, var30);

      for (int var35 = 0; var35 <= 24; var35++) {
         addVertexPair(var24, var25, var20, var21, var22, var31, var32, var33, var34, 0.025F, 0.025F, var27, var28, var35, false);
      }

      for (int var35 = 24; var35 >= 0; var35--) {
         addVertexPair(var24, var25, var20, var21, var22, var31, var32, var33, var34, 0.025F, 0.0F, var27, var28, var35, true);
      }

      param2.popPose();
   }

   private static void addVertexPair(
      VertexConsumer param0,
      Matrix4f param1,
      float param2,
      float param3,
      float param4,
      int param5,
      int param6,
      int param7,
      int param8,
      float param9,
      float param10,
      float param11,
      float param12,
      int param13,
      boolean param14
   ) {
      float var15 = param13 / 24.0F;
      int var16 = (int)Mth.lerp(var15, param5, param6);
      int var17 = (int)Mth.lerp(var15, param7, param8);
      int var18 = LightTexture.pack(var16, var17);
      float var19 = param13 % 2 == (param14 ? 1 : 0) ? 0.7F : 1.0F;
      float var20 = 0.5F * var19;
      float var21 = 0.4F * var19;
      float var22 = 0.3F * var19;
      float var23 = param2 * var15;
      float var24 = param3 > 0.0F ? param3 * var15 * var15 : param3 - param3 * (1.0F - var15) * (1.0F - var15);
      float var25 = param4 * var15;
      param0.vertex(param1, var23 - param11, var24 + param10, var25 + param12).color(var20, var21, var22, 1.0F).uv2(var18).endVertex();
      param0.vertex(param1, var23 + param11, var24 + param9 - param10, var25 - param12).color(var20, var21, var22, 1.0F).uv2(var18).endVertex();
   }
}

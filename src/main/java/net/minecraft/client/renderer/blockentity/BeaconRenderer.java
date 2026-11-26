package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class BeaconRenderer implements BlockEntityRenderer<BeaconBlockEntity> {
   public static final ResourceLocation BEAM_LOCATION = new ResourceLocation("textures/entity/beacon_beam.png");
   public static final int MAX_RENDER_Y = 1024;

   public BeaconRenderer(BlockEntityRendererProvider.Context param0) {
   }

   public void render(BeaconBlockEntity param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      long var7 = param0.getLevel().getGameTime();
      List<BeaconBlockEntity.BeaconBeamSection> var9 = param0.getBeamSections();
      int var10 = 0;

      for (int var11 = 0; var11 < var9.size(); var11++) {
         BeaconBlockEntity.BeaconBeamSection var12 = var9.get(var11);
         renderBeaconBeam(param2, param3, param1, var7, var10, var11 == var9.size() - 1 ? 1024 : var12.getHeight(), var12.getColor());
         var10 += var12.getHeight();
      }
   }

   private static void renderBeaconBeam(PoseStack param0, MultiBufferSource param1, float param2, long param3, int param5, int param6, float[] param7) {
      renderBeaconBeam(param0, param1, BEAM_LOCATION, param2, 1.0F, param3, param5, param6, param7, 0.2F, 0.25F);
   }

   public static void renderBeaconBeam(
      PoseStack param0,
      MultiBufferSource param1,
      ResourceLocation param2,
      float param3,
      float param4,
      long param5,
      int param7,
      int param8,
      float[] param9,
      float param10,
      float param11
   ) {
      int var12 = param7 + param8;
      param0.pushPose();
      param0.translate(0.5, 0.0, 0.5);
      float var13 = Math.floorMod(param5, 40) + param3;
      float var14 = param8 < 0 ? var13 : -var13;
      float var15 = Mth.frac(var14 * 0.2F - Mth.floor(var14 * 0.1F));
      float var16 = param9[0];
      float var17 = param9[1];
      float var18 = param9[2];
      param0.pushPose();
      param0.mulPose(Axis.YP.rotationDegrees(var13 * 2.25F - 45.0F));
      float var19 = 0.0F;
      float var20 = param10;
      float var21 = param10;
      float var22 = 0.0F;
      float var23 = -param10;
      float var24 = 0.0F;
      float var25 = 0.0F;
      float var26 = -param10;
      float var27 = 0.0F;
      float var28 = 1.0F;
      float var29 = -1.0F + var15;
      float var30 = param8 * param4 * (0.5F / param10) + var29;
      renderPart(
         param0,
         param1.getBuffer(RenderType.beaconBeam(param2, false)),
         var16,
         var17,
         var18,
         1.0F,
         param7,
         var12,
         0.0F,
         var20,
         var21,
         0.0F,
         var23,
         0.0F,
         0.0F,
         var26,
         0.0F,
         1.0F,
         var30,
         var29
      );
      param0.popPose();
      var19 = -param11;
      var20 = -param11;
      var21 = param11;
      var22 = -param11;
      var23 = -param11;
      var24 = param11;
      var25 = param11;
      var26 = param11;
      var27 = 0.0F;
      var28 = 1.0F;
      var29 = -1.0F + var15;
      var30 = param8 * param4 + var29;
      renderPart(
         param0,
         param1.getBuffer(RenderType.beaconBeam(param2, true)),
         var16,
         var17,
         var18,
         0.125F,
         param7,
         var12,
         var19,
         var20,
         var21,
         var22,
         var23,
         var24,
         var25,
         var26,
         0.0F,
         1.0F,
         var30,
         var29
      );
      param0.popPose();
   }

   private static void renderPart(
      PoseStack param0,
      VertexConsumer param1,
      float param2,
      float param3,
      float param4,
      float param5,
      int param6,
      int param7,
      float param8,
      float param9,
      float param10,
      float param11,
      float param12,
      float param13,
      float param14,
      float param15,
      float param16,
      float param17,
      float param18,
      float param19
   ) {
      PoseStack.Pose var20 = param0.last();
      Matrix4f var21 = var20.pose();
      Matrix3f var22 = var20.normal();
      renderQuad(var21, var22, param1, param2, param3, param4, param5, param6, param7, param8, param9, param10, param11, param16, param17, param18, param19);
      renderQuad(var21, var22, param1, param2, param3, param4, param5, param6, param7, param14, param15, param12, param13, param16, param17, param18, param19);
      renderQuad(var21, var22, param1, param2, param3, param4, param5, param6, param7, param10, param11, param14, param15, param16, param17, param18, param19);
      renderQuad(var21, var22, param1, param2, param3, param4, param5, param6, param7, param12, param13, param8, param9, param16, param17, param18, param19);
   }

   private static void renderQuad(
      Matrix4f param0,
      Matrix3f param1,
      VertexConsumer param2,
      float param3,
      float param4,
      float param5,
      float param6,
      int param7,
      int param8,
      float param9,
      float param10,
      float param11,
      float param12,
      float param13,
      float param14,
      float param15,
      float param16
   ) {
      addVertex(param0, param1, param2, param3, param4, param5, param6, param8, param9, param10, param14, param15);
      addVertex(param0, param1, param2, param3, param4, param5, param6, param7, param9, param10, param14, param16);
      addVertex(param0, param1, param2, param3, param4, param5, param6, param7, param11, param12, param13, param16);
      addVertex(param0, param1, param2, param3, param4, param5, param6, param8, param11, param12, param13, param15);
   }

   private static void addVertex(
      Matrix4f param0,
      Matrix3f param1,
      VertexConsumer param2,
      float param3,
      float param4,
      float param5,
      float param6,
      int param7,
      float param8,
      float param9,
      float param10,
      float param11
   ) {
      param2.vertex(param0, param8, param7, param9)
         .color(param3, param4, param5, param6)
         .uv(param10, param11)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(15728880)
         .normal(param1, 0.0F, 1.0F, 0.0F)
         .endVertex();
   }

   public boolean shouldRenderOffScreen(BeaconBlockEntity param0) {
      return true;
   }

   @Override
   public int getViewDistance() {
      return 256;
   }

   public boolean shouldRender(BeaconBlockEntity param0, Vec3 param1) {
      return Vec3.atCenterOf(param0.getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(param1.multiply(1.0, 0.0, 1.0), this.getViewDistance());
   }
}

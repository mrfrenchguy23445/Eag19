package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.PaintingTextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class PaintingRenderer extends EntityRenderer<Painting> {
   public PaintingRenderer(EntityRendererProvider.Context param0) {
      super(param0);
   }

   public void render(Painting param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      param3.pushPose();
      param3.mulPose(Axis.YP.rotationDegrees(180.0F - param1));
      PaintingVariant var7 = param0.getVariant().value();
      float var8 = 0.0625F;
      param3.scale(0.0625F, 0.0625F, 0.0625F);
      VertexConsumer var9 = param4.getBuffer(RenderType.entitySolid(this.getTextureLocation(param0)));
      PaintingTextureManager var10 = Minecraft.getInstance().getPaintingTextures();
      this.renderPainting(param3, var9, param0, var7.getWidth(), var7.getHeight(), var10.get(var7), var10.getBackSprite());
      param3.popPose();
      super.render(param0, param1, param2, param3, param4, param5);
   }

   public ResourceLocation getTextureLocation(Painting param0) {
      return Minecraft.getInstance().getPaintingTextures().getBackSprite().atlasLocation();
   }

   private void renderPainting(
      PoseStack param0, VertexConsumer param1, Painting param2, int param3, int param4, TextureAtlasSprite param5, TextureAtlasSprite param6
   ) {
      PoseStack.Pose var8 = param0.last();
      Matrix4f var9 = var8.pose();
      Matrix3f var10 = var8.normal();
      float var11 = -param3 / 2.0F;
      float var12 = -param4 / 2.0F;
      float var13 = 0.5F;
      float var14 = param6.getU0();
      float var15 = param6.getU1();
      float var16 = param6.getV0();
      float var17 = param6.getV1();
      float var18 = param6.getU0();
      float var19 = param6.getU1();
      float var20 = param6.getV0();
      float var21 = param6.getV(1.0);
      float var22 = param6.getU0();
      float var23 = param6.getU(1.0);
      float var24 = param6.getV0();
      float var25 = param6.getV1();
      int var26 = param3 / 16;
      int var27 = param4 / 16;
      double var28 = 16.0 / var26;
      double var30 = 16.0 / var27;

      for (int var32 = 0; var32 < var26; var32++) {
         for (int var33 = 0; var33 < var27; var33++) {
            float var34 = var11 + (var32 + 1) * 16;
            float var35 = var11 + var32 * 16;
            float var36 = var12 + (var33 + 1) * 16;
            float var37 = var12 + var33 * 16;
            int var38 = param2.getBlockX();
            int var39 = Mth.floor(param2.getY() + (var36 + var37) / 2.0F / 16.0F);
            int var40 = param2.getBlockZ();
            Direction var41 = param2.getDirection();
            if (var41 == Direction.NORTH) {
               var38 = Mth.floor(param2.getX() + (var34 + var35) / 2.0F / 16.0F);
            }

            if (var41 == Direction.WEST) {
               var40 = Mth.floor(param2.getZ() - (var34 + var35) / 2.0F / 16.0F);
            }

            if (var41 == Direction.SOUTH) {
               var38 = Mth.floor(param2.getX() - (var34 + var35) / 2.0F / 16.0F);
            }

            if (var41 == Direction.EAST) {
               var40 = Mth.floor(param2.getZ() + (var34 + var35) / 2.0F / 16.0F);
            }

            int var42 = LevelRenderer.getLightColor(param2.level, new BlockPos(var38, var39, var40));
            float var43 = param5.getU(var28 * (var26 - var32));
            float var44 = param5.getU(var28 * (var26 - (var32 + 1)));
            float var45 = param5.getV(var30 * (var27 - var33));
            float var46 = param5.getV(var30 * (var27 - (var33 + 1)));
            this.vertex(var9, var10, param1, var34, var37, var44, var45, -0.5F, 0, 0, -1, var42);
            this.vertex(var9, var10, param1, var35, var37, var43, var45, -0.5F, 0, 0, -1, var42);
            this.vertex(var9, var10, param1, var35, var36, var43, var46, -0.5F, 0, 0, -1, var42);
            this.vertex(var9, var10, param1, var34, var36, var44, var46, -0.5F, 0, 0, -1, var42);
            this.vertex(var9, var10, param1, var34, var36, var15, var16, 0.5F, 0, 0, 1, var42);
            this.vertex(var9, var10, param1, var35, var36, var14, var16, 0.5F, 0, 0, 1, var42);
            this.vertex(var9, var10, param1, var35, var37, var14, var17, 0.5F, 0, 0, 1, var42);
            this.vertex(var9, var10, param1, var34, var37, var15, var17, 0.5F, 0, 0, 1, var42);
            this.vertex(var9, var10, param1, var34, var36, var18, var20, -0.5F, 0, 1, 0, var42);
            this.vertex(var9, var10, param1, var35, var36, var19, var20, -0.5F, 0, 1, 0, var42);
            this.vertex(var9, var10, param1, var35, var36, var19, var21, 0.5F, 0, 1, 0, var42);
            this.vertex(var9, var10, param1, var34, var36, var18, var21, 0.5F, 0, 1, 0, var42);
            this.vertex(var9, var10, param1, var34, var37, var18, var20, 0.5F, 0, -1, 0, var42);
            this.vertex(var9, var10, param1, var35, var37, var19, var20, 0.5F, 0, -1, 0, var42);
            this.vertex(var9, var10, param1, var35, var37, var19, var21, -0.5F, 0, -1, 0, var42);
            this.vertex(var9, var10, param1, var34, var37, var18, var21, -0.5F, 0, -1, 0, var42);
            this.vertex(var9, var10, param1, var34, var36, var23, var24, 0.5F, -1, 0, 0, var42);
            this.vertex(var9, var10, param1, var34, var37, var23, var25, 0.5F, -1, 0, 0, var42);
            this.vertex(var9, var10, param1, var34, var37, var22, var25, -0.5F, -1, 0, 0, var42);
            this.vertex(var9, var10, param1, var34, var36, var22, var24, -0.5F, -1, 0, 0, var42);
            this.vertex(var9, var10, param1, var35, var36, var23, var24, -0.5F, 1, 0, 0, var42);
            this.vertex(var9, var10, param1, var35, var37, var23, var25, -0.5F, 1, 0, 0, var42);
            this.vertex(var9, var10, param1, var35, var37, var22, var25, 0.5F, 1, 0, 0, var42);
            this.vertex(var9, var10, param1, var35, var36, var22, var24, 0.5F, 1, 0, 0, var42);
         }
      }
   }

   private void vertex(
      Matrix4f param0,
      Matrix3f param1,
      VertexConsumer param2,
      float param3,
      float param4,
      float param5,
      float param6,
      float param7,
      int param8,
      int param9,
      int param10,
      int param11
   ) {
      param2.vertex(param0, param3, param4, param7)
         .color(255, 255, 255, 255)
         .uv(param5, param6)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(param11)
         .normal(param1, param8, param9, param10)
         .endVertex();
   }
}

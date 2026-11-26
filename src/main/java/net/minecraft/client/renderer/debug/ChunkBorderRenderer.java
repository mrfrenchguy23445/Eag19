package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.joml.Matrix4f;

public class ChunkBorderRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int CELL_BORDER = FastColor.ARGB32.color(255, 0, 155, 155);
   private static final int YELLOW = FastColor.ARGB32.color(255, 255, 255, 0);

   public ChunkBorderRenderer(Minecraft param0) {
      this.minecraft = param0;
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      Entity var9 = this.minecraft.gameRenderer.getMainCamera().getEntity();
      float var10 = (float)(this.minecraft.level.getMinBuildHeight() - param4);
      float var11 = (float)(this.minecraft.level.getMaxBuildHeight() - param4);
      ChunkPos var12 = var9.chunkPosition();
      float var13 = (float)(var12.getMinBlockX() - param2);
      float var14 = (float)(var12.getMinBlockZ() - param6);
      VertexConsumer var15 = param1.getBuffer(RenderType.debugLineStrip(1.0));
      Matrix4f var16 = param0.last().pose();

      for (int var17 = -16; var17 <= 32; var17 += 16) {
         for (int var18 = -16; var18 <= 32; var18 += 16) {
            var15.vertex(var16, var13 + var17, var10, var14 + var18).color(1.0F, 0.0F, 0.0F, 0.0F).endVertex();
            var15.vertex(var16, var13 + var17, var10, var14 + var18).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
            var15.vertex(var16, var13 + var17, var11, var14 + var18).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
            var15.vertex(var16, var13 + var17, var11, var14 + var18).color(1.0F, 0.0F, 0.0F, 0.0F).endVertex();
         }
      }

      for (int var17 = 2; var17 < 16; var17 += 2) {
         int var18 = var17 % 4 == 0 ? CELL_BORDER : YELLOW;
         var15.vertex(var16, var13 + var17, var10, var14).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
         var15.vertex(var16, var13 + var17, var10, var14).color(var18).endVertex();
         var15.vertex(var16, var13 + var17, var11, var14).color(var18).endVertex();
         var15.vertex(var16, var13 + var17, var11, var14).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
         var15.vertex(var16, var13 + var17, var10, var14 + 16.0F).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
         var15.vertex(var16, var13 + var17, var10, var14 + 16.0F).color(var18).endVertex();
         var15.vertex(var16, var13 + var17, var11, var14 + 16.0F).color(var18).endVertex();
         var15.vertex(var16, var13 + var17, var11, var14 + 16.0F).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
      }

      for (int var17 = 2; var17 < 16; var17 += 2) {
         int var18 = var17 % 4 == 0 ? CELL_BORDER : YELLOW;
         var15.vertex(var16, var13, var10, var14 + var17).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
         var15.vertex(var16, var13, var10, var14 + var17).color(var18).endVertex();
         var15.vertex(var16, var13, var11, var14 + var17).color(var18).endVertex();
         var15.vertex(var16, var13, var11, var14 + var17).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
         var15.vertex(var16, var13 + 16.0F, var10, var14 + var17).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
         var15.vertex(var16, var13 + 16.0F, var10, var14 + var17).color(var18).endVertex();
         var15.vertex(var16, var13 + 16.0F, var11, var14 + var17).color(var18).endVertex();
         var15.vertex(var16, var13 + 16.0F, var11, var14 + var17).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
      }

      for (int var17 = this.minecraft.level.getMinBuildHeight(); var17 <= this.minecraft.level.getMaxBuildHeight(); var17 += 2) {
         float var18 = (float)(var17 - param4);
         int var19 = var17 % 8 == 0 ? CELL_BORDER : YELLOW;
         var15.vertex(var16, var13, var18, var14).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
         var15.vertex(var16, var13, var18, var14).color(var19).endVertex();
         var15.vertex(var16, var13, var18, var14 + 16.0F).color(var19).endVertex();
         var15.vertex(var16, var13 + 16.0F, var18, var14 + 16.0F).color(var19).endVertex();
         var15.vertex(var16, var13 + 16.0F, var18, var14).color(var19).endVertex();
         var15.vertex(var16, var13, var18, var14).color(var19).endVertex();
         var15.vertex(var16, var13, var18, var14).color(1.0F, 1.0F, 0.0F, 0.0F).endVertex();
      }

      var15 = param1.getBuffer(RenderType.debugLineStrip(2.0));

      for (int var17 = 0; var17 <= 16; var17 += 16) {
         for (int var18 = 0; var18 <= 16; var18 += 16) {
            var15.vertex(var16, var13 + var17, var10, var14 + var18).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
            var15.vertex(var16, var13 + var17, var10, var14 + var18).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            var15.vertex(var16, var13 + var17, var11, var14 + var18).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
            var15.vertex(var16, var13 + var17, var11, var14 + var18).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
         }
      }

      for (int var17 = this.minecraft.level.getMinBuildHeight(); var17 <= this.minecraft.level.getMaxBuildHeight(); var17 += 16) {
         float var18 = (float)(var17 - param4);
         var15.vertex(var16, var13, var18, var14).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
         var15.vertex(var16, var13, var18, var14).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
         var15.vertex(var16, var13, var18, var14 + 16.0F).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
         var15.vertex(var16, var13 + 16.0F, var18, var14 + 16.0F).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
         var15.vertex(var16, var13 + 16.0F, var18, var14).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
         var15.vertex(var16, var13, var18, var14).color(0.25F, 0.25F, 1.0F, 1.0F).endVertex();
         var15.vertex(var16, var13, var18, var14).color(0.25F, 0.25F, 1.0F, 0.0F).endVertex();
      }
   }
}

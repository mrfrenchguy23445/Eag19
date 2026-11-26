package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

public class SolidFaceRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;

   public SolidFaceRenderer(Minecraft param0) {
      this.minecraft = param0;
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      Matrix4f var9 = param0.last().pose();
      BlockGetter var10 = this.minecraft.player.level;
      BlockPos var11 = BlockPos.containing(param2, param4, param6);

      for (BlockPos var13 : BlockPos.betweenClosed(var11.offset(-6, -6, -6), var11.offset(6, 6, 6))) {
         BlockState var14 = var10.getBlockState(var13);
         if (!var14.is(Blocks.AIR)) {
            VoxelShape var15 = var14.getShape(var10, var13);

            for (AABB var17 : var15.toAabbs()) {
               AABB var18 = var17.move(var13).inflate(0.002);
               float var19 = (float)(var18.minX - param2);
               float var20 = (float)(var18.minY - param4);
               float var21 = (float)(var18.minZ - param6);
               float var22 = (float)(var18.maxX - param2);
               float var23 = (float)(var18.maxY - param4);
               float var24 = (float)(var18.maxZ - param6);
               float var25 = 1.0F;
               float var26 = 0.0F;
               float var27 = 0.0F;
               float var28 = 0.5F;
               if (var14.isFaceSturdy(var10, var13, Direction.WEST)) {
                  VertexConsumer var29 = param1.getBuffer(RenderType.debugFilledBox());
                  var29.vertex(var9, var19, var20, var21).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var19, var20, var24).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var19, var23, var21).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var19, var23, var24).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
               }

               if (var14.isFaceSturdy(var10, var13, Direction.SOUTH)) {
                  VertexConsumer var29 = param1.getBuffer(RenderType.debugFilledBox());
                  var29.vertex(var9, var19, var23, var24).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var19, var20, var24).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var22, var23, var24).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var22, var20, var24).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
               }

               if (var14.isFaceSturdy(var10, var13, Direction.EAST)) {
                  VertexConsumer var29 = param1.getBuffer(RenderType.debugFilledBox());
                  var29.vertex(var9, var22, var20, var24).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var22, var20, var21).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var22, var23, var24).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var22, var23, var21).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
               }

               if (var14.isFaceSturdy(var10, var13, Direction.NORTH)) {
                  VertexConsumer var29 = param1.getBuffer(RenderType.debugFilledBox());
                  var29.vertex(var9, var22, var23, var21).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var22, var20, var21).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var19, var23, var21).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var19, var20, var21).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
               }

               if (var14.isFaceSturdy(var10, var13, Direction.DOWN)) {
                  VertexConsumer var29 = param1.getBuffer(RenderType.debugFilledBox());
                  var29.vertex(var9, var19, var20, var21).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var22, var20, var21).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var19, var20, var24).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var22, var20, var24).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
               }

               if (var14.isFaceSturdy(var10, var13, Direction.UP)) {
                  VertexConsumer var29 = param1.getBuffer(RenderType.debugFilledBox());
                  var29.vertex(var9, var19, var23, var21).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var19, var23, var24).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var22, var23, var21).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
                  var29.vertex(var9, var22, var23, var24).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
               }
            }
         }
      }
   }
}

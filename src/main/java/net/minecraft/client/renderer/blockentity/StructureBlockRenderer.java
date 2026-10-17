package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;

public class StructureBlockRenderer implements BlockEntityRenderer<StructureBlockEntity> {
   public StructureBlockRenderer(BlockEntityRendererProvider.Context param0) {
   }

   public void render(StructureBlockEntity param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      if (Minecraft.getInstance().player.canUseGameMasterBlocks() || Minecraft.getInstance().player.isSpectator()) {
         BlockPos var7 = param0.getStructurePos();
         Vec3i var8 = param0.getStructureSize();
         if (var8.getX() >= 1 && var8.getY() >= 1 && var8.getZ() >= 1) {
            if (param0.getMode() == StructureMode.SAVE || param0.getMode() == StructureMode.LOAD) {
               double var9 = var7.getX();
               double var11 = var7.getZ();
               double var19 = var7.getY();
               double var25 = var19 + var8.getY();
               double var13;
               double var15;
               switch (param0.getMirror()) {
                  case LEFT_RIGHT:
                     var13 = var8.getX();
                     var15 = -var8.getZ();
                     break;
                  case FRONT_BACK:
                     var13 = -var8.getX();
                     var15 = var8.getZ();
                     break;
                  default:
                     var13 = var8.getX();
                     var15 = var8.getZ();
               }

               double var17;
               double var21;
               double var23;
               double var27;
               switch (param0.getRotation()) {
                  case CLOCKWISE_90:
                     var17 = var15 < 0.0 ? var9 : var9 + 1.0;
                     var21 = var13 < 0.0 ? var11 + 1.0 : var11;
                     var23 = var17 - var15;
                     var27 = var21 + var13;
                     break;
                  case CLOCKWISE_180:
                     var17 = var13 < 0.0 ? var9 : var9 + 1.0;
                     var21 = var15 < 0.0 ? var11 : var11 + 1.0;
                     var23 = var17 - var13;
                     var27 = var21 - var15;
                     break;
                  case COUNTERCLOCKWISE_90:
                     var17 = var15 < 0.0 ? var9 + 1.0 : var9;
                     var21 = var13 < 0.0 ? var11 : var11 + 1.0;
                     var23 = var17 + var15;
                     var27 = var21 - var13;
                     break;
                  default:
                     var17 = var13 < 0.0 ? var9 + 1.0 : var9;
                     var21 = var15 < 0.0 ? var11 + 1.0 : var11;
                     var23 = var17 + var13;
                     var27 = var21 + var15;
               }

               float var29 = 1.0F;
               float var30 = 0.9F;
               float var31 = 0.5F;
               VertexConsumer var32 = param3.getBuffer(RenderType.lines());
               if (param0.getMode() == StructureMode.SAVE || param0.getShowBoundingBox()) {
                  LevelRenderer.renderLineBox(param2, var32, var17, var19, var21, var23, var25, var27, 0.9F, 0.9F, 0.9F, 1.0F, 0.5F, 0.5F, 0.5F);
               }

               if (param0.getMode() == StructureMode.SAVE && param0.getShowAir()) {
                  this.renderInvisibleBlocks(param0, var32, var7, param2);
               }
            }
         }
      }
   }

   private void renderInvisibleBlocks(StructureBlockEntity param0, VertexConsumer param1, BlockPos param2, PoseStack param3) {
      BlockGetter var5 = param0.getLevel();
      BlockPos var6 = param0.getBlockPos();
      BlockPos var7 = var6.offset(param2);

      for (BlockPos var9 : BlockPos.betweenClosed(var7, var7.offset(param0.getStructureSize()).offset(-1, -1, -1))) {
         BlockState var10 = var5.getBlockState(var9);
         boolean var11 = var10.isAir();
         boolean var12 = var10.is(Blocks.STRUCTURE_VOID);
         boolean var13 = var10.is(Blocks.BARRIER);
         boolean var14 = var10.is(Blocks.LIGHT);
         boolean var15 = var12 || var13 || var14;
         if (var11 || var15) {
            float var16 = var11 ? 0.05F : 0.0F;
            double var17 = var9.getX() - var6.getX() + 0.45F - var16;
            double var19 = var9.getY() - var6.getY() + 0.45F - var16;
            double var21 = var9.getZ() - var6.getZ() + 0.45F - var16;
            double var23 = var9.getX() - var6.getX() + 0.55F + var16;
            double var25 = var9.getY() - var6.getY() + 0.55F + var16;
            double var27 = var9.getZ() - var6.getZ() + 0.55F + var16;
            if (var11) {
               LevelRenderer.renderLineBox(param3, param1, var17, var19, var21, var23, var25, var27, 0.5F, 0.5F, 1.0F, 1.0F, 0.5F, 0.5F, 1.0F);
            } else if (var12) {
               LevelRenderer.renderLineBox(param3, param1, var17, var19, var21, var23, var25, var27, 1.0F, 0.75F, 0.75F, 1.0F, 1.0F, 0.75F, 0.75F);
            } else if (var13) {
               LevelRenderer.renderLineBox(param3, param1, var17, var19, var21, var23, var25, var27, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F);
            } else if (var14) {
               LevelRenderer.renderLineBox(param3, param1, var17, var19, var21, var23, var25, var27, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F);
            }
         }
      }
   }

   public boolean shouldRenderOffScreen(StructureBlockEntity param0) {
      return true;
   }

   @Override
   public int getViewDistance() {
      return 96;
   }
}

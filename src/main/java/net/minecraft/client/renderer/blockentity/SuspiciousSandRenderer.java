package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SuspiciousSandBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SuspiciousSandRenderer implements BlockEntityRenderer<SuspiciousSandBlockEntity> {
   private final ItemRenderer itemRenderer;

   public SuspiciousSandRenderer(BlockEntityRendererProvider.Context param0) {
      this.itemRenderer = param0.getItemRenderer();
   }

   public void render(SuspiciousSandBlockEntity param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      if (param0.getLevel() != null) {
         int var7 = param0.getBlockState().getValue(BlockStateProperties.DUSTED);
         if (var7 > 0) {
            Direction var8 = param0.getHitDirection();
            if (var8 != null) {
               ItemStack var9 = param0.getItem();
               if (!var9.isEmpty()) {
                  param2.pushPose();
                  param2.translate(0.0F, 0.5F, 0.0F);
                  float[] var10 = this.translations(var8, var7);
                  param2.translate(var10[0], var10[1], var10[2]);
                  param2.mulPose(Axis.YP.rotationDegrees(75.0F));
                  boolean var11 = var8 == Direction.EAST || var8 == Direction.WEST;
                  param2.mulPose(Axis.YP.rotationDegrees((var11 ? 90 : 0) + 11));
                  param2.scale(0.5F, 0.5F, 0.5F);
                  int var12 = LevelRenderer.getLightColor(param0.getLevel(), param0.getBlockState(), param0.getBlockPos().relative(var8));
                  this.itemRenderer.renderStatic(var9, ItemDisplayContext.FIXED, var12, OverlayTexture.NO_OVERLAY, param2, param3, param0.getLevel(), 0);
                  param2.popPose();
               }
            }
         }
      }
   }

   private float[] translations(Direction param0, int param1) {
      float[] var3 = new float[]{0.5F, 0.0F, 0.5F};
      float var4 = param1 / 10.0F * 0.75F;
      switch (param0) {
         case EAST:
            var3[0] = 0.73F + var4;
            break;
         case WEST:
            var3[0] = 0.25F - var4;
            break;
         case UP:
            var3[1] = 0.25F + var4;
            break;
         case DOWN:
            var3[1] = -0.23F - var4;
            break;
         case NORTH:
            var3[2] = 0.25F - var4;
            break;
         case SOUTH:
            var3[2] = 0.73F + var4;
      }

      return var3;
   }
}

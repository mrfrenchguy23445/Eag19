package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

public class CampfireRenderer implements BlockEntityRenderer<CampfireBlockEntity> {
   private static final float SIZE = 0.375F;
   private final ItemRenderer itemRenderer;

   public CampfireRenderer(BlockEntityRendererProvider.Context param0) {
      this.itemRenderer = param0.getItemRenderer();
   }

   public void render(CampfireBlockEntity param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      Direction var7 = param0.getBlockState().getValue(CampfireBlock.FACING);
      NonNullList<ItemStack> var8 = param0.getItems();
      int var9 = (int)param0.getBlockPos().asLong();

      for (int var10 = 0; var10 < var8.size(); var10++) {
         ItemStack var11 = var8.get(var10);
         if (var11 != ItemStack.EMPTY) {
            param2.pushPose();
            param2.translate(0.5F, 0.44921875F, 0.5F);
            Direction var12 = Direction.from2DDataValue((var10 + var7.get2DDataValue()) % 4);
            float var13 = -var12.toYRot();
            param2.mulPose(Axis.YP.rotationDegrees(var13));
            param2.mulPose(Axis.XP.rotationDegrees(90.0F));
            param2.translate(-0.3125F, -0.3125F, 0.0F);
            param2.scale(0.375F, 0.375F, 0.375F);
            this.itemRenderer.renderStatic(var11, ItemDisplayContext.FIXED, param4, param5, param2, param3, param0.getLevel(), var9 + var10);
            param2.popPose();
         }
      }
   }
}

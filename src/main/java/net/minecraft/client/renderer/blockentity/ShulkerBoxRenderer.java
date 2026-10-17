package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ShulkerBoxRenderer implements BlockEntityRenderer<ShulkerBoxBlockEntity> {
   private final ShulkerModel<?> model;

   public ShulkerBoxRenderer(BlockEntityRendererProvider.Context param0) {
      this.model = new ShulkerModel(param0.bakeLayer(ModelLayers.SHULKER));
   }

   public void render(ShulkerBoxBlockEntity param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      Direction var7 = Direction.UP;
      if (param0.hasLevel()) {
         BlockState var8 = param0.getLevel().getBlockState(param0.getBlockPos());
         if (var8.getBlock() instanceof ShulkerBoxBlock) {
            var7 = var8.getValue(ShulkerBoxBlock.FACING);
         }
      }

      DyeColor var9 = param0.getColor();
      Material var8;
      if (var9 == null) {
         var8 = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION;
      } else {
         var8 = Sheets.SHULKER_TEXTURE_LOCATION.get(var9.getId());
      }

      param2.pushPose();
      param2.translate(0.5F, 0.5F, 0.5F);
      float var10 = 0.9995F;
      param2.scale(0.9995F, 0.9995F, 0.9995F);
      param2.mulPose(var7.getRotation());
      param2.scale(1.0F, -1.0F, -1.0F);
      param2.translate(0.0F, -1.0F, 0.0F);
      ModelPart var11 = this.model.getLid();
      var11.setPos(0.0F, 24.0F - param0.getProgress(param1) * 0.5F * 16.0F, 0.0F);
      var11.yRot = 270.0F * param0.getProgress(param1) * (float) (Math.PI / 180.0);
      VertexConsumer var12 = var8.buffer(param3, RenderType::entityCutoutNoCull);
      this.model.renderToBuffer(param2, var12, param4, param5, 1.0F, 1.0F, 1.0F, 1.0F);
      param2.popPose();
   }
}

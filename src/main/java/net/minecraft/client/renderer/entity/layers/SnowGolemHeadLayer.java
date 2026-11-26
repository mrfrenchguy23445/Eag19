package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SnowGolemHeadLayer extends RenderLayer<SnowGolem, SnowGolemModel<SnowGolem>> {
   private final BlockRenderDispatcher blockRenderer;
   private final ItemRenderer itemRenderer;

   public SnowGolemHeadLayer(RenderLayerParent<SnowGolem, SnowGolemModel<SnowGolem>> param0, BlockRenderDispatcher param1, ItemRenderer param2) {
      super(param0);
      this.blockRenderer = param1;
      this.itemRenderer = param2;
   }

   public void render(
      PoseStack param0,
      MultiBufferSource param1,
      int param2,
      SnowGolem param3,
      float param4,
      float param5,
      float param6,
      float param7,
      float param8,
      float param9
   ) {
      if (param3.hasPumpkin()) {
         boolean var11 = Minecraft.getInstance().shouldEntityAppearGlowing(param3) && param3.isInvisible();
         if (!param3.isInvisible() || var11) {
            param0.pushPose();
            this.getParentModel().getHead().translateAndRotate(param0);
            float var12 = 0.625F;
            param0.translate(0.0F, -0.34375F, 0.0F);
            param0.mulPose(Axis.YP.rotationDegrees(180.0F));
            param0.scale(0.625F, -0.625F, -0.625F);
            ItemStack var13 = new ItemStack(Blocks.CARVED_PUMPKIN);
            if (var11) {
               BlockState var14 = Blocks.CARVED_PUMPKIN.defaultBlockState();
               BakedModel var15 = this.blockRenderer.getBlockModel(var14);
               int var16 = LivingEntityRenderer.getOverlayCoords(param3, 0.0F);
               param0.translate(-0.5F, -0.5F, -0.5F);
               this.blockRenderer
                  .getModelRenderer()
                  .renderModel(param0.last(), param1.getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS)), var14, var15, 0.0F, 0.0F, 0.0F, param2, var16);
            } else {
               this.itemRenderer
                  .renderStatic(
                     param3,
                     var13,
                     ItemDisplayContext.HEAD,
                     false,
                     param0,
                     param1,
                     param3.level,
                     param2,
                     LivingEntityRenderer.getOverlayCoords(param3, 0.0F),
                     param3.getId()
                  );
            }

            param0.popPose();
         }
      }
   }
}

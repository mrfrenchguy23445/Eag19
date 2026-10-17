package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class FallingBlockRenderer extends EntityRenderer<FallingBlockEntity> {
   private final BlockRenderDispatcher dispatcher;

   public FallingBlockRenderer(EntityRendererProvider.Context param0) {
      super(param0);
      this.shadowRadius = 0.5F;
      this.dispatcher = param0.getBlockRenderDispatcher();
   }

   public void render(FallingBlockEntity param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      BlockState var7 = param0.getBlockState();
      if (var7.getRenderShape() == RenderShape.MODEL) {
         Level var8 = param0.getLevel();
         if (var7 != var8.getBlockState(param0.blockPosition()) && var7.getRenderShape() != RenderShape.INVISIBLE) {
            param3.pushPose();
            BlockPos var9 = BlockPos.containing(param0.getX(), param0.getBoundingBox().maxY, param0.getZ());
            param3.translate(-0.5, 0.0, -0.5);
            this.dispatcher
               .getModelRenderer()
               .tesselateBlock(
                  var8,
                  this.dispatcher.getBlockModel(var7),
                  var7,
                  var9,
                  param3,
                  param4.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(var7)),
                  false,
                  RandomSource.create(),
                  var7.getSeed(param0.getStartPos()),
                  OverlayTexture.NO_OVERLAY
               );
            param3.popPose();
            super.render(param0, param1, param2, param3, param4, param5);
         }
      }
   }

   public ResourceLocation getTextureLocation(FallingBlockEntity param0) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}

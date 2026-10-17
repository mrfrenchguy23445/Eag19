package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;

public class PistonHeadRenderer implements BlockEntityRenderer<PistonMovingBlockEntity> {
   private final BlockRenderDispatcher blockRenderer;

   public PistonHeadRenderer(BlockEntityRendererProvider.Context param0) {
      this.blockRenderer = param0.getBlockRenderDispatcher();
   }

   public void render(PistonMovingBlockEntity param0, float param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      Level var7 = param0.getLevel();
      if (var7 != null) {
         BlockPos var8 = param0.getBlockPos().relative(param0.getMovementDirection().getOpposite());
         BlockState var9 = param0.getMovedState();
         if (!var9.isAir()) {
            ModelBlockRenderer.enableCaching();
            param2.pushPose();
            param2.translate(param0.getXOff(param1), param0.getYOff(param1), param0.getZOff(param1));
            if (var9.is(Blocks.PISTON_HEAD) && param0.getProgress(param1) <= 4.0F) {
               var9 = var9.setValue(PistonHeadBlock.SHORT, param0.getProgress(param1) <= 0.5F);
               this.renderBlock(var8, var9, param2, param3, var7, false, param5);
            } else if (param0.isSourcePiston() && !param0.isExtending()) {
               PistonType var10 = var9.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT;
               BlockState var11 = Blocks.PISTON_HEAD
                  .defaultBlockState()
                  .setValue(PistonHeadBlock.TYPE, var10)
                  .setValue(PistonHeadBlock.FACING, var9.getValue(PistonBaseBlock.FACING));
               var11 = var11.setValue(PistonHeadBlock.SHORT, param0.getProgress(param1) >= 0.5F);
               this.renderBlock(var8, var11, param2, param3, var7, false, param5);
               BlockPos var12 = var8.relative(param0.getMovementDirection());
               param2.popPose();
               param2.pushPose();
               var9 = var9.setValue(PistonBaseBlock.EXTENDED, true);
               this.renderBlock(var12, var9, param2, param3, var7, true, param5);
            } else {
               this.renderBlock(var8, var9, param2, param3, var7, false, param5);
            }

            param2.popPose();
            ModelBlockRenderer.clearCache();
         }
      }
   }

   private void renderBlock(BlockPos param0, BlockState param1, PoseStack param2, MultiBufferSource param3, Level param4, boolean param5, int param6) {
      RenderType var8 = ItemBlockRenderTypes.getMovingBlockRenderType(param1);
      VertexConsumer var9 = param3.getBuffer(var8);
      this.blockRenderer
         .getModelRenderer()
         .tesselateBlock(
            param4, this.blockRenderer.getBlockModel(param1), param1, param0, param2, var9, param5, RandomSource.create(), param1.getSeed(param0), param6
         );
   }

   @Override
   public int getViewDistance() {
      return 68;
   }
}

package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class BlockRenderDispatcher implements ResourceManagerReloadListener {
   private final BlockModelShaper blockModelShaper;
   private final ModelBlockRenderer modelRenderer;
   private final BlockEntityWithoutLevelRenderer blockEntityRenderer;
   private final LiquidBlockRenderer liquidBlockRenderer;
   private final RandomSource random = RandomSource.create();
   private final BlockColors blockColors;

   public BlockRenderDispatcher(BlockModelShaper param0, BlockEntityWithoutLevelRenderer param1, BlockColors param2) {
      this.blockModelShaper = param0;
      this.blockEntityRenderer = param1;
      this.blockColors = param2;
      this.modelRenderer = new ModelBlockRenderer(this.blockColors);
      this.liquidBlockRenderer = new LiquidBlockRenderer();
   }

   public BlockModelShaper getBlockModelShaper() {
      return this.blockModelShaper;
   }

   public void renderBreakingTexture(BlockState param0, BlockPos param1, BlockAndTintGetter param2, PoseStack param3, VertexConsumer param4) {
      if (param0.getRenderShape() == RenderShape.MODEL) {
         BakedModel var6 = this.blockModelShaper.getBlockModel(param0);
         long var7 = param0.getSeed(param1);
         this.modelRenderer.tesselateBlock(param2, var6, param0, param1, param3, param4, true, this.random, var7, OverlayTexture.NO_OVERLAY);
      }
   }

   public void renderBatched(
      BlockState param0, BlockPos param1, BlockAndTintGetter param2, PoseStack param3, VertexConsumer param4, boolean param5, RandomSource param6
   ) {
      try {
         RenderShape var8 = param0.getRenderShape();
         if (var8 == RenderShape.MODEL) {
            this.modelRenderer
               .tesselateBlock(
                  param2, this.getBlockModel(param0), param0, param1, param3, param4, param5, param6, param0.getSeed(param1), OverlayTexture.NO_OVERLAY
               );
         }
      } catch (Throwable var8) {
         CrashReport var9 = CrashReport.forThrowable(var8, "Tesselating block in world");
         CrashReportCategory var10 = var9.addCategory("Block being tesselated");
         CrashReportCategory.populateBlockDetails(var10, param2, param1, param0);
         throw new ReportedException(var9);
      }
   }

   public void renderLiquid(BlockPos param0, BlockAndTintGetter param1, VertexConsumer param2, BlockState param3, FluidState param4) {
      try {
         this.liquidBlockRenderer.tesselate(param1, param0, param2, param3, param4);
      } catch (Throwable var6) {
         CrashReport var7 = CrashReport.forThrowable(var6, "Tesselating liquid in world");
         CrashReportCategory var8 = var7.addCategory("Block being tesselated");
         CrashReportCategory.populateBlockDetails(var8, param1, param0, null);
         throw new ReportedException(var7);
      }
   }

   public ModelBlockRenderer getModelRenderer() {
      return this.modelRenderer;
   }

   public BakedModel getBlockModel(BlockState param0) {
      return this.blockModelShaper.getBlockModel(param0);
   }

   public void renderSingleBlock(BlockState param0, PoseStack param1, MultiBufferSource param2, int param3, int param4) {
      RenderShape var6 = param0.getRenderShape();
      if (var6 != RenderShape.INVISIBLE) {
         switch (var6) {
            case MODEL:
               BakedModel var7 = this.getBlockModel(param0);
               int var8 = this.blockColors.getColor(param0, null, null, 0);
               float var9 = (var8 >> 16 & 0xFF) / 255.0F;
               float var10 = (var8 >> 8 & 0xFF) / 255.0F;
               float var11 = (var8 & 0xFF) / 255.0F;
               this.modelRenderer
                  .renderModel(
                     param1.last(), param2.getBuffer(ItemBlockRenderTypes.getRenderType(param0, false)), param0, var7, var9, var10, var11, param3, param4
                  );
               break;
            case ENTITYBLOCK_ANIMATED:
               this.blockEntityRenderer.renderByItem(new ItemStack(param0.getBlock()), ItemDisplayContext.NONE, param1, param2, param3, param4);
         }
      }
   }

   @Override
   public void onResourceManagerReload(ResourceManager param0) {
      this.liquidBlockRenderer.setupSprites();
   }
}

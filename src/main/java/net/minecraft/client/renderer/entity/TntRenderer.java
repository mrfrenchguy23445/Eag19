package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;

public class TntRenderer extends EntityRenderer<PrimedTnt> {
   private final BlockRenderDispatcher blockRenderer;

   public TntRenderer(EntityRendererProvider.Context param0) {
      super(param0);
      this.shadowRadius = 0.5F;
      this.blockRenderer = param0.getBlockRenderDispatcher();
   }

   public void render(PrimedTnt param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      param3.pushPose();
      param3.translate(0.0F, 0.5F, 0.0F);
      int var7 = param0.getFuse();
      if (var7 - param2 + 1.0F < 10.0F) {
         float var8 = 1.0F - (var7 - param2 + 1.0F) / 10.0F;
         var8 = Mth.clamp(var8, 0.0F, 1.0F);
         var8 *= var8;
         var8 *= var8;
         float var9 = 1.0F + var8 * 0.3F;
         param3.scale(var9, var9, var9);
      }

      param3.mulPose(Axis.YP.rotationDegrees(-90.0F));
      param3.translate(-0.5F, -0.5F, 0.5F);
      param3.mulPose(Axis.YP.rotationDegrees(90.0F));
      TntMinecartRenderer.renderWhiteSolidBlock(this.blockRenderer, Blocks.TNT.defaultBlockState(), param3, param4, param5, var7 / 5 % 2 == 0);
      param3.popPose();
      super.render(param0, param1, param2, param3, param4, param5);
   }

   public ResourceLocation getTextureLocation(PrimedTnt param0) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}

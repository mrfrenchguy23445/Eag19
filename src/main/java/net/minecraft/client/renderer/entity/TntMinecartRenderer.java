package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.block.state.BlockState;

public class TntMinecartRenderer extends MinecartRenderer<MinecartTNT> {
   private final BlockRenderDispatcher blockRenderer;

   public TntMinecartRenderer(EntityRendererProvider.Context param0) {
      super(param0, ModelLayers.TNT_MINECART);
      this.blockRenderer = param0.getBlockRenderDispatcher();
   }

   protected void renderMinecartContents(MinecartTNT param0, float param1, BlockState param2, PoseStack param3, MultiBufferSource param4, int param5) {
      int var7 = param0.getFuse();
      if (var7 > -1 && var7 - param1 + 1.0F < 10.0F) {
         float var8 = 1.0F - (var7 - param1 + 1.0F) / 10.0F;
         var8 = Mth.clamp(var8, 0.0F, 1.0F);
         var8 *= var8;
         var8 *= var8;
         float var9 = 1.0F + var8 * 0.3F;
         param3.scale(var9, var9, var9);
      }

      renderWhiteSolidBlock(this.blockRenderer, param2, param3, param4, param5, var7 > -1 && var7 / 5 % 2 == 0);
   }

   public static void renderWhiteSolidBlock(
      BlockRenderDispatcher param0, BlockState param1, PoseStack param2, MultiBufferSource param3, int param4, boolean param5
   ) {
      int var6;
      if (param5) {
         var6 = OverlayTexture.pack(OverlayTexture.u(1.0F), 10);
      } else {
         var6 = OverlayTexture.NO_OVERLAY;
      }

      param0.renderSingleBlock(param1, param2, param3, param4, var6);
   }
}

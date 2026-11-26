package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.SquidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.GlowSquid;

public class GlowSquidRenderer extends SquidRenderer<GlowSquid> {
   private static final ResourceLocation GLOW_SQUID_LOCATION = new ResourceLocation("textures/entity/squid/glow_squid.png");

   public GlowSquidRenderer(EntityRendererProvider.Context param0, SquidModel<GlowSquid> param1) {
      super(param0, param1);
   }

   public ResourceLocation getTextureLocation(GlowSquid param0) {
      return GLOW_SQUID_LOCATION;
   }

   protected int getBlockLightLevel(GlowSquid param0, BlockPos param1) {
      int var3 = (int)Mth.clampedLerp(0.0F, 15.0F, 1.0F - param0.getDarkTicksRemaining() / 10.0F);
      return var3 == 15 ? 15 : Math.max(var3, super.getBlockLightLevel(param0, param1));
   }
}

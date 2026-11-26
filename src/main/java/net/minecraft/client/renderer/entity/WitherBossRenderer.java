package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.WitherArmorLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public class WitherBossRenderer extends MobRenderer<WitherBoss, WitherBossModel<WitherBoss>> {
   private static final ResourceLocation WITHER_INVULNERABLE_LOCATION = new ResourceLocation("textures/entity/wither/wither_invulnerable.png");
   private static final ResourceLocation WITHER_LOCATION = new ResourceLocation("textures/entity/wither/wither.png");

   public WitherBossRenderer(EntityRendererProvider.Context param0) {
      super(param0, new WitherBossModel<>(param0.bakeLayer(ModelLayers.WITHER)), 1.0F);
      this.addLayer(new WitherArmorLayer(this, param0.getModelSet()));
   }

   protected int getBlockLightLevel(WitherBoss param0, BlockPos param1) {
      return 15;
   }

   public ResourceLocation getTextureLocation(WitherBoss param0) {
      int var2 = param0.getInvulnerableTicks();
      return var2 > 0 && (var2 > 80 || var2 / 5 % 2 != 1) ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;
   }

   protected void scale(WitherBoss param0, PoseStack param1, float param2) {
      float var4 = 2.0F;
      int var5 = param0.getInvulnerableTicks();
      if (var5 > 0) {
         var4 -= (var5 - param2) / 220.0F * 0.5F;
      }

      param1.scale(var4, var4, var4);
   }
}

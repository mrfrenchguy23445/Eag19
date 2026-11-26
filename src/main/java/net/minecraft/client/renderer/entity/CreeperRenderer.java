package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;

public class CreeperRenderer extends MobRenderer<Creeper, CreeperModel<Creeper>> {
   private static final ResourceLocation CREEPER_LOCATION = new ResourceLocation("textures/entity/creeper/creeper.png");

   public CreeperRenderer(EntityRendererProvider.Context param0) {
      super(param0, new CreeperModel<>(param0.bakeLayer(ModelLayers.CREEPER)), 0.5F);
      this.addLayer(new CreeperPowerLayer(this, param0.getModelSet()));
   }

   protected void scale(Creeper param0, PoseStack param1, float param2) {
      float var4 = param0.getSwelling(param2);
      float var5 = 1.0F + Mth.sin(var4 * 100.0F) * var4 * 0.01F;
      var4 = Mth.clamp(var4, 0.0F, 1.0F);
      var4 *= var4;
      var4 *= var4;
      float var6 = (1.0F + var4 * 0.4F) * var5;
      float var7 = (1.0F + var4 * 0.1F) / var5;
      param1.scale(var6, var7, var6);
   }

   protected float getWhiteOverlayProgress(Creeper param0, float param1) {
      float var3 = param0.getSwelling(param1);
      return (int)(var3 * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(var3, 0.5F, 1.0F);
   }

   public ResourceLocation getTextureLocation(Creeper param0) {
      return CREEPER_LOCATION;
   }
}

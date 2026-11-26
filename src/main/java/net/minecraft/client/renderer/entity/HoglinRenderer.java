package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HoglinModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

public class HoglinRenderer extends MobRenderer<Hoglin, HoglinModel<Hoglin>> {
   private static final ResourceLocation HOGLIN_LOCATION = new ResourceLocation("textures/entity/hoglin/hoglin.png");

   public HoglinRenderer(EntityRendererProvider.Context param0) {
      super(param0, new HoglinModel<>(param0.bakeLayer(ModelLayers.HOGLIN)), 0.7F);
   }

   public ResourceLocation getTextureLocation(Hoglin param0) {
      return HOGLIN_LOCATION;
   }

   protected boolean isShaking(Hoglin param0) {
      return super.isShaking(param0) || param0.isConverting();
   }
}

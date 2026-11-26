package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Chicken;

public class ChickenRenderer extends MobRenderer<Chicken, ChickenModel<Chicken>> {
   private static final ResourceLocation CHICKEN_LOCATION = new ResourceLocation("textures/entity/chicken.png");

   public ChickenRenderer(EntityRendererProvider.Context param0) {
      super(param0, new ChickenModel<>(param0.bakeLayer(ModelLayers.CHICKEN)), 0.3F);
   }

   public ResourceLocation getTextureLocation(Chicken param0) {
      return CHICKEN_LOCATION;
   }

   protected float getBob(Chicken param0, float param1) {
      float var3 = Mth.lerp(param1, param0.oFlap, param0.flap);
      float var4 = Mth.lerp(param1, param0.oFlapSpeed, param0.flapSpeed);
      return (Mth.sin(var3) + 1.0F) * var4;
   }
}

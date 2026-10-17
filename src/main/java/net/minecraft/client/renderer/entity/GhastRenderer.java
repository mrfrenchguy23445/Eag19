package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.GhastModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Ghast;

public class GhastRenderer extends MobRenderer<Ghast, GhastModel<Ghast>> {
   private static final ResourceLocation GHAST_LOCATION = new ResourceLocation("textures/entity/ghast/ghast.png");
   private static final ResourceLocation GHAST_SHOOTING_LOCATION = new ResourceLocation("textures/entity/ghast/ghast_shooting.png");

   public GhastRenderer(EntityRendererProvider.Context param0) {
      super(param0, new GhastModel<>(param0.bakeLayer(ModelLayers.GHAST)), 1.5F);
   }

   public ResourceLocation getTextureLocation(Ghast param0) {
      return param0.isCharging() ? GHAST_SHOOTING_LOCATION : GHAST_LOCATION;
   }

   protected void scale(Ghast param0, PoseStack param1, float param2) {
      float var4 = 1.0F;
      float var5 = 4.5F;
      float var6 = 4.5F;
      param1.scale(4.5F, 4.5F, 4.5F);
   }
}

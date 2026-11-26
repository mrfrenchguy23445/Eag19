package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.WardenEmissiveLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.warden.Warden;

public class WardenRenderer extends MobRenderer<Warden, WardenModel<Warden>> {
   private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/warden/warden.png");
   private static final ResourceLocation BIOLUMINESCENT_LAYER_TEXTURE = new ResourceLocation("textures/entity/warden/warden_bioluminescent_layer.png");
   private static final ResourceLocation HEART_TEXTURE = new ResourceLocation("textures/entity/warden/warden_heart.png");
   private static final ResourceLocation PULSATING_SPOTS_TEXTURE_1 = new ResourceLocation("textures/entity/warden/warden_pulsating_spots_1.png");
   private static final ResourceLocation PULSATING_SPOTS_TEXTURE_2 = new ResourceLocation("textures/entity/warden/warden_pulsating_spots_2.png");

   public WardenRenderer(EntityRendererProvider.Context param0) {
      super(param0, new WardenModel<>(param0.bakeLayer(ModelLayers.WARDEN)), 0.9F);
      this.addLayer(
         new WardenEmissiveLayer<>(this, BIOLUMINESCENT_LAYER_TEXTURE, (l_param0, l_param1, l_param2) -> 1.0F, WardenModel::getBioluminescentLayerModelParts)
      );
      this.addLayer(
         new WardenEmissiveLayer<>(
            this,
            PULSATING_SPOTS_TEXTURE_1,
            (l_param0, l_param1, l_param2) -> Math.max(0.0F, Mth.cos(l_param2 * 0.045F) * 0.25F),
            WardenModel::getPulsatingSpotsLayerModelParts
         )
      );
      this.addLayer(
         new WardenEmissiveLayer<>(
            this,
            PULSATING_SPOTS_TEXTURE_2,
            (l_param0, l_param1, l_param2) -> Math.max(0.0F, Mth.cos(l_param2 * 0.045F + (float) Math.PI) * 0.25F),
            WardenModel::getPulsatingSpotsLayerModelParts
         )
      );
      this.addLayer(
         new WardenEmissiveLayer<>(
            this, TEXTURE, (l_param0, l_param1, l_param2) -> l_param0.getTendrilAnimation(l_param1), WardenModel::getTendrilsLayerModelParts
         )
      );
      this.addLayer(
         new WardenEmissiveLayer<>(
            this, HEART_TEXTURE, (l_param0, l_param1, l_param2) -> l_param0.getHeartAnimation(l_param1), WardenModel::getHeartLayerModelParts
         )
      );
   }

   public ResourceLocation getTextureLocation(Warden param0) {
      return TEXTURE;
   }
}

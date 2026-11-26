package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Vindicator;

public class VindicatorRenderer extends IllagerRenderer<Vindicator> {
   private static final ResourceLocation VINDICATOR = new ResourceLocation("textures/entity/illager/vindicator.png");

   public VindicatorRenderer(EntityRendererProvider.Context param0) {
      super(param0, new IllagerModel<>(param0.bakeLayer(ModelLayers.VINDICATOR)), 0.5F);
      this.addLayer(
         new ItemInHandLayer<Vindicator, IllagerModel<Vindicator>>(this, param0.getItemInHandRenderer()) {
            public void render(
               PoseStack l_param0,
               MultiBufferSource l_param1,
               int l_param2,
               Vindicator l_param3,
               float l_param4,
               float l_param5,
               float l_param6,
               float l_param7,
               float l_param8,
               float l_param9
            ) {
               if (l_param3.isAggressive()) {
                  super.render(l_param0, l_param1, l_param2, l_param3, l_param4, l_param5, l_param6, l_param7, l_param8, l_param9);
               }
            }
         }
      );
   }

   public ResourceLocation getTextureLocation(Vindicator param0) {
      return VINDICATOR;
   }
}

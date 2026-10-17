package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

public class CatCollarLayer extends RenderLayer<Cat, CatModel<Cat>> {
   private static final ResourceLocation CAT_COLLAR_LOCATION = new ResourceLocation("textures/entity/cat/cat_collar.png");
   private final CatModel<Cat> catModel;

   public CatCollarLayer(RenderLayerParent<Cat, CatModel<Cat>> param0, EntityModelSet param1) {
      super(param0);
      this.catModel = new CatModel<>(param1.bakeLayer(ModelLayers.CAT_COLLAR));
   }

   public void render(
      PoseStack param0, MultiBufferSource param1, int param2, Cat param3, float param4, float param5, float param6, float param7, float param8, float param9
   ) {
      if (param3.isTame()) {
         float[] var11 = param3.getCollarColor().getTextureDiffuseColors();
         coloredCutoutModelCopyLayerRender(
            this.getParentModel(),
            this.catModel,
            CAT_COLLAR_LOCATION,
            param0,
            param1,
            param2,
            param3,
            param4,
            param5,
            param7,
            param8,
            param9,
            param6,
            var11[0],
            var11[1],
            var11[2]
         );
      }
   }
}

package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;

public class SlimeOuterLayer<T extends LivingEntity> extends RenderLayer<T, SlimeModel<T>> {
   private final EntityModel<T> model;

   public SlimeOuterLayer(RenderLayerParent<T, SlimeModel<T>> param0, EntityModelSet param1) {
      super(param0);
      this.model = new SlimeModel<>(param1.bakeLayer(ModelLayers.SLIME_OUTER));
   }

   public void render(
      PoseStack param0, MultiBufferSource param1, int param2, T param3, float param4, float param5, float param6, float param7, float param8, float param9
   ) {
      Minecraft var11 = Minecraft.getInstance();
      boolean var12 = var11.shouldEntityAppearGlowing(param3) && param3.isInvisible();
      if (!param3.isInvisible() || var12) {
         VertexConsumer var13;
         if (var12) {
            var13 = param1.getBuffer(RenderType.outline(this.getTextureLocation(param3)));
         } else {
            var13 = param1.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(param3)));
         }

         this.getParentModel().copyPropertiesTo(this.model);
         this.model.prepareMobModel(param3, param4, param5, param6);
         this.model.setupAnim(param3, param4, param5, param7, param8, param9);
         this.model.renderToBuffer(param0, var13, param2, LivingEntityRenderer.getOverlayCoords(param3, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
      }
   }
}

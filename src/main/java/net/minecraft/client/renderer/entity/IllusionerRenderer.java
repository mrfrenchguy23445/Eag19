package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.phys.Vec3;

public class IllusionerRenderer extends IllagerRenderer<Illusioner> {
   private static final ResourceLocation ILLUSIONER = new ResourceLocation("textures/entity/illager/illusioner.png");

   public IllusionerRenderer(EntityRendererProvider.Context param0) {
      super(param0, new IllagerModel<>(param0.bakeLayer(ModelLayers.ILLUSIONER)), 0.5F);
      this.addLayer(
         new ItemInHandLayer<Illusioner, IllagerModel<Illusioner>>(this, param0.getItemInHandRenderer()) {
            public void render(
               PoseStack l_param0,
               MultiBufferSource l_param1,
               int l_param2,
               Illusioner l_param3,
               float l_param4,
               float l_param5,
               float l_param6,
               float l_param7,
               float l_param8,
               float l_param9
            ) {
               if (l_param3.isCastingSpell() || l_param3.isAggressive()) {
                  super.render(l_param0, l_param1, l_param2, l_param3, l_param4, l_param5, l_param6, l_param7, l_param8, l_param9);
               }
            }
         }
      );
      this.model.getHat().visible = true;
   }

   public ResourceLocation getTextureLocation(Illusioner param0) {
      return ILLUSIONER;
   }

   public void render(Illusioner param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      if (param0.isInvisible()) {
         Vec3[] var7 = param0.getIllusionOffsets(param2);
         float var8 = this.getBob(param0, param2);

         for (int var9 = 0; var9 < var7.length; var9++) {
            param3.pushPose();
            param3.translate(
               var7[var9].x + Mth.cos(var9 + var8 * 0.5F) * 0.025,
               var7[var9].y + Mth.cos(var9 + var8 * 0.75F) * 0.0125,
               var7[var9].z + Mth.cos(var9 + var8 * 0.7F) * 0.025
            );
            super.render(param0, param1, param2, param3, param4, param5);
            param3.popPose();
         }
      } else {
         super.render(param0, param1, param2, param3, param4, param5);
      }
   }

   protected boolean isBodyVisible(Illusioner param0) {
      return true;
   }
}

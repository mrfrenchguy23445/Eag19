package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.SalmonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Salmon;

public class SalmonRenderer extends MobRenderer<Salmon, SalmonModel<Salmon>> {
   private static final ResourceLocation SALMON_LOCATION = new ResourceLocation("textures/entity/fish/salmon.png");

   public SalmonRenderer(EntityRendererProvider.Context param0) {
      super(param0, new SalmonModel<>(param0.bakeLayer(ModelLayers.SALMON)), 0.4F);
   }

   public ResourceLocation getTextureLocation(Salmon param0) {
      return SALMON_LOCATION;
   }

   protected void setupRotations(Salmon param0, PoseStack param1, float param2, float param3, float param4) {
      super.setupRotations(param0, param1, param2, param3, param4);
      float var6 = 1.0F;
      float var7 = 1.0F;
      if (!param0.isInWater()) {
         var6 = 1.3F;
         var7 = 1.7F;
      }

      float var8 = var6 * 4.3F * Mth.sin(var7 * 0.6F * param2);
      param1.mulPose(Axis.YP.rotationDegrees(var8));
      param1.translate(0.0F, 0.0F, -0.4F);
      if (!param0.isInWater()) {
         param1.translate(0.2F, 0.1F, 0.0F);
         param1.mulPose(Axis.ZP.rotationDegrees(90.0F));
      }
   }
}

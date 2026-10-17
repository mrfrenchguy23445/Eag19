package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.PhantomEyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Phantom;

public class PhantomRenderer extends MobRenderer<Phantom, PhantomModel<Phantom>> {
   private static final ResourceLocation PHANTOM_LOCATION = new ResourceLocation("textures/entity/phantom.png");

   public PhantomRenderer(EntityRendererProvider.Context param0) {
      super(param0, new PhantomModel<>(param0.bakeLayer(ModelLayers.PHANTOM)), 0.75F);
      this.addLayer(new PhantomEyesLayer<>(this));
   }

   public ResourceLocation getTextureLocation(Phantom param0) {
      return PHANTOM_LOCATION;
   }

   protected void scale(Phantom param0, PoseStack param1, float param2) {
      int var4 = param0.getPhantomSize();
      float var5 = 1.0F + 0.15F * var4;
      param1.scale(var5, var5, var5);
      param1.translate(0.0F, 1.3125F, 0.1875F);
   }

   protected void setupRotations(Phantom param0, PoseStack param1, float param2, float param3, float param4) {
      super.setupRotations(param0, param1, param2, param3, param4);
      param1.mulPose(Axis.XP.rotationDegrees(param0.getXRot()));
   }
}

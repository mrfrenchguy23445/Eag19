package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ColorableHierarchicalModel;
import net.minecraft.client.model.TropicalFishModelA;
import net.minecraft.client.model.TropicalFishModelB;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.TropicalFish;

public class TropicalFishRenderer extends MobRenderer<TropicalFish, ColorableHierarchicalModel<TropicalFish>> {
   private final ColorableHierarchicalModel<TropicalFish> modelA = this.getModel();
   private final ColorableHierarchicalModel<TropicalFish> modelB;
   private static final ResourceLocation MODEL_A_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_a.png");
   private static final ResourceLocation MODEL_B_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_b.png");

   public TropicalFishRenderer(EntityRendererProvider.Context param0) {
      super(param0, new TropicalFishModelA<>(param0.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL)), 0.15F);
      this.modelB = new TropicalFishModelB<>(param0.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE));
      this.addLayer(new TropicalFishPatternLayer(this, param0.getModelSet()));
   }

   public ResourceLocation getTextureLocation(TropicalFish param0) {
      return switch (param0.getVariant().base()) {
         case SMALL -> MODEL_A_TEXTURE;
         case LARGE -> MODEL_B_TEXTURE;
      };
   }

   public void render(TropicalFish param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      ColorableHierarchicalModel<TropicalFish> var7 = switch (param0.getVariant().base()) {
         case SMALL -> this.modelA;
         case LARGE -> this.modelB;
      };
      this.model = var7;
      float[] var8 = param0.getBaseColor().getTextureDiffuseColors();
      var7.setColor(var8[0], var8[1], var8[2]);
      super.render(param0, param1, param2, param3, param4, param5);
      var7.setColor(1.0F, 1.0F, 1.0F);
   }

   protected void setupRotations(TropicalFish param0, PoseStack param1, float param2, float param3, float param4) {
      super.setupRotations(param0, param1, param2, param3, param4);
      float var6 = 4.3F * Mth.sin(0.6F * param2);
      param1.mulPose(Axis.YP.rotationDegrees(var6));
      if (!param0.isInWater()) {
         param1.translate(0.2F, 0.1F, 0.0F);
         param1.mulPose(Axis.ZP.rotationDegrees(90.0F));
      }
   }
}

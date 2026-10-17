package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;

public class SheepFurLayer extends RenderLayer<Sheep, SheepModel<Sheep>> {
   private static final ResourceLocation SHEEP_FUR_LOCATION = new ResourceLocation("textures/entity/sheep/sheep_fur.png");
   private final SheepFurModel<Sheep> model;

   public SheepFurLayer(RenderLayerParent<Sheep, SheepModel<Sheep>> param0, EntityModelSet param1) {
      super(param0);
      this.model = new SheepFurModel<>(param1.bakeLayer(ModelLayers.SHEEP_FUR));
   }

   public void render(
      PoseStack param0, MultiBufferSource param1, int param2, Sheep param3, float param4, float param5, float param6, float param7, float param8, float param9
   ) {
      if (!param3.isSheared()) {
         if (param3.isInvisible()) {
            Minecraft var11 = Minecraft.getInstance();
            boolean var12 = var11.shouldEntityAppearGlowing(param3);
            if (var12) {
               this.getParentModel().copyPropertiesTo(this.model);
               this.model.prepareMobModel(param3, param4, param5, param6);
               this.model.setupAnim(param3, param4, param5, param7, param8, param9);
               VertexConsumer var13 = param1.getBuffer(RenderType.outline(SHEEP_FUR_LOCATION));
               this.model.renderToBuffer(param0, var13, param2, LivingEntityRenderer.getOverlayCoords(param3, 0.0F), 0.0F, 0.0F, 0.0F, 1.0F);
            }
         } else {
            float var11;
            float var12;
            float var13;
            if (param3.hasCustomName() && "jeb_".equals(param3.getName().getString())) {
               int var14 = 25;
               int var15 = param3.tickCount / 25 + param3.getId();
               int var16 = DyeColor.values().length;
               int var17 = var15 % var16;
               int var18 = (var15 + 1) % var16;
               float var19 = (param3.tickCount % 25 + param6) / 25.0F;
               float[] var20 = Sheep.getColorArray(DyeColor.byId(var17));
               float[] var21 = Sheep.getColorArray(DyeColor.byId(var18));
               var11 = var20[0] * (1.0F - var19) + var21[0] * var19;
               var12 = var20[1] * (1.0F - var19) + var21[1] * var19;
               var13 = var20[2] * (1.0F - var19) + var21[2] * var19;
            } else {
               float[] var14 = Sheep.getColorArray(param3.getColor());
               var11 = var14[0];
               var12 = var14[1];
               var13 = var14[2];
            }

            coloredCutoutModelCopyLayerRender(
               this.getParentModel(),
               this.model,
               SHEEP_FUR_LOCATION,
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
               var11,
               var12,
               var13
            );
         }
      }
   }
}

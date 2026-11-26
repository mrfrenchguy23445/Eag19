package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.DyeableHorseArmorItem;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;

public class HorseArmorLayer extends RenderLayer<Horse, HorseModel<Horse>> {
   private final HorseModel<Horse> model;

   public HorseArmorLayer(RenderLayerParent<Horse, HorseModel<Horse>> param0, EntityModelSet param1) {
      super(param0);
      this.model = new HorseModel<>(param1.bakeLayer(ModelLayers.HORSE_ARMOR));
   }

   public void render(
      PoseStack param0, MultiBufferSource param1, int param2, Horse param3, float param4, float param5, float param6, float param7, float param8, float param9
   ) {
      ItemStack var11 = param3.getArmor();
      if (var11.getItem() instanceof HorseArmorItem) {
         HorseArmorItem var12 = (HorseArmorItem)var11.getItem();
         this.getParentModel().copyPropertiesTo(this.model);
         this.model.prepareMobModel(param3, param4, param5, param6);
         this.model.setupAnim(param3, param4, param5, param7, param8, param9);
         float var13;
         float var14;
         float var15;
         if (var12 instanceof DyeableHorseArmorItem) {
            int var16 = ((DyeableHorseArmorItem)var12).getColor(var11);
            var13 = (var16 >> 16 & 0xFF) / 255.0F;
            var14 = (var16 >> 8 & 0xFF) / 255.0F;
            var15 = (var16 & 0xFF) / 255.0F;
         } else {
            var13 = 1.0F;
            var14 = 1.0F;
            var15 = 1.0F;
         }

         VertexConsumer var16 = param1.getBuffer(RenderType.entityCutoutNoCull(var12.getTexture()));
         this.model.renderToBuffer(param0, var16, param2, OverlayTexture.NO_OVERLAY, var13, var14, var15, 1.0F);
      }
   }
}

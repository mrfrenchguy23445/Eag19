package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ElytraLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
   private static final ResourceLocation WINGS_LOCATION = new ResourceLocation("textures/entity/elytra.png");
   private final ElytraModel<T> elytraModel;

   public ElytraLayer(RenderLayerParent<T, M> param0, EntityModelSet param1) {
      super(param0);
      this.elytraModel = new ElytraModel<>(param1.bakeLayer(ModelLayers.ELYTRA));
   }

   public void render(
      PoseStack param0, MultiBufferSource param1, int param2, T param3, float param4, float param5, float param6, float param7, float param8, float param9
   ) {
      ItemStack var11 = param3.getItemBySlot(EquipmentSlot.CHEST);
      if (var11.is(Items.ELYTRA)) {
         ResourceLocation var12;
         if (param3 instanceof AbstractClientPlayer var13) {
            if (var13.isElytraLoaded() && var13.getElytraTextureLocation() != null) {
               var12 = var13.getElytraTextureLocation();
            } else if (var13.isCapeLoaded() && var13.getCloakTextureLocation() != null && var13.isModelPartShown(PlayerModelPart.CAPE)) {
               var12 = var13.getCloakTextureLocation();
            } else {
               var12 = WINGS_LOCATION;
            }
         } else {
            var12 = WINGS_LOCATION;
         }

         param0.pushPose();
         param0.translate(0.0F, 0.0F, 0.125F);
         this.getParentModel().copyPropertiesTo(this.elytraModel);
         this.elytraModel.setupAnim(param3, param4, param5, param7, param8, param9);
         VertexConsumer var13 = ItemRenderer.getArmorFoilBuffer(param1, RenderType.armorCutoutNoCull(var12), false, var11.hasFoil());
         this.elytraModel.renderToBuffer(param0, var13, param2, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
         param0.popPose();
      }
   }
}

package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.FoxModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class FoxHeldItemLayer extends RenderLayer<Fox, FoxModel<Fox>> {
   private final ItemInHandRenderer itemInHandRenderer;

   public FoxHeldItemLayer(RenderLayerParent<Fox, FoxModel<Fox>> param0, ItemInHandRenderer param1) {
      super(param0);
      this.itemInHandRenderer = param1;
   }

   public void render(
      PoseStack param0, MultiBufferSource param1, int param2, Fox param3, float param4, float param5, float param6, float param7, float param8, float param9
   ) {
      boolean var11 = param3.isSleeping();
      boolean var12 = param3.isBaby();
      param0.pushPose();
      if (var12) {
         float var13 = 0.75F;
         param0.scale(0.75F, 0.75F, 0.75F);
         param0.translate(0.0F, 0.5F, 0.209375F);
      }

      param0.translate(this.getParentModel().head.x / 16.0F, this.getParentModel().head.y / 16.0F, this.getParentModel().head.z / 16.0F);
      float var13 = param3.getHeadRollAngle(param6);
      param0.mulPose(Axis.ZP.rotation(var13));
      param0.mulPose(Axis.YP.rotationDegrees(param8));
      param0.mulPose(Axis.XP.rotationDegrees(param9));
      if (param3.isBaby()) {
         if (var11) {
            param0.translate(0.4F, 0.26F, 0.15F);
         } else {
            param0.translate(0.06F, 0.26F, -0.5F);
         }
      } else if (var11) {
         param0.translate(0.46F, 0.26F, 0.22F);
      } else {
         param0.translate(0.06F, 0.27F, -0.5F);
      }

      param0.mulPose(Axis.XP.rotationDegrees(90.0F));
      if (var11) {
         param0.mulPose(Axis.ZP.rotationDegrees(90.0F));
      }

      ItemStack var14 = param3.getItemBySlot(EquipmentSlot.MAINHAND);
      this.itemInHandRenderer.renderItem(param3, var14, ItemDisplayContext.GROUND, false, param0, param1, param2);
      param0.popPose();
   }
}

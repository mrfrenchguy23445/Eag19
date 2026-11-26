package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ItemInHandLayer<T extends LivingEntity, M extends EntityModel<T> & ArmedModel> extends RenderLayer<T, M> {
   private final ItemInHandRenderer itemInHandRenderer;

   public ItemInHandLayer(RenderLayerParent<T, M> param0, ItemInHandRenderer param1) {
      super(param0);
      this.itemInHandRenderer = param1;
   }

   public void render(
      PoseStack param0, MultiBufferSource param1, int param2, T param3, float param4, float param5, float param6, float param7, float param8, float param9
   ) {
      boolean var11 = param3.getMainArm() == HumanoidArm.RIGHT;
      ItemStack var12 = var11 ? param3.getOffhandItem() : param3.getMainHandItem();
      ItemStack var13 = var11 ? param3.getMainHandItem() : param3.getOffhandItem();
      if (!var12.isEmpty() || !var13.isEmpty()) {
         param0.pushPose();
         if (this.getParentModel().young) {
            float var14 = 0.5F;
            param0.translate(0.0F, 0.75F, 0.0F);
            param0.scale(0.5F, 0.5F, 0.5F);
         }

         this.renderArmWithItem(param3, var13, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, param0, param1, param2);
         this.renderArmWithItem(param3, var12, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, param0, param1, param2);
         param0.popPose();
      }
   }

   protected void renderArmWithItem(
      LivingEntity param0, ItemStack param1, ItemDisplayContext param2, HumanoidArm param3, PoseStack param4, MultiBufferSource param5, int param6
   ) {
      if (!param1.isEmpty()) {
         param4.pushPose();
         this.getParentModel().translateToHand(param3, param4);
         param4.mulPose(Axis.XP.rotationDegrees(-90.0F));
         param4.mulPose(Axis.YP.rotationDegrees(180.0F));
         boolean var8 = param3 == HumanoidArm.LEFT;
         param4.translate((var8 ? -1 : 1) / 16.0F, 0.125F, -0.625F);
         this.itemInHandRenderer.renderItem(param0, param1, param2, var8, param4, param5, param6);
         param4.popPose();
      }
   }
}

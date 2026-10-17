package net.minecraft.client.renderer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix4f;

public class ItemInHandRenderer {
   private static final RenderType MAP_BACKGROUND = RenderType.text(new ResourceLocation("textures/map/map_background.png"));
   private static final RenderType MAP_BACKGROUND_CHECKERBOARD = RenderType.text(new ResourceLocation("textures/map/map_background_checkerboard.png"));
   private static final float ITEM_SWING_X_POS_SCALE = -0.4F;
   private static final float ITEM_SWING_Y_POS_SCALE = 0.2F;
   private static final float ITEM_SWING_Z_POS_SCALE = -0.2F;
   private static final float ITEM_HEIGHT_SCALE = -0.6F;
   private static final float ITEM_POS_X = 0.56F;
   private static final float ITEM_POS_Y = -0.52F;
   private static final float ITEM_POS_Z = -0.72F;
   private static final float ITEM_PRESWING_ROT_Y = 45.0F;
   private static final float ITEM_SWING_X_ROT_AMOUNT = -80.0F;
   private static final float ITEM_SWING_Y_ROT_AMOUNT = -20.0F;
   private static final float ITEM_SWING_Z_ROT_AMOUNT = -20.0F;
   private static final float EAT_JIGGLE_X_ROT_AMOUNT = 10.0F;
   private static final float EAT_JIGGLE_Y_ROT_AMOUNT = 90.0F;
   private static final float EAT_JIGGLE_Z_ROT_AMOUNT = 30.0F;
   private static final float EAT_JIGGLE_X_POS_SCALE = 0.6F;
   private static final float EAT_JIGGLE_Y_POS_SCALE = -0.5F;
   private static final float EAT_JIGGLE_Z_POS_SCALE = 0.0F;
   private static final double EAT_JIGGLE_EXPONENT = 27.0;
   private static final float EAT_EXTRA_JIGGLE_CUTOFF = 0.8F;
   private static final float EAT_EXTRA_JIGGLE_SCALE = 0.1F;
   private static final float ARM_SWING_X_POS_SCALE = -0.3F;
   private static final float ARM_SWING_Y_POS_SCALE = 0.4F;
   private static final float ARM_SWING_Z_POS_SCALE = -0.4F;
   private static final float ARM_SWING_Y_ROT_AMOUNT = 70.0F;
   private static final float ARM_SWING_Z_ROT_AMOUNT = -20.0F;
   private static final float ARM_HEIGHT_SCALE = -0.6F;
   private static final float ARM_POS_SCALE = 0.8F;
   private static final float ARM_POS_X = 0.8F;
   private static final float ARM_POS_Y = -0.75F;
   private static final float ARM_POS_Z = -0.9F;
   private static final float ARM_PRESWING_ROT_Y = 45.0F;
   private static final float ARM_PREROTATION_X_OFFSET = -1.0F;
   private static final float ARM_PREROTATION_Y_OFFSET = 3.6F;
   private static final float ARM_PREROTATION_Z_OFFSET = 3.5F;
   private static final float ARM_POSTROTATION_X_OFFSET = 5.6F;
   private static final int ARM_ROT_X = 200;
   private static final int ARM_ROT_Y = -135;
   private static final int ARM_ROT_Z = 120;
   private static final float MAP_SWING_X_POS_SCALE = -0.4F;
   private static final float MAP_SWING_Z_POS_SCALE = -0.2F;
   private static final float MAP_HANDS_POS_X = 0.0F;
   private static final float MAP_HANDS_POS_Y = 0.04F;
   private static final float MAP_HANDS_POS_Z = -0.72F;
   private static final float MAP_HANDS_HEIGHT_SCALE = -1.2F;
   private static final float MAP_HANDS_TILT_SCALE = -0.5F;
   private static final float MAP_PLAYER_PITCH_SCALE = 45.0F;
   private static final float MAP_HANDS_Z_ROT_AMOUNT = -85.0F;
   private static final float MAPHAND_X_ROT_AMOUNT = 45.0F;
   private static final float MAPHAND_Y_ROT_AMOUNT = 92.0F;
   private static final float MAPHAND_Z_ROT_AMOUNT = -41.0F;
   private static final float MAP_HAND_X_POS = 0.3F;
   private static final float MAP_HAND_Y_POS = -1.1F;
   private static final float MAP_HAND_Z_POS = 0.45F;
   private static final float MAP_SWING_X_ROT_AMOUNT = 20.0F;
   private static final float MAP_PRE_ROT_SCALE = 0.38F;
   private static final float MAP_GLOBAL_X_POS = -0.5F;
   private static final float MAP_GLOBAL_Y_POS = -0.5F;
   private static final float MAP_GLOBAL_Z_POS = 0.0F;
   private static final float MAP_FINAL_SCALE = 0.0078125F;
   private static final int MAP_BORDER = 7;
   private static final int MAP_HEIGHT = 128;
   private static final int MAP_WIDTH = 128;
   private static final float BOW_CHARGE_X_POS_SCALE = 0.0F;
   private static final float BOW_CHARGE_Y_POS_SCALE = 0.0F;
   private static final float BOW_CHARGE_Z_POS_SCALE = 0.04F;
   private static final float BOW_CHARGE_SHAKE_X_SCALE = 0.0F;
   private static final float BOW_CHARGE_SHAKE_Y_SCALE = 0.004F;
   private static final float BOW_CHARGE_SHAKE_Z_SCALE = 0.0F;
   private static final float BOW_CHARGE_Z_SCALE = 0.2F;
   private static final float BOW_MIN_SHAKE_CHARGE = 0.1F;
   private final Minecraft minecraft;
   private ItemStack mainHandItem = ItemStack.EMPTY;
   private ItemStack offHandItem = ItemStack.EMPTY;
   private float mainHandHeight;
   private float oMainHandHeight;
   private float offHandHeight;
   private float oOffHandHeight;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final ItemRenderer itemRenderer;

   public ItemInHandRenderer(Minecraft param0, EntityRenderDispatcher param1, ItemRenderer param2) {
      this.minecraft = param0;
      this.entityRenderDispatcher = param1;
      this.itemRenderer = param2;
   }

   public void renderItem(
      LivingEntity param0, ItemStack param1, ItemDisplayContext param2, boolean param3, PoseStack param4, MultiBufferSource param5, int param6
   ) {
      if (!param1.isEmpty()) {
         this.itemRenderer
            .renderStatic(param0, param1, param2, param3, param4, param5, param0.level, param6, OverlayTexture.NO_OVERLAY, param0.getId() + param2.ordinal());
      }
   }

   private float calculateMapTilt(float param0) {
      float var2 = 1.0F - param0 / 45.0F + 0.1F;
      var2 = Mth.clamp(var2, 0.0F, 1.0F);
      return -Mth.cos(var2 * (float) Math.PI) * 0.5F + 0.5F;
   }

   private void renderMapHand(PoseStack param0, MultiBufferSource param1, int param2, HumanoidArm param3) {
      RenderSystem.setShaderTexture(0, this.minecraft.player.getSkinTextureLocation());
      PlayerRenderer var5 = (PlayerRenderer)this.entityRenderDispatcher.<AbstractClientPlayer>getRenderer(this.minecraft.player);
      param0.pushPose();
      float var6 = param3 == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      param0.mulPose(Axis.YP.rotationDegrees(92.0F));
      param0.mulPose(Axis.XP.rotationDegrees(45.0F));
      param0.mulPose(Axis.ZP.rotationDegrees(var6 * -41.0F));
      param0.translate(var6 * 0.3F, -1.1F, 0.45F);
      if (param3 == HumanoidArm.RIGHT) {
         var5.renderRightHand(param0, param1, param2, this.minecraft.player);
      } else {
         var5.renderLeftHand(param0, param1, param2, this.minecraft.player);
      }

      param0.popPose();
   }

   private void renderOneHandedMap(PoseStack param0, MultiBufferSource param1, int param2, float param3, HumanoidArm param4, float param5, ItemStack param6) {
      float var8 = param4 == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      param0.translate(var8 * 0.125F, -0.125F, 0.0F);
      if (!this.minecraft.player.isInvisible()) {
         param0.pushPose();
         param0.mulPose(Axis.ZP.rotationDegrees(var8 * 10.0F));
         this.renderPlayerArm(param0, param1, param2, param3, param5, param4);
         param0.popPose();
      }

      param0.pushPose();
      param0.translate(var8 * 0.51F, -0.08F + param3 * -1.2F, -0.75F);
      float var9 = Mth.sqrt(param5);
      float var10 = Mth.sin(var9 * (float) Math.PI);
      float var11 = -0.5F * var10;
      float var12 = 0.4F * Mth.sin(var9 * (float) (Math.PI * 2));
      float var13 = -0.3F * Mth.sin(param5 * (float) Math.PI);
      param0.translate(var8 * var11, var12 - 0.3F * var10, var13);
      param0.mulPose(Axis.XP.rotationDegrees(var10 * -45.0F));
      param0.mulPose(Axis.YP.rotationDegrees(var8 * var10 * -30.0F));
      this.renderMap(param0, param1, param2, param6);
      param0.popPose();
   }

   private void renderTwoHandedMap(PoseStack param0, MultiBufferSource param1, int param2, float param3, float param4, float param5) {
      float var7 = Mth.sqrt(param5);
      float var8 = -0.2F * Mth.sin(param5 * (float) Math.PI);
      float var9 = -0.4F * Mth.sin(var7 * (float) Math.PI);
      param0.translate(0.0F, -var8 / 2.0F, var9);
      float var10 = this.calculateMapTilt(param3);
      param0.translate(0.0F, 0.04F + param4 * -1.2F + var10 * -0.5F, -0.72F);
      param0.mulPose(Axis.XP.rotationDegrees(var10 * -85.0F));
      if (!this.minecraft.player.isInvisible()) {
         param0.pushPose();
         param0.mulPose(Axis.YP.rotationDegrees(90.0F));
         this.renderMapHand(param0, param1, param2, HumanoidArm.RIGHT);
         this.renderMapHand(param0, param1, param2, HumanoidArm.LEFT);
         param0.popPose();
      }

      float var11 = Mth.sin(var7 * (float) Math.PI);
      param0.mulPose(Axis.XP.rotationDegrees(var11 * 20.0F));
      param0.scale(2.0F, 2.0F, 2.0F);
      this.renderMap(param0, param1, param2, this.mainHandItem);
   }

   private void renderMap(PoseStack param0, MultiBufferSource param1, int param2, ItemStack param3) {
      param0.mulPose(Axis.YP.rotationDegrees(180.0F));
      param0.mulPose(Axis.ZP.rotationDegrees(180.0F));
      param0.scale(0.38F, 0.38F, 0.38F);
      param0.translate(-0.5F, -0.5F, 0.0F);
      param0.scale(0.0078125F, 0.0078125F, 0.0078125F);
      Integer var5 = MapItem.getMapId(param3);
      MapItemSavedData var6 = MapItem.getSavedData(var5, this.minecraft.level);
      VertexConsumer var7 = param1.getBuffer(var6 == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD);
      Matrix4f var8 = param0.last().pose();
      var7.vertex(var8, -7.0F, 135.0F, 0.0F).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(param2).endVertex();
      var7.vertex(var8, 135.0F, 135.0F, 0.0F).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(param2).endVertex();
      var7.vertex(var8, 135.0F, -7.0F, 0.0F).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(param2).endVertex();
      var7.vertex(var8, -7.0F, -7.0F, 0.0F).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(param2).endVertex();
      if (var6 != null) {
         this.minecraft.gameRenderer.getMapRenderer().render(param0, param1, var5, var6, false, param2);
      }
   }

   private void renderPlayerArm(PoseStack param0, MultiBufferSource param1, int param2, float param3, float param4, HumanoidArm param5) {
      boolean var7 = param5 != HumanoidArm.LEFT;
      float var8 = var7 ? 1.0F : -1.0F;
      float var9 = Mth.sqrt(param4);
      float var10 = -0.3F * Mth.sin(var9 * (float) Math.PI);
      float var11 = 0.4F * Mth.sin(var9 * (float) (Math.PI * 2));
      float var12 = -0.4F * Mth.sin(param4 * (float) Math.PI);
      param0.translate(var8 * (var10 + 0.64000005F), var11 + -0.6F + param3 * -0.6F, var12 + -0.71999997F);
      param0.mulPose(Axis.YP.rotationDegrees(var8 * 45.0F));
      float var13 = Mth.sin(param4 * param4 * (float) Math.PI);
      float var14 = Mth.sin(var9 * (float) Math.PI);
      param0.mulPose(Axis.YP.rotationDegrees(var8 * var14 * 70.0F));
      param0.mulPose(Axis.ZP.rotationDegrees(var8 * var13 * -20.0F));
      AbstractClientPlayer var15 = this.minecraft.player;
      RenderSystem.setShaderTexture(0, var15.getSkinTextureLocation());
      param0.translate(var8 * -1.0F, 3.6F, 3.5F);
      param0.mulPose(Axis.ZP.rotationDegrees(var8 * 120.0F));
      param0.mulPose(Axis.XP.rotationDegrees(200.0F));
      param0.mulPose(Axis.YP.rotationDegrees(var8 * -135.0F));
      param0.translate(var8 * 5.6F, 0.0F, 0.0F);
      PlayerRenderer var16 = (PlayerRenderer)this.entityRenderDispatcher.<AbstractClientPlayer>getRenderer(var15);
      if (var7) {
         var16.renderRightHand(param0, param1, param2, var15);
      } else {
         var16.renderLeftHand(param0, param1, param2, var15);
      }
   }

   private void applyEatTransform(PoseStack param0, float param1, HumanoidArm param2, ItemStack param3) {
      float var5 = this.minecraft.player.getUseItemRemainingTicks() - param1 + 1.0F;
      float var6 = var5 / param3.getUseDuration();
      if (var6 < 0.8F) {
         float var7 = Mth.abs(Mth.cos(var5 / 4.0F * (float) Math.PI) * 0.1F);
         param0.translate(0.0F, var7, 0.0F);
      }

      float var7 = 1.0F - (float)Math.pow(var6, 27.0);
      int var8 = param2 == HumanoidArm.RIGHT ? 1 : -1;
      param0.translate(var7 * 0.6F * var8, var7 * -0.5F, var7 * 0.0F);
      param0.mulPose(Axis.YP.rotationDegrees(var8 * var7 * 90.0F));
      param0.mulPose(Axis.XP.rotationDegrees(var7 * 10.0F));
      param0.mulPose(Axis.ZP.rotationDegrees(var8 * var7 * 30.0F));
   }

   private void applyBrushTransform(PoseStack param0, float param1, HumanoidArm param2, ItemStack param3, float param4) {
      this.applyItemArmTransform(param0, param2, param4);
      float var6 = this.minecraft.player.getUseItemRemainingTicks() - param1 + 1.0F;
      float var7 = 1.0F - var6 / param3.getUseDuration();
      float var8 = -90.0F;
      float var9 = 60.0F;
      int var10 = 45;
      float var11 = 150.0F;
      float var12 = -15.0F;
      float var13 = -15.0F + 75.0F * Mth.cos(var7 * 45.0F * (float) Math.PI);
      if (param2 != HumanoidArm.RIGHT) {
         param0.translate(0.1, 0.83, 0.35);
         param0.mulPose(Axis.XP.rotationDegrees(-80.0F));
         param0.mulPose(Axis.YP.rotationDegrees(-90.0F));
         param0.mulPose(Axis.XP.rotationDegrees(var13));
         param0.translate(-0.3, 0.22, 0.35);
      } else {
         param0.translate(-0.25, 0.22, 0.35);
         param0.mulPose(Axis.XP.rotationDegrees(-80.0F));
         param0.mulPose(Axis.YP.rotationDegrees(90.0F));
         param0.mulPose(Axis.ZP.rotationDegrees(0.0F));
         param0.mulPose(Axis.XP.rotationDegrees(var13));
      }
   }

   private void applyItemArmAttackTransform(PoseStack param0, HumanoidArm param1, float param2) {
      int var4 = param1 == HumanoidArm.RIGHT ? 1 : -1;
      float var5 = Mth.sin(param2 * param2 * (float) Math.PI);
      param0.mulPose(Axis.YP.rotationDegrees(var4 * (45.0F + var5 * -20.0F)));
      float var6 = Mth.sin(Mth.sqrt(param2) * (float) Math.PI);
      param0.mulPose(Axis.ZP.rotationDegrees(var4 * var6 * -20.0F));
      param0.mulPose(Axis.XP.rotationDegrees(var6 * -80.0F));
      param0.mulPose(Axis.YP.rotationDegrees(var4 * -45.0F));
   }

   private void applyItemArmTransform(PoseStack param0, HumanoidArm param1, float param2) {
      int var4 = param1 == HumanoidArm.RIGHT ? 1 : -1;
      param0.translate(var4 * 0.56F, -0.52F + param2 * -0.6F, -0.72F);
   }

   public void renderHandsWithItems(float param0, PoseStack param1, MultiBufferSource.BufferSource param2, LocalPlayer param3, int param4) {
      float var6 = param3.getAttackAnim(param0);
      InteractionHand var7 = MoreObjects.firstNonNull(param3.swingingArm, InteractionHand.MAIN_HAND);
      float var8 = Mth.lerp(param0, param3.xRotO, param3.getXRot());
      ItemInHandRenderer.HandRenderSelection var9 = evaluateWhichHandsToRender(param3);
      float var10 = Mth.lerp(param0, param3.xBobO, param3.xBob);
      float var11 = Mth.lerp(param0, param3.yBobO, param3.yBob);
      param1.mulPose(Axis.XP.rotationDegrees((param3.getViewXRot(param0) - var10) * 0.1F));
      param1.mulPose(Axis.YP.rotationDegrees((param3.getViewYRot(param0) - var11) * 0.1F));
      if (var9.renderMainHand) {
         float var12 = var7 == InteractionHand.MAIN_HAND ? var6 : 0.0F;
         float var13 = 1.0F - Mth.lerp(param0, this.oMainHandHeight, this.mainHandHeight);
         this.renderArmWithItem(param3, param0, var8, InteractionHand.MAIN_HAND, var12, this.mainHandItem, var13, param1, param2, param4);
      }

      if (var9.renderOffHand) {
         float var12 = var7 == InteractionHand.OFF_HAND ? var6 : 0.0F;
         float var13 = 1.0F - Mth.lerp(param0, this.oOffHandHeight, this.offHandHeight);
         this.renderArmWithItem(param3, param0, var8, InteractionHand.OFF_HAND, var12, this.offHandItem, var13, param1, param2, param4);
      }

      param2.endBatch();
   }

   @VisibleForTesting
   static ItemInHandRenderer.HandRenderSelection evaluateWhichHandsToRender(LocalPlayer param0) {
      ItemStack var1 = param0.getMainHandItem();
      ItemStack var2 = param0.getOffhandItem();
      boolean var3 = var1.is(Items.BOW) || var2.is(Items.BOW);
      boolean var4 = var1.is(Items.CROSSBOW) || var2.is(Items.CROSSBOW);
      if (!var3 && !var4) {
         return ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
      } else if (param0.isUsingItem()) {
         return selectionUsingItemWhileHoldingBowLike(param0);
      } else {
         return isChargedCrossbow(var1)
            ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY
            : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
      }
   }

   private static ItemInHandRenderer.HandRenderSelection selectionUsingItemWhileHoldingBowLike(LocalPlayer param0) {
      ItemStack var1 = param0.getUseItem();
      InteractionHand var2 = param0.getUsedItemHand();
      if (!var1.is(Items.BOW) && !var1.is(Items.CROSSBOW)) {
         return var2 == InteractionHand.MAIN_HAND && isChargedCrossbow(param0.getOffhandItem())
            ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY
            : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
      } else {
         return ItemInHandRenderer.HandRenderSelection.onlyForHand(var2);
      }
   }

   private static boolean isChargedCrossbow(ItemStack param0) {
      return param0.is(Items.CROSSBOW) && CrossbowItem.isCharged(param0);
   }

   private void renderArmWithItem(
      AbstractClientPlayer param0,
      float param1,
      float param2,
      InteractionHand param3,
      float param4,
      ItemStack param5,
      float param6,
      PoseStack param7,
      MultiBufferSource param8,
      int param9
   ) {
      if (!param0.isScoping()) {
         boolean var11 = param3 == InteractionHand.MAIN_HAND;
         HumanoidArm var12 = var11 ? param0.getMainArm() : param0.getMainArm().getOpposite();
         param7.pushPose();
         if (param5.isEmpty()) {
            if (var11 && !param0.isInvisible()) {
               this.renderPlayerArm(param7, param8, param9, param6, param4, var12);
            }
         } else if (param5.is(Items.FILLED_MAP)) {
            if (var11 && this.offHandItem.isEmpty()) {
               this.renderTwoHandedMap(param7, param8, param9, param2, param6, param4);
            } else {
               this.renderOneHandedMap(param7, param8, param9, param6, var12, param4, param5);
            }
         } else if (param5.is(Items.CROSSBOW)) {
            boolean var13 = CrossbowItem.isCharged(param5);
            boolean var14 = var12 == HumanoidArm.RIGHT;
            int var15 = var14 ? 1 : -1;
            if (param0.isUsingItem() && param0.getUseItemRemainingTicks() > 0 && param0.getUsedItemHand() == param3) {
               this.applyItemArmTransform(param7, var12, param6);
               param7.translate(var15 * -0.4785682F, -0.094387F, 0.05731531F);
               param7.mulPose(Axis.XP.rotationDegrees(-11.935F));
               param7.mulPose(Axis.YP.rotationDegrees(var15 * 65.3F));
               param7.mulPose(Axis.ZP.rotationDegrees(var15 * -9.785F));
               float var16 = param5.getUseDuration() - (this.minecraft.player.getUseItemRemainingTicks() - param1 + 1.0F);
               float var17 = var16 / CrossbowItem.getChargeDuration(param5);
               if (var17 > 1.0F) {
                  var17 = 1.0F;
               }

               if (var17 > 0.1F) {
                  float var18 = Mth.sin((var16 - 0.1F) * 1.3F);
                  float var19 = var17 - 0.1F;
                  float var20 = var18 * var19;
                  param7.translate(var20 * 0.0F, var20 * 0.004F, var20 * 0.0F);
               }

               param7.translate(var17 * 0.0F, var17 * 0.0F, var17 * 0.04F);
               param7.scale(1.0F, 1.0F, 1.0F + var17 * 0.2F);
               param7.mulPose(Axis.YN.rotationDegrees(var15 * 45.0F));
            } else {
               float var16 = -0.4F * Mth.sin(Mth.sqrt(param4) * (float) Math.PI);
               float var17 = 0.2F * Mth.sin(Mth.sqrt(param4) * (float) (Math.PI * 2));
               float var18 = -0.2F * Mth.sin(param4 * (float) Math.PI);
               param7.translate(var15 * var16, var17, var18);
               this.applyItemArmTransform(param7, var12, param6);
               this.applyItemArmAttackTransform(param7, var12, param4);
               if (var13 && param4 < 0.001F && var11) {
                  param7.translate(var15 * -0.641864F, 0.0F, 0.0F);
                  param7.mulPose(Axis.YP.rotationDegrees(var15 * 10.0F));
               }
            }

            this.renderItem(
               param0, param5, var14 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !var14, param7, param8, param9
            );
         } else {
            boolean var13 = var12 == HumanoidArm.RIGHT;
            if (param0.isUsingItem() && param0.getUseItemRemainingTicks() > 0 && param0.getUsedItemHand() == param3) {
               int var14 = var13 ? 1 : -1;
               switch (param5.getUseAnimation()) {
                  case NONE:
                     this.applyItemArmTransform(param7, var12, param6);
                     break;
                  case EAT:
                  case DRINK:
                     this.applyEatTransform(param7, param1, var12, param5);
                     this.applyItemArmTransform(param7, var12, param6);
                     break;
                  case BLOCK:
                     this.applyItemArmTransform(param7, var12, param6);
                     break;
                  case BOW:
                     this.applyItemArmTransform(param7, var12, param6);
                     param7.translate(var14 * -0.2785682F, 0.18344387F, 0.15731531F);
                     param7.mulPose(Axis.XP.rotationDegrees(-13.935F));
                     param7.mulPose(Axis.YP.rotationDegrees(var14 * 35.3F));
                     param7.mulPose(Axis.ZP.rotationDegrees(var14 * -9.785F));
                     float var15 = param5.getUseDuration() - (this.minecraft.player.getUseItemRemainingTicks() - param1 + 1.0F);
                     float var16 = var15 / 20.0F;
                     var16 = (var16 * var16 + var16 * 2.0F) / 3.0F;
                     if (var16 > 1.0F) {
                        var16 = 1.0F;
                     }

                     if (var16 > 0.1F) {
                        float var17 = Mth.sin((var15 - 0.1F) * 1.3F);
                        float var18 = var16 - 0.1F;
                        float var19 = var17 * var18;
                        param7.translate(var19 * 0.0F, var19 * 0.004F, var19 * 0.0F);
                     }

                     param7.translate(var16 * 0.0F, var16 * 0.0F, var16 * 0.04F);
                     param7.scale(1.0F, 1.0F, 1.0F + var16 * 0.2F);
                     param7.mulPose(Axis.YN.rotationDegrees(var14 * 45.0F));
                     break;
                  case SPEAR:
                     this.applyItemArmTransform(param7, var12, param6);
                     param7.translate(var14 * -0.5F, 0.7F, 0.1F);
                     param7.mulPose(Axis.XP.rotationDegrees(-55.0F));
                     param7.mulPose(Axis.YP.rotationDegrees(var14 * 35.3F));
                     param7.mulPose(Axis.ZP.rotationDegrees(var14 * -9.785F));
                     float var15_1 = param5.getUseDuration() - (this.minecraft.player.getUseItemRemainingTicks() - param1 + 1.0F);
                     float var16_1 = var15_1 / 10.0F;
                     if (var16_1 > 1.0F) {
                        var16_1 = 1.0F;
                     }

                     if (var16_1 > 0.1F) {
                        float var17 = Mth.sin((var15_1 - 0.1F) * 1.3F);
                        float var18 = var16_1 - 0.1F;
                        float var19 = var17 * var18;
                        param7.translate(var19 * 0.0F, var19 * 0.004F, var19 * 0.0F);
                     }

                     param7.translate(0.0F, 0.0F, var16_1 * 0.2F);
                     param7.scale(1.0F, 1.0F, 1.0F + var16_1 * 0.2F);
                     param7.mulPose(Axis.YN.rotationDegrees(var14 * 45.0F));
                     break;
                  case BRUSH:
                     this.applyBrushTransform(param7, param1, var12, param5, param6);
               }
            } else if (param0.isAutoSpinAttack()) {
               this.applyItemArmTransform(param7, var12, param6);
               int var14 = var13 ? 1 : -1;
               param7.translate(var14 * -0.4F, 0.8F, 0.3F);
               param7.mulPose(Axis.YP.rotationDegrees(var14 * 65.0F));
               param7.mulPose(Axis.ZP.rotationDegrees(var14 * -85.0F));
            } else {
               float var14 = -0.4F * Mth.sin(Mth.sqrt(param4) * (float) Math.PI);
               float var15 = 0.2F * Mth.sin(Mth.sqrt(param4) * (float) (Math.PI * 2));
               float var16 = -0.2F * Mth.sin(param4 * (float) Math.PI);
               int var17 = var13 ? 1 : -1;
               param7.translate(var17 * var14, var15, var16);
               this.applyItemArmTransform(param7, var12, param6);
               this.applyItemArmAttackTransform(param7, var12, param4);
            }

            this.renderItem(
               param0, param5, var13 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !var13, param7, param8, param9
            );
         }

         param7.popPose();
      }
   }

   public void tick() {
      this.oMainHandHeight = this.mainHandHeight;
      this.oOffHandHeight = this.offHandHeight;
      LocalPlayer var1 = this.minecraft.player;
      ItemStack var2 = var1.getMainHandItem();
      ItemStack var3 = var1.getOffhandItem();
      if (ItemStack.matches(this.mainHandItem, var2)) {
         this.mainHandItem = var2;
      }

      if (ItemStack.matches(this.offHandItem, var3)) {
         this.offHandItem = var3;
      }

      if (var1.isHandsBusy()) {
         this.mainHandHeight = Mth.clamp(this.mainHandHeight - 0.4F, 0.0F, 1.0F);
         this.offHandHeight = Mth.clamp(this.offHandHeight - 0.4F, 0.0F, 1.0F);
      } else {
         float var4 = var1.getAttackStrengthScale(1.0F);
         this.mainHandHeight = this.mainHandHeight + Mth.clamp((this.mainHandItem == var2 ? var4 * var4 * var4 : 0.0F) - this.mainHandHeight, -0.4F, 0.4F);
         this.offHandHeight = this.offHandHeight + Mth.clamp((this.offHandItem == var3 ? 1 : 0) - this.offHandHeight, -0.4F, 0.4F);
      }

      if (this.mainHandHeight < 0.1F) {
         this.mainHandItem = var2;
      }

      if (this.offHandHeight < 0.1F) {
         this.offHandItem = var3;
      }
   }

   public void itemUsed(InteractionHand param0) {
      if (param0 == InteractionHand.MAIN_HAND) {
         this.mainHandHeight = 0.0F;
      } else {
         this.offHandHeight = 0.0F;
      }
   }

   @VisibleForTesting
   enum HandRenderSelection {
      RENDER_BOTH_HANDS(true, true),
      RENDER_MAIN_HAND_ONLY(true, false),
      RENDER_OFF_HAND_ONLY(false, true);

      final boolean renderMainHand;
      final boolean renderOffHand;

      HandRenderSelection(boolean param2, boolean param3) {
         this.renderMainHand = param2;
         this.renderOffHand = param3;
      }

      public static ItemInHandRenderer.HandRenderSelection onlyForHand(InteractionHand param0) {
         return param0 == InteractionHand.MAIN_HAND ? RENDER_MAIN_HAND_ONLY : RENDER_OFF_HAND_ONLY;
      }
   }
}

package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

public class ItemFrameRenderer<T extends ItemFrame> extends EntityRenderer<T> {
   private static final ModelResourceLocation FRAME_LOCATION = ModelResourceLocation.vanilla("item_frame", "map=false");
   private static final ModelResourceLocation MAP_FRAME_LOCATION = ModelResourceLocation.vanilla("item_frame", "map=true");
   private static final ModelResourceLocation GLOW_FRAME_LOCATION = ModelResourceLocation.vanilla("glow_item_frame", "map=false");
   private static final ModelResourceLocation GLOW_MAP_FRAME_LOCATION = ModelResourceLocation.vanilla("glow_item_frame", "map=true");
   public static final int GLOW_FRAME_BRIGHTNESS = 5;
   public static final int BRIGHT_MAP_LIGHT_ADJUSTMENT = 30;
   private final ItemRenderer itemRenderer;
   private final BlockRenderDispatcher blockRenderer;

   public ItemFrameRenderer(EntityRendererProvider.Context param0) {
      super(param0);
      this.itemRenderer = param0.getItemRenderer();
      this.blockRenderer = param0.getBlockRenderDispatcher();
   }

   protected int getBlockLightLevel(T param0, BlockPos param1) {
      return param0.getType() == EntityType.GLOW_ITEM_FRAME ? Math.max(5, super.getBlockLightLevel(param0, param1)) : super.getBlockLightLevel(param0, param1);
   }

   public void render(T param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      super.render(param0, param1, param2, param3, param4, param5);
      param3.pushPose();
      Direction var7 = param0.getDirection();
      Vec3 var8 = this.getRenderOffset(param0, param2);
      param3.translate(-var8.x(), -var8.y(), -var8.z());
      double var9 = 0.46875;
      param3.translate(var7.getStepX() * 0.46875, var7.getStepY() * 0.46875, var7.getStepZ() * 0.46875);
      param3.mulPose(Axis.XP.rotationDegrees(param0.getXRot()));
      param3.mulPose(Axis.YP.rotationDegrees(180.0F - param0.getYRot()));
      boolean var11 = param0.isInvisible();
      ItemStack var12 = param0.getItem();
      if (!var11) {
         ModelManager var13 = this.blockRenderer.getBlockModelShaper().getModelManager();
         ModelResourceLocation var14 = this.getFrameModelResourceLoc(param0, var12);
         param3.pushPose();
         param3.translate(-0.5F, -0.5F, -0.5F);
         this.blockRenderer
            .getModelRenderer()
            .renderModel(
               param3.last(), param4.getBuffer(Sheets.solidBlockSheet()), null, var13.getModel(var14), 1.0F, 1.0F, 1.0F, param5, OverlayTexture.NO_OVERLAY
            );
         param3.popPose();
      }

      if (!var12.isEmpty()) {
         OptionalInt var13 = param0.getFramedMapId();
         if (var11) {
            param3.translate(0.0F, 0.0F, 0.5F);
         } else {
            param3.translate(0.0F, 0.0F, 0.4375F);
         }

         int var14 = var13.isPresent() ? param0.getRotation() % 4 * 2 : param0.getRotation();
         param3.mulPose(Axis.ZP.rotationDegrees(var14 * 360.0F / 8.0F));
         if (var13.isPresent()) {
            param3.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float var15 = 0.0078125F;
            param3.scale(0.0078125F, 0.0078125F, 0.0078125F);
            param3.translate(-64.0F, -64.0F, 0.0F);
            MapItemSavedData var16 = MapItem.getSavedData(var13.getAsInt(), param0.level);
            param3.translate(0.0F, 0.0F, -1.0F);
            if (var16 != null) {
               int var17 = this.getLightVal(param0, 15728850, param5);
               Minecraft.getInstance().gameRenderer.getMapRenderer().render(param3, param4, var13.getAsInt(), var16, true, var17);
            }
         } else {
            int var15 = this.getLightVal(param0, 15728880, param5);
            param3.scale(0.5F, 0.5F, 0.5F);
            this.itemRenderer.renderStatic(var12, ItemDisplayContext.FIXED, var15, OverlayTexture.NO_OVERLAY, param3, param4, param0.level, param0.getId());
         }
      }

      param3.popPose();
   }

   private int getLightVal(T param0, int param1, int param2) {
      return param0.getType() == EntityType.GLOW_ITEM_FRAME ? param1 : param2;
   }

   private ModelResourceLocation getFrameModelResourceLoc(T param0, ItemStack param1) {
      boolean var3 = param0.getType() == EntityType.GLOW_ITEM_FRAME;
      if (param1.is(Items.FILLED_MAP)) {
         return var3 ? GLOW_MAP_FRAME_LOCATION : MAP_FRAME_LOCATION;
      } else {
         return var3 ? GLOW_FRAME_LOCATION : FRAME_LOCATION;
      }
   }

   public Vec3 getRenderOffset(T param0, float param1) {
      return new Vec3(param0.getDirection().getStepX() * 0.3F, -0.25, param0.getDirection().getStepZ() * 0.3F);
   }

   public ResourceLocation getTextureLocation(T param0) {
      return TextureAtlas.LOCATION_BLOCKS;
   }

   protected boolean shouldShowName(T param0) {
      if (Minecraft.renderNames()
         && !param0.getItem().isEmpty()
         && param0.getItem().hasCustomHoverName()
         && this.entityRenderDispatcher.crosshairPickEntity == param0) {
         double var2 = this.entityRenderDispatcher.distanceToSqr(param0);
         float var4 = param0.isDiscrete() ? 32.0F : 64.0F;
         return var2 < var4 * var4;
      } else {
         return false;
      }
   }

   protected void renderNameTag(T param0, Component param1, PoseStack param2, MultiBufferSource param3, int param4) {
      super.renderNameTag(param0, param0.getItem().getHoverName(), param2, param3, param4);
   }
}

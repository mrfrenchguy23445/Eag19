package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ItemEntityRenderer extends EntityRenderer<ItemEntity> {
   private static final float ITEM_BUNDLE_OFFSET_SCALE = 0.15F;
   private static final int ITEM_COUNT_FOR_5_BUNDLE = 48;
   private static final int ITEM_COUNT_FOR_4_BUNDLE = 32;
   private static final int ITEM_COUNT_FOR_3_BUNDLE = 16;
   private static final int ITEM_COUNT_FOR_2_BUNDLE = 1;
   private static final float FLAT_ITEM_BUNDLE_OFFSET_X = 0.0F;
   private static final float FLAT_ITEM_BUNDLE_OFFSET_Y = 0.0F;
   private static final float FLAT_ITEM_BUNDLE_OFFSET_Z = 0.09375F;
   private final ItemRenderer itemRenderer;
   private final RandomSource random = RandomSource.create();

   public ItemEntityRenderer(EntityRendererProvider.Context param0) {
      super(param0);
      this.itemRenderer = param0.getItemRenderer();
      this.shadowRadius = 0.15F;
      this.shadowStrength = 0.75F;
   }

   private int getRenderAmount(ItemStack param0) {
      int var2 = 1;
      if (param0.getCount() > 48) {
         var2 = 5;
      } else if (param0.getCount() > 32) {
         var2 = 4;
      } else if (param0.getCount() > 16) {
         var2 = 3;
      } else if (param0.getCount() > 1) {
         var2 = 2;
      }

      return var2;
   }

   public void render(ItemEntity param0, float param1, float param2, PoseStack param3, MultiBufferSource param4, int param5) {
      param3.pushPose();
      ItemStack var7 = param0.getItem();
      int var8 = var7.isEmpty() ? 187 : Item.getId(var7.getItem()) + var7.getDamageValue();
      this.random.setSeed(var8);
      BakedModel var9 = this.itemRenderer.getModel(var7, param0.level, null, param0.getId());
      boolean var10 = var9.isGui3d();
      int var11 = this.getRenderAmount(var7);
      float var12 = 0.25F;
      float var13 = Mth.sin((param0.getAge() + param2) / 10.0F + param0.bobOffs) * 0.1F + 0.1F;
      float var14 = var9.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
      param3.translate(0.0F, var13 + 0.25F * var14, 0.0F);
      float var15 = param0.getSpin(param2);
      param3.mulPose(Axis.YP.rotation(var15));
      float var16 = var9.getTransforms().ground.scale.x();
      float var17 = var9.getTransforms().ground.scale.y();
      float var18 = var9.getTransforms().ground.scale.z();
      if (!var10) {
         float var19 = -0.0F * (var11 - 1) * 0.5F * var16;
         float var20 = -0.0F * (var11 - 1) * 0.5F * var17;
         float var21 = -0.09375F * (var11 - 1) * 0.5F * var18;
         param3.translate(var19, var20, var21);
      }

      for (int var19 = 0; var19 < var11; var19++) {
         param3.pushPose();
         if (var19 > 0) {
            if (var10) {
               float var20 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float var21 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float var22 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               param3.translate(var20, var21, var22);
            } else {
               float var20 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               float var21 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               param3.translate(var20, var21, 0.0F);
            }
         }

         this.itemRenderer.render(var7, ItemDisplayContext.GROUND, false, param3, param4, param5, OverlayTexture.NO_OVERLAY, var9);
         param3.popPose();
         if (!var10) {
            param3.translate(0.0F * var16, 0.0F * var17, 0.09375F * var18);
         }
      }

      param3.popPose();
      super.render(param0, param1, param2, param3, param4, param5);
   }

   public ResourceLocation getTextureLocation(ItemEntity param0) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}

package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class CartographyTableScreen extends AbstractContainerScreen<CartographyTableMenu> {
   private static final ResourceLocation BG_LOCATION = new ResourceLocation("textures/gui/container/cartography_table.png");

   public CartographyTableScreen(CartographyTableMenu param0, Inventory param1, Component param2) {
      super(param0, param1, param2);
      this.titleLabelY -= 2;
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      super.render(param0, param1, param2, param3);
      this.renderTooltip(param0, param1, param2);
   }

   @Override
   protected void renderBg(PoseStack param0, float param1, int param2, int param3) {
      this.renderBackground(param0);
      RenderSystem.setShaderTexture(0, BG_LOCATION);
      int var5 = this.leftPos;
      int var6 = this.topPos;
      blit(param0, var5, var6, 0, 0, this.imageWidth, this.imageHeight);
      ItemStack var7 = this.menu.getSlot(1).getItem();
      boolean var8 = var7.is(Items.MAP);
      boolean var9 = var7.is(Items.PAPER);
      boolean var10 = var7.is(Items.GLASS_PANE);
      ItemStack var11 = this.menu.getSlot(0).getItem();
      boolean var14 = false;
      Integer var12;
      MapItemSavedData var13;
      if (var11.is(Items.FILLED_MAP)) {
         var12 = MapItem.getMapId(var11);
         var13 = MapItem.getSavedData(var12, this.minecraft.level);
         if (var13 != null) {
            if (var13.locked) {
               var14 = true;
               if (var9 || var10) {
                  blit(param0, var5 + 35, var6 + 31, this.imageWidth + 50, 132, 28, 21);
               }
            }

            if (var9 && var13.scale >= 4) {
               var14 = true;
               blit(param0, var5 + 35, var6 + 31, this.imageWidth + 50, 132, 28, 21);
            }
         }
      } else {
         var12 = null;
         var13 = null;
      }

      this.renderResultingMap(param0, var12, var13, var8, var9, var10, var14);
   }

   private void renderResultingMap(
      PoseStack param0, @Nullable Integer param1, @Nullable MapItemSavedData param2, boolean param3, boolean param4, boolean param5, boolean param6
   ) {
      int var8 = this.leftPos;
      int var9 = this.topPos;
      if (param4 && !param6) {
         blit(param0, var8 + 67, var9 + 13, this.imageWidth, 66, 66, 66);
         this.renderMap(param0, param1, param2, var8 + 85, var9 + 31, 0.226F);
      } else if (param3) {
         blit(param0, var8 + 67 + 16, var9 + 13, this.imageWidth, 132, 50, 66);
         this.renderMap(param0, param1, param2, var8 + 86, var9 + 16, 0.34F);
         RenderSystem.setShaderTexture(0, BG_LOCATION);
         param0.pushPose();
         param0.translate(0.0F, 0.0F, 1.0F);
         blit(param0, var8 + 67, var9 + 13 + 16, this.imageWidth, 132, 50, 66);
         this.renderMap(param0, param1, param2, var8 + 70, var9 + 32, 0.34F);
         param0.popPose();
      } else if (param5) {
         blit(param0, var8 + 67, var9 + 13, this.imageWidth, 0, 66, 66);
         this.renderMap(param0, param1, param2, var8 + 71, var9 + 17, 0.45F);
         RenderSystem.setShaderTexture(0, BG_LOCATION);
         param0.pushPose();
         param0.translate(0.0F, 0.0F, 1.0F);
         blit(param0, var8 + 66, var9 + 12, 0, this.imageHeight, 66, 66);
         param0.popPose();
      } else {
         blit(param0, var8 + 67, var9 + 13, this.imageWidth, 0, 66, 66);
         this.renderMap(param0, param1, param2, var8 + 71, var9 + 17, 0.45F);
      }
   }

   private void renderMap(PoseStack param0, @Nullable Integer param1, @Nullable MapItemSavedData param2, int param3, int param4, float param5) {
      if (param1 != null && param2 != null) {
         param0.pushPose();
         param0.translate(param3, param4, 1.0F);
         param0.scale(param5, param5, 1.0F);
         MultiBufferSource.BufferSource var7 = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
         this.minecraft.gameRenderer.getMapRenderer().render(param0, var7, param1, param2, true, 15728880);
         var7.endBatch();
         param0.popPose();
      }
   }
}

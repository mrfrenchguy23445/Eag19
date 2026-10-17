package net.minecraft.client.gui.screens.inventory.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.item.ItemStack;

public class ClientBundleTooltip implements ClientTooltipComponent {
   public static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("textures/gui/container/bundle.png");
   private static final int MARGIN_Y = 4;
   private static final int BORDER_WIDTH = 1;
   private static final int TEX_SIZE = 128;
   private static final int SLOT_SIZE_X = 18;
   private static final int SLOT_SIZE_Y = 20;
   private final NonNullList<ItemStack> items;
   private final int weight;

   public ClientBundleTooltip(BundleTooltip param0) {
      this.items = param0.getItems();
      this.weight = param0.getWeight();
   }

   @Override
   public int getHeight() {
      return this.gridSizeY() * 20 + 2 + 4;
   }

   @Override
   public int getWidth(Font param0) {
      return this.gridSizeX() * 18 + 2;
   }

   @Override
   public void renderImage(Font param0, int param1, int param2, PoseStack param3, ItemRenderer param4) {
      int var6 = this.gridSizeX();
      int var7 = this.gridSizeY();
      boolean var8 = this.weight >= 64;
      int var9 = 0;

      for (int var10 = 0; var10 < var7; var10++) {
         for (int var11 = 0; var11 < var6; var11++) {
            int var12 = param1 + var11 * 18 + 1;
            int var13 = param2 + var10 * 20 + 1;
            this.renderSlot(var12, var13, var9++, var8, param0, param3, param4);
         }
      }

      this.drawBorder(param1, param2, var6, var7, param3);
   }

   private void renderSlot(int param0, int param1, int param2, boolean param3, Font param4, PoseStack param5, ItemRenderer param6) {
      if (param2 >= this.items.size()) {
         this.blit(param5, param0, param1, param3 ? ClientBundleTooltip.Texture.BLOCKED_SLOT : ClientBundleTooltip.Texture.SLOT);
      } else {
         ItemStack var8 = this.items.get(param2);
         this.blit(param5, param0, param1, ClientBundleTooltip.Texture.SLOT);
         param6.renderAndDecorateItem(param5, var8, param0 + 1, param1 + 1, param2);
         param6.renderGuiItemDecorations(param5, param4, var8, param0 + 1, param1 + 1);
         if (param2 == 0) {
            AbstractContainerScreen.renderSlotHighlight(param5, param0 + 1, param1 + 1, 0);
         }
      }
   }

   private void drawBorder(int param0, int param1, int param2, int param3, PoseStack param4) {
      this.blit(param4, param0, param1, ClientBundleTooltip.Texture.BORDER_CORNER_TOP);
      this.blit(param4, param0 + param2 * 18 + 1, param1, ClientBundleTooltip.Texture.BORDER_CORNER_TOP);

      for (int var6 = 0; var6 < param2; var6++) {
         this.blit(param4, param0 + 1 + var6 * 18, param1, ClientBundleTooltip.Texture.BORDER_HORIZONTAL_TOP);
         this.blit(param4, param0 + 1 + var6 * 18, param1 + param3 * 20, ClientBundleTooltip.Texture.BORDER_HORIZONTAL_BOTTOM);
      }

      for (int var6 = 0; var6 < param3; var6++) {
         this.blit(param4, param0, param1 + var6 * 20 + 1, ClientBundleTooltip.Texture.BORDER_VERTICAL);
         this.blit(param4, param0 + param2 * 18 + 1, param1 + var6 * 20 + 1, ClientBundleTooltip.Texture.BORDER_VERTICAL);
      }

      this.blit(param4, param0, param1 + param3 * 20, ClientBundleTooltip.Texture.BORDER_CORNER_BOTTOM);
      this.blit(param4, param0 + param2 * 18 + 1, param1 + param3 * 20, ClientBundleTooltip.Texture.BORDER_CORNER_BOTTOM);
   }

   private void blit(PoseStack param0, int param1, int param2, ClientBundleTooltip.Texture param3) {
      RenderSystem.setShaderTexture(0, TEXTURE_LOCATION);
      GuiComponent.blit(param0, param1, param2, 0, param3.x, param3.y, param3.w, param3.h, 128, 128);
   }

   private int gridSizeX() {
      return Math.max(2, (int)Math.ceil(Math.sqrt(this.items.size() + 1.0)));
   }

   private int gridSizeY() {
      return (int)Math.ceil((this.items.size() + 1.0) / this.gridSizeX());
   }

   enum Texture {
      SLOT(0, 0, 18, 20),
      BLOCKED_SLOT(0, 40, 18, 20),
      BORDER_VERTICAL(0, 18, 1, 20),
      BORDER_HORIZONTAL_TOP(0, 20, 18, 1),
      BORDER_HORIZONTAL_BOTTOM(0, 60, 18, 1),
      BORDER_CORNER_TOP(0, 20, 1, 1),
      BORDER_CORNER_BOTTOM(0, 60, 1, 1);

      public final int x;
      public final int y;
      public final int w;
      public final int h;

      Texture(int param2, int param3, int param4, int param5) {
         this.x = param2;
         this.y = param3;
         this.w = param4;
         this.h = param5;
      }
   }
}

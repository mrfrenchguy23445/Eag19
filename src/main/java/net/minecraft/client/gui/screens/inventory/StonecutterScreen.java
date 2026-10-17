package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.StonecutterRecipe;

public class StonecutterScreen extends AbstractContainerScreen<StonecutterMenu> {
   private static final ResourceLocation BG_LOCATION = new ResourceLocation("textures/gui/container/stonecutter.png");
   private static final int SCROLLER_WIDTH = 12;
   private static final int SCROLLER_HEIGHT = 15;
   private static final int RECIPES_COLUMNS = 4;
   private static final int RECIPES_ROWS = 3;
   private static final int RECIPES_IMAGE_SIZE_WIDTH = 16;
   private static final int RECIPES_IMAGE_SIZE_HEIGHT = 18;
   private static final int SCROLLER_FULL_HEIGHT = 54;
   private static final int RECIPES_X = 52;
   private static final int RECIPES_Y = 14;
   private float scrollOffs;
   private boolean scrolling;
   private int startIndex;
   private boolean displayRecipes;

   public StonecutterScreen(StonecutterMenu param0, Inventory param1, Component param2) {
      super(param0, param1, param2);
      param0.registerUpdateListener(this::containerChanged);
      this.titleLabelY--;
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
      int var7 = (int)(41.0F * this.scrollOffs);
      blit(param0, var5 + 119, var6 + 15 + var7, 176 + (this.isScrollBarActive() ? 0 : 12), 0, 12, 15);
      int var8 = this.leftPos + 52;
      int var9 = this.topPos + 14;
      int var10 = this.startIndex + 12;
      this.renderButtons(param0, param2, param3, var8, var9, var10);
      this.renderRecipes(param0, var8, var9, var10);
   }

   @Override
   protected void renderTooltip(PoseStack param0, int param1, int param2) {
      super.renderTooltip(param0, param1, param2);
      if (this.displayRecipes) {
         int var4 = this.leftPos + 52;
         int var5 = this.topPos + 14;
         int var6 = this.startIndex + 12;
         List<StonecutterRecipe> var7 = this.menu.getRecipes();

         for (int var8 = this.startIndex; var8 < var6 && var8 < this.menu.getNumRecipes(); var8++) {
            int var9 = var8 - this.startIndex;
            int var10 = var4 + var9 % 4 * 16;
            int var11 = var5 + var9 / 4 * 18 + 2;
            if (param1 >= var10 && param1 < var10 + 16 && param2 >= var11 && param2 < var11 + 18) {
               this.renderTooltip(param0, var7.get(var8).getResultItem(this.minecraft.level.registryAccess()), param1, param2);
            }
         }
      }
   }

   private void renderButtons(PoseStack param0, int param1, int param2, int param3, int param4, int param5) {
      for (int var7 = this.startIndex; var7 < param5 && var7 < this.menu.getNumRecipes(); var7++) {
         int var8 = var7 - this.startIndex;
         int var9 = param3 + var8 % 4 * 16;
         int var10 = var8 / 4;
         int var11 = param4 + var10 * 18 + 2;
         int var12 = this.imageHeight;
         if (var7 == this.menu.getSelectedRecipeIndex()) {
            var12 += 18;
         } else if (param1 >= var9 && param2 >= var11 && param1 < var9 + 16 && param2 < var11 + 18) {
            var12 += 36;
         }

         blit(param0, var9, var11 - 1, 0, var12, 16, 18);
      }
   }

   private void renderRecipes(PoseStack param0, int param1, int param2, int param3) {
      List<StonecutterRecipe> var5 = this.menu.getRecipes();

      for (int var6 = this.startIndex; var6 < param3 && var6 < this.menu.getNumRecipes(); var6++) {
         int var7 = var6 - this.startIndex;
         int var8 = param1 + var7 % 4 * 16;
         int var9 = var7 / 4;
         int var10 = param2 + var9 * 18 + 2;
         this.minecraft.getItemRenderer().renderAndDecorateItem(param0, var5.get(var6).getResultItem(this.minecraft.level.registryAccess()), var8, var10);
      }
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      this.scrolling = false;
      if (this.displayRecipes) {
         int var6 = this.leftPos + 52;
         int var7 = this.topPos + 14;
         int var8 = this.startIndex + 12;

         for (int var9 = this.startIndex; var9 < var8; var9++) {
            int var10 = var9 - this.startIndex;
            double var11 = param0 - (var6 + var10 % 4 * 16);
            double var13 = param2 - (var7 + var10 / 4 * 18);
            if (var11 >= 0.0 && var13 >= 0.0 && var11 < 16.0 && var13 < 18.0 && this.menu.clickMenuButton(this.minecraft.player, var9)) {
               Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
               this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, var9);
               return true;
            }
         }

         var6 = this.leftPos + 119;
         var7 = this.topPos + 9;
         if (param0 >= var6 && param0 < var6 + 12 && param2 >= var7 && param2 < var7 + 54) {
            this.scrolling = true;
         }
      }

      return super.mouseClicked(param0, param2, param4);
   }

   @Override
   public boolean mouseDragged(double param0, double param2, int param4, double param5, double param7) {
      if (this.scrolling && this.isScrollBarActive()) {
         int var10 = this.topPos + 14;
         int var11 = var10 + 54;
         this.scrollOffs = ((float)param2 - var10 - 7.5F) / (var11 - var10 - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         this.startIndex = (int)(this.scrollOffs * this.getOffscreenRows() + 0.5) * 4;
         return true;
      } else {
         return super.mouseDragged(param0, param2, param4, param5, param7);
      }
   }

   @Override
   public boolean mouseScrolled(double param0, double param2, double param4) {
      if (this.isScrollBarActive()) {
         int var7 = this.getOffscreenRows();
         float var8 = (float)param4 / var7;
         this.scrollOffs = Mth.clamp(this.scrollOffs - var8, 0.0F, 1.0F);
         this.startIndex = (int)(this.scrollOffs * var7 + 0.5) * 4;
      }

      return true;
   }

   private boolean isScrollBarActive() {
      return this.displayRecipes && this.menu.getNumRecipes() > 12;
   }

   protected int getOffscreenRows() {
      return (this.menu.getNumRecipes() + 4 - 1) / 4 - 3;
   }

   private void containerChanged() {
      this.displayRecipes = this.menu.hasInputItem();
      if (!this.displayRecipes) {
         this.scrollOffs = 0.0F;
         this.startIndex = 0;
      }
   }
}

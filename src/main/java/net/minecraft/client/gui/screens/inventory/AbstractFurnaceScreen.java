package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.recipebook.AbstractFurnaceRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public abstract class AbstractFurnaceScreen<T extends AbstractFurnaceMenu> extends AbstractContainerScreen<T> implements RecipeUpdateListener {
   private static final ResourceLocation RECIPE_BUTTON_LOCATION = new ResourceLocation("textures/gui/recipe_button.png");
   public final AbstractFurnaceRecipeBookComponent recipeBookComponent;
   private boolean widthTooNarrow;
   private final ResourceLocation texture;

   public AbstractFurnaceScreen(T param0, AbstractFurnaceRecipeBookComponent param1, Inventory param2, Component param3, ResourceLocation param4) {
      super(param0, param2, param3);
      this.recipeBookComponent = param1;
      this.texture = param4;
   }

   @Override
   public void init() {
      super.init();
      this.widthTooNarrow = this.width < 379;
      this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
      this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
      this.addRenderableWidget(new ImageButton(this.leftPos + 20, this.height / 2 - 49, 20, 18, 0, 0, 19, RECIPE_BUTTON_LOCATION, l_param0 -> {
         this.recipeBookComponent.toggleVisibility();
         this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
         ((ImageButton)l_param0).setPosition(this.leftPos + 20, this.height / 2 - 49);
      }));
      this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
   }

   @Override
   public void containerTick() {
      super.containerTick();
      this.recipeBookComponent.tick();
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
         this.renderBg(param0, param3, param1, param2);
         this.recipeBookComponent.render(param0, param1, param2, param3);
      } else {
         this.recipeBookComponent.render(param0, param1, param2, param3);
         super.render(param0, param1, param2, param3);
         this.recipeBookComponent.renderGhostRecipe(param0, this.leftPos, this.topPos, true, param3);
      }

      this.renderTooltip(param0, param1, param2);
      this.recipeBookComponent.renderTooltip(param0, this.leftPos, this.topPos, param1, param2);
   }

   @Override
   protected void renderBg(PoseStack param0, float param1, int param2, int param3) {
      RenderSystem.setShaderTexture(0, this.texture);
      int var5 = this.leftPos;
      int var6 = this.topPos;
      blit(param0, var5, var6, 0, 0, this.imageWidth, this.imageHeight);
      if (this.menu.isLit()) {
         int var7 = this.menu.getLitProgress();
         blit(param0, var5 + 56, var6 + 36 + 12 - var7, 176, 12 - var7, 14, var7 + 1);
      }

      int var7 = this.menu.getBurnProgress();
      blit(param0, var5 + 79, var6 + 34, 176, 14, var7 + 1, 16);
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (this.recipeBookComponent.mouseClicked(param0, param2, param4)) {
         return true;
      } else {
         return this.widthTooNarrow && this.recipeBookComponent.isVisible() ? true : super.mouseClicked(param0, param2, param4);
      }
   }

   @Override
   protected void slotClicked(Slot param0, int param1, int param2, ClickType param3) {
      super.slotClicked(param0, param1, param2, param3);
      this.recipeBookComponent.slotClicked(param0);
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      return this.recipeBookComponent.keyPressed(param0, param1, param2) ? false : super.keyPressed(param0, param1, param2);
   }

   @Override
   protected boolean hasClickedOutside(double param0, double param2, int param4, int param5, int param6) {
      boolean var8 = param0 < param4 || param2 < param5 || param0 >= param4 + this.imageWidth || param2 >= param5 + this.imageHeight;
      return this.recipeBookComponent.hasClickedOutside(param0, param2, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, param6) && var8;
   }

   @Override
   public boolean charTyped(char param0, int param1) {
      return this.recipeBookComponent.charTyped(param0, param1) ? true : super.charTyped(param0, param1);
   }

   @Override
   public void recipesUpdated() {
      this.recipeBookComponent.recipesUpdated();
   }

   @Override
   public RecipeBookComponent getRecipeBookComponent() {
      return this.recipeBookComponent;
   }
}

package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.recipebook.PlaceRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public class OverlayRecipeComponent extends GuiComponent implements Renderable, GuiEventListener {
   static final ResourceLocation RECIPE_BOOK_LOCATION = new ResourceLocation("textures/gui/recipe_book.png");
   private static final int MAX_ROW = 4;
   private static final int MAX_ROW_LARGE = 5;
   private static final float ITEM_RENDER_SCALE = 0.375F;
   public static final int BUTTON_SIZE = 25;
   private final List<OverlayRecipeComponent.OverlayRecipeButton> recipeButtons = Lists.newArrayList();
   private boolean isVisible;
   private int x;
   private int y;
   Minecraft minecraft;
   private RecipeCollection collection;
   @Nullable
   private Recipe<?> lastRecipeClicked;
   float time;
   boolean isFurnaceMenu;

   public void init(Minecraft param0, RecipeCollection param1, int param2, int param3, int param4, int param5, float param6) {
      this.minecraft = param0;
      this.collection = param1;
      if (param0.player.containerMenu instanceof AbstractFurnaceMenu) {
         this.isFurnaceMenu = true;
      }

      boolean var8 = param0.player.getRecipeBook().isFiltering((RecipeBookMenu<?>)param0.player.containerMenu);
      List<Recipe<?>> var9 = param1.getDisplayRecipes(true);
      List<Recipe<?>> var10 = var8 ? Collections.emptyList() : param1.getDisplayRecipes(false);
      int var11 = var9.size();
      int var12 = var11 + var10.size();
      int var13 = var12 <= 16 ? 4 : 5;
      int var14 = (int)Math.ceil((float)var12 / var13);
      this.x = param2;
      this.y = param3;
      float var15 = this.x + Math.min(var12, var13) * 25;
      float var16 = param4 + 50;
      if (var15 > var16) {
         this.x = (int)(this.x - param6 * (int)((var15 - var16) / param6));
      }

      float var17 = this.y + var14 * 25;
      float var18 = param5 + 50;
      if (var17 > var18) {
         this.y = (int)(this.y - param6 * Mth.ceil((var17 - var18) / param6));
      }

      float var19 = this.y;
      float var20 = param5 - 100;
      if (var19 < var20) {
         this.y = (int)(this.y - param6 * Mth.ceil((var19 - var20) / param6));
      }

      this.isVisible = true;
      this.recipeButtons.clear();

      for (int var21 = 0; var21 < var12; var21++) {
         boolean var22 = var21 < var11;
         Recipe<?> var23 = var22 ? var9.get(var21) : var10.get(var21 - var11);
         int var24 = this.x + 4 + 25 * (var21 % var13);
         int var25 = this.y + 5 + 25 * (var21 / var13);
         if (this.isFurnaceMenu) {
            this.recipeButtons.add(new OverlayRecipeComponent.OverlaySmeltingRecipeButton(var24, var25, var23, var22));
         } else {
            this.recipeButtons.add(new OverlayRecipeComponent.OverlayRecipeButton(var24, var25, var23, var22));
         }
      }

      this.lastRecipeClicked = null;
   }

   public RecipeCollection getRecipeCollection() {
      return this.collection;
   }

   @Nullable
   public Recipe<?> getLastRecipeClicked() {
      return this.lastRecipeClicked;
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (param4 != 0) {
         return false;
      }

      for (OverlayRecipeComponent.OverlayRecipeButton var7 : this.recipeButtons) {
         if (var7.mouseClicked(param0, param2, param4)) {
            this.lastRecipeClicked = var7.recipe;
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean isMouseOver(double param0, double param2) {
      return false;
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      if (this.isVisible) {
         this.time += param3;
         RenderSystem.enableBlend();
         RenderSystem.setShaderTexture(0, RECIPE_BOOK_LOCATION);
         param0.pushPose();
         param0.translate(0.0F, 0.0F, 170.0F);
         int var5 = this.recipeButtons.size() <= 16 ? 4 : 5;
         int var6 = Math.min(this.recipeButtons.size(), var5);
         int var7 = Mth.ceil((float)this.recipeButtons.size() / var5);
         int var8 = 4;
         blitNineSliced(param0, this.x, this.y, var6 * 25 + 8, var7 * 25 + 8, 4, 32, 32, 82, 208);
         RenderSystem.disableBlend();

         for (OverlayRecipeComponent.OverlayRecipeButton var10 : this.recipeButtons) {
            var10.render(param0, param1, param2, param3);
         }

         param0.popPose();
      }
   }

   public void setVisible(boolean param0) {
      this.isVisible = param0;
   }

   public boolean isVisible() {
      return this.isVisible;
   }

   @Override
   public void setFocused(boolean param0) {
   }

   @Override
   public boolean isFocused() {
      return false;
   }

   class OverlayRecipeButton extends AbstractWidget implements PlaceRecipe<Ingredient> {
      final Recipe<?> recipe;
      private final boolean isCraftable;
      protected final List<OverlayRecipeComponent.OverlayRecipeButton.Pos> ingredientPos = Lists.newArrayList();

      public OverlayRecipeButton(int param1, int param2, Recipe<?> param3, boolean param4) {
         super(param1, param2, 200, 20, CommonComponents.EMPTY);
         this.width = 24;
         this.height = 24;
         this.recipe = param3;
         this.isCraftable = param4;
         this.calculateIngredientsPositions(param3);
      }

      protected void calculateIngredientsPositions(Recipe<?> param0) {
         this.placeRecipe(3, 3, -1, param0, param0.getIngredients().iterator(), 0);
      }

      @Override
      public void updateWidgetNarration(NarrationElementOutput param0) {
         this.defaultButtonNarrationText(param0);
      }

      @Override
      public void addItemToSlot(Iterator<Ingredient> param0, int param1, int param2, int param3, int param4) {
         ItemStack[] var6 = param0.next().getItems();
         if (var6.length != 0) {
            this.ingredientPos.add(new OverlayRecipeComponent.OverlayRecipeButton.Pos(3 + param4 * 7, 3 + param3 * 7, var6));
         }
      }

      @Override
      public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
         RenderSystem.setShaderTexture(0, OverlayRecipeComponent.RECIPE_BOOK_LOCATION);
         int var5 = 152;
         if (!this.isCraftable) {
            var5 += 26;
         }

         int var6 = OverlayRecipeComponent.this.isFurnaceMenu ? 130 : 78;
         if (this.isHoveredOrFocused()) {
            var6 += 26;
         }

         blit(param0, this.getX(), this.getY(), var5, var6, this.width, this.height);
         param0.pushPose();
         param0.translate(this.getX() + 2, this.getY() + 2, 150.0);

         for (OverlayRecipeComponent.OverlayRecipeButton.Pos var8 : this.ingredientPos) {
            param0.pushPose();
            param0.translate(var8.x, var8.y, 0.0);
            param0.scale(0.375F, 0.375F, 1.0F);
            param0.translate(-8.0, -8.0, 0.0);
            OverlayRecipeComponent.this.minecraft
               .getItemRenderer()
               .renderAndDecorateItem(param0, var8.ingredients[Mth.floor(OverlayRecipeComponent.this.time / 30.0F) % var8.ingredients.length], 0, 0);
            param0.popPose();
         }

         param0.popPose();
      }

      protected class Pos {
         public final ItemStack[] ingredients;
         public final int x;
         public final int y;

         public Pos(int param1, int param2, ItemStack[] param3) {
            this.x = param1;
            this.y = param2;
            this.ingredients = param3;
         }
      }
   }

   class OverlaySmeltingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
      public OverlaySmeltingRecipeButton(int param1, int param2, Recipe<?> param3, boolean param4) {
         super(param1, param2, param3, param4);
      }

      @Override
      protected void calculateIngredientsPositions(Recipe<?> param0) {
         ItemStack[] var2 = param0.getIngredients().get(0).getItems();
         this.ingredientPos.add(new OverlayRecipeComponent.OverlayRecipeButton.Pos(10, 10, var2));
      }
   }
}

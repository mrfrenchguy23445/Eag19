package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeBookPage {
   public static final int ITEMS_PER_PAGE = 20;
   private final List<RecipeButton> buttons = Lists.newArrayListWithCapacity(20);
   @Nullable
   private RecipeButton hoveredButton;
   private final OverlayRecipeComponent overlay = new OverlayRecipeComponent();
   private Minecraft minecraft;
   private final List<RecipeShownListener> showListeners = Lists.newArrayList();
   private List<RecipeCollection> recipeCollections = ImmutableList.of();
   private StateSwitchingButton forwardButton;
   private StateSwitchingButton backButton;
   private int totalPages;
   private int currentPage;
   private RecipeBook recipeBook;
   @Nullable
   private Recipe<?> lastClickedRecipe;
   @Nullable
   private RecipeCollection lastClickedRecipeCollection;

   public RecipeBookPage() {
      for (int var1 = 0; var1 < 20; var1++) {
         this.buttons.add(new RecipeButton());
      }
   }

   public void init(Minecraft param0, int param1, int param2) {
      this.minecraft = param0;
      this.recipeBook = param0.player.getRecipeBook();

      for (int var4 = 0; var4 < this.buttons.size(); var4++) {
         this.buttons.get(var4).setPosition(param1 + 11 + 25 * (var4 % 5), param2 + 31 + 25 * (var4 / 5));
      }

      this.forwardButton = new StateSwitchingButton(param1 + 93, param2 + 137, 12, 17, false);
      this.forwardButton.initTextureValues(1, 208, 13, 18, RecipeBookComponent.RECIPE_BOOK_LOCATION);
      this.backButton = new StateSwitchingButton(param1 + 38, param2 + 137, 12, 17, true);
      this.backButton.initTextureValues(1, 208, 13, 18, RecipeBookComponent.RECIPE_BOOK_LOCATION);
   }

   public void addListener(RecipeBookComponent param0) {
      this.showListeners.remove(param0);
      this.showListeners.add(param0);
   }

   public void updateCollections(List<RecipeCollection> param0, boolean param1) {
      this.recipeCollections = param0;
      this.totalPages = (int)Math.ceil(param0.size() / 20.0);
      if (this.totalPages <= this.currentPage || param1) {
         this.currentPage = 0;
      }

      this.updateButtonsForPage();
   }

   private void updateButtonsForPage() {
      int var1 = 20 * this.currentPage;

      for (int var2 = 0; var2 < this.buttons.size(); var2++) {
         RecipeButton var3 = this.buttons.get(var2);
         if (var1 + var2 < this.recipeCollections.size()) {
            RecipeCollection var4 = this.recipeCollections.get(var1 + var2);
            var3.init(var4, this);
            var3.visible = true;
         } else {
            var3.visible = false;
         }
      }

      this.updateArrowButtons();
   }

   private void updateArrowButtons() {
      this.forwardButton.visible = this.totalPages > 1 && this.currentPage < this.totalPages - 1;
      this.backButton.visible = this.totalPages > 1 && this.currentPage > 0;
   }

   public void render(PoseStack param0, int param1, int param2, int param3, int param4, float param5) {
      if (this.totalPages > 1) {
         String var7 = this.currentPage + 1 + "/" + this.totalPages;
         int var8 = this.minecraft.font.width(var7);
         this.minecraft.font.draw(param0, var7, param1 - var8 / 2 + 73, param2 + 141, -1);
      }

      this.hoveredButton = null;

      for (RecipeButton var8 : this.buttons) {
         var8.render(param0, param3, param4, param5);
         if (var8.visible && var8.isHoveredOrFocused()) {
            this.hoveredButton = var8;
         }
      }

      this.backButton.render(param0, param3, param4, param5);
      this.forwardButton.render(param0, param3, param4, param5);
      this.overlay.render(param0, param3, param4, param5);
   }

   public void renderTooltip(PoseStack param0, int param1, int param2) {
      if (this.minecraft.screen != null && this.hoveredButton != null && !this.overlay.isVisible()) {
         this.minecraft.screen.renderComponentTooltip(param0, this.hoveredButton.getTooltipText(this.minecraft.screen), param1, param2);
      }
   }

   @Nullable
   public Recipe<?> getLastClickedRecipe() {
      return this.lastClickedRecipe;
   }

   @Nullable
   public RecipeCollection getLastClickedRecipeCollection() {
      return this.lastClickedRecipeCollection;
   }

   public void setInvisible() {
      this.overlay.setVisible(false);
   }

   public boolean mouseClicked(double param0, double param2, int param4, int param5, int param6, int param7, int param8) {
      this.lastClickedRecipe = null;
      this.lastClickedRecipeCollection = null;
      if (this.overlay.isVisible()) {
         if (this.overlay.mouseClicked(param0, param2, param4)) {
            this.lastClickedRecipe = this.overlay.getLastRecipeClicked();
            this.lastClickedRecipeCollection = this.overlay.getRecipeCollection();
         } else {
            this.overlay.setVisible(false);
         }

         return true;
      } else {
         if (this.forwardButton.mouseClicked(param0, param2, param4)) {
            this.currentPage++;
            this.updateButtonsForPage();
            return true;
         }

         if (this.backButton.mouseClicked(param0, param2, param4)) {
            this.currentPage--;
            this.updateButtonsForPage();
            return true;
         }

         for (RecipeButton var11 : this.buttons) {
            if (var11.mouseClicked(param0, param2, param4)) {
               if (param4 == 0) {
                  this.lastClickedRecipe = var11.getRecipe();
                  this.lastClickedRecipeCollection = var11.getCollection();
               } else if (param4 == 1 && !this.overlay.isVisible() && !var11.isOnlyOption()) {
                  this.overlay
                     .init(this.minecraft, var11.getCollection(), var11.getX(), var11.getY(), param5 + param7 / 2, param6 + 13 + param8 / 2, var11.getWidth());
               }

               return true;
            }
         }

         return false;
      }
   }

   public void recipesShown(List<Recipe<?>> param0) {
      for (RecipeShownListener var3 : this.showListeners) {
         var3.recipesShown(param0);
      }
   }

   public Minecraft getMinecraft() {
      return this.minecraft;
   }

   public RecipeBook getRecipeBook() {
      return this.recipeBook;
   }

   protected void listButtons(Consumer<AbstractWidget> param0) {
      param0.accept(this.forwardButton);
      param0.accept(this.backButton);
      this.buttons.forEach(param0);
   }
}

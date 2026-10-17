package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeButton extends AbstractWidget {
   private static final ResourceLocation RECIPE_BOOK_LOCATION = new ResourceLocation("textures/gui/recipe_book.png");
   private static final float ANIMATION_TIME = 15.0F;
   private static final int BACKGROUND_SIZE = 25;
   public static final int TICKS_TO_SWAP = 30;
   private static final Component MORE_RECIPES_TOOLTIP = Component.translatable("gui.recipebook.moreRecipes");
   private RecipeBookMenu<?> menu;
   private RecipeBook book;
   private RecipeCollection collection;
   private float time;
   private float animationTime;
   private int currentIndex;

   public RecipeButton() {
      super(0, 0, 25, 25, CommonComponents.EMPTY);
   }

   public void init(RecipeCollection param0, RecipeBookPage param1) {
      this.collection = param0;
      this.menu = (RecipeBookMenu<?>)param1.getMinecraft().player.containerMenu;
      this.book = param1.getRecipeBook();
      List<Recipe<?>> var3 = param0.getRecipes(this.book.isFiltering(this.menu));

      for (Recipe<?> var5 : var3) {
         if (this.book.willHighlight(var5)) {
            param1.recipesShown(var3);
            this.animationTime = 15.0F;
            break;
         }
      }
   }

   public RecipeCollection getCollection() {
      return this.collection;
   }

   @Override
   public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
      if (!Screen.hasControlDown()) {
         this.time += param3;
      }

      Minecraft var5 = Minecraft.getInstance();
      RenderSystem.setShaderTexture(0, RECIPE_BOOK_LOCATION);
      int var6 = 29;
      if (!this.collection.hasCraftable()) {
         var6 += 25;
      }

      int var7 = 206;
      if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
         var7 += 25;
      }

      boolean var8 = this.animationTime > 0.0F;
      if (var8) {
         float var9 = 1.0F + 0.1F * (float)Math.sin(this.animationTime / 15.0F * (float) Math.PI);
         param0.pushPose();
         param0.translate(this.getX() + 8, this.getY() + 12, 0.0F);
         param0.scale(var9, var9, 1.0F);
         param0.translate(-(this.getX() + 8), -(this.getY() + 12), 0.0F);
         this.animationTime -= param3;
      }

      blit(param0, this.getX(), this.getY(), var6, var7, this.width, this.height);
      List<Recipe<?>> var9 = this.getOrderedRecipes();
      this.currentIndex = Mth.floor(this.time / 30.0F) % var9.size();
      ItemStack var10 = var9.get(this.currentIndex).getResultItem(this.collection.registryAccess());
      int var11 = 4;
      if (this.collection.hasSingleResultItem() && this.getOrderedRecipes().size() > 1) {
         var5.getItemRenderer().renderAndDecorateItem(param0, var10, this.getX() + var11 + 1, this.getY() + var11 + 1, 0, 10);
         var11--;
      }

      var5.getItemRenderer().renderAndDecorateFakeItem(param0, var10, this.getX() + var11, this.getY() + var11);
      if (var8) {
         param0.popPose();
      }
   }

   private List<Recipe<?>> getOrderedRecipes() {
      List<Recipe<?>> var1 = this.collection.getDisplayRecipes(true);
      if (!this.book.isFiltering(this.menu)) {
         var1.addAll(this.collection.getDisplayRecipes(false));
      }

      return var1;
   }

   public boolean isOnlyOption() {
      return this.getOrderedRecipes().size() == 1;
   }

   public Recipe<?> getRecipe() {
      List<Recipe<?>> var1 = this.getOrderedRecipes();
      return var1.get(this.currentIndex);
   }

   public List<Component> getTooltipText(Screen param0) {
      ItemStack var2 = this.getOrderedRecipes().get(this.currentIndex).getResultItem(this.collection.registryAccess());
      List<Component> var3 = Lists.newArrayList(param0.getTooltipFromItem(var2));
      if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
         var3.add(MORE_RECIPES_TOOLTIP);
      }

      return var3;
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput param0) {
      ItemStack var2 = this.getOrderedRecipes().get(this.currentIndex).getResultItem(this.collection.registryAccess());
      param0.add(NarratedElementType.TITLE, Component.translatable("narration.recipe", var2.getHoverName()));
      if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
         param0.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"), Component.translatable("narration.recipe.usage.more"));
      } else {
         param0.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
      }
   }

   @Override
   public int getWidth() {
      return 25;
   }

   @Override
   protected boolean isValidClickButton(int param0) {
      return param0 == 0 || param0 == 1;
   }
}

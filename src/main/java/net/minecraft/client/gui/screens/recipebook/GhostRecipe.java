package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public class GhostRecipe {
   @Nullable
   private Recipe<?> recipe;
   private final List<GhostRecipe.GhostIngredient> ingredients = Lists.newArrayList();
   float time;

   public void clear() {
      this.recipe = null;
      this.ingredients.clear();
      this.time = 0.0F;
   }

   public void addIngredient(Ingredient param0, int param1, int param2) {
      this.ingredients.add(new GhostRecipe.GhostIngredient(param0, param1, param2));
   }

   public GhostRecipe.GhostIngredient get(int param0) {
      return this.ingredients.get(param0);
   }

   public int size() {
      return this.ingredients.size();
   }

   @Nullable
   public Recipe<?> getRecipe() {
      return this.recipe;
   }

   public void setRecipe(Recipe<?> param0) {
      this.recipe = param0;
   }

   public void render(PoseStack param0, Minecraft param1, int param2, int param3, boolean param4, float param5) {
      if (!Screen.hasControlDown()) {
         this.time += param5;
      }

      for (int var7 = 0; var7 < this.ingredients.size(); var7++) {
         GhostRecipe.GhostIngredient var8 = this.ingredients.get(var7);
         int var9 = var8.getX() + param2;
         int var10 = var8.getY() + param3;
         if (var7 == 0 && param4) {
            GuiComponent.fill(param0, var9 - 4, var10 - 4, var9 + 20, var10 + 20, 822018048);
         } else {
            GuiComponent.fill(param0, var9, var10, var9 + 16, var10 + 16, 822018048);
         }

         ItemStack var11 = var8.getItem();
         ItemRenderer var12 = param1.getItemRenderer();
         var12.renderAndDecorateFakeItem(param0, var11, var9, var10);
         RenderSystem.depthFunc(516);
         GuiComponent.fill(param0, var9, var10, var9 + 16, var10 + 16, 822083583);
         RenderSystem.depthFunc(515);
         if (var7 == 0) {
            var12.renderGuiItemDecorations(param0, param1.font, var11, var9, var10);
         }
      }
   }

   public class GhostIngredient {
      private final Ingredient ingredient;
      private final int x;
      private final int y;

      public GhostIngredient(Ingredient param1, int param2, int param3) {
         this.ingredient = param1;
         this.x = param2;
         this.y = param3;
      }

      public int getX() {
         return this.x;
      }

      public int getY() {
         return this.y;
      }

      public ItemStack getItem() {
         ItemStack[] var1 = this.ingredient.getItems();
         return var1.length == 0 ? ItemStack.EMPTY : var1[Mth.floor(GhostRecipe.this.time / 30.0F) % var1.length];
      }
   }
}

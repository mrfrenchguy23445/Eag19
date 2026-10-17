package net.minecraft.client.gui.screens.recipebook;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public abstract class AbstractFurnaceRecipeBookComponent extends RecipeBookComponent {
   @Nullable
   private Ingredient fuels;

   @Override
   protected void initFilterButtonTextures() {
      this.filterButton.initTextureValues(152, 182, 28, 18, RECIPE_BOOK_LOCATION);
   }

   @Override
   public void slotClicked(@Nullable Slot param0) {
      super.slotClicked(param0);
      if (param0 != null && param0.index < this.menu.getSize()) {
         this.ghostRecipe.clear();
      }
   }

   @Override
   public void setupGhostRecipe(Recipe<?> param0, List<Slot> param1) {
      ItemStack var3 = param0.getResultItem(this.minecraft.level.registryAccess());
      this.ghostRecipe.setRecipe(param0);
      this.ghostRecipe.addIngredient(Ingredient.of(var3), param1.get(2).x, param1.get(2).y);
      NonNullList<Ingredient> var4 = param0.getIngredients();
      Slot var5 = param1.get(1);
      if (var5.getItem().isEmpty()) {
         if (this.fuels == null) {
            this.fuels = Ingredient.of(
               this.getFuelItems().stream().filter(l_param0 -> l_param0.isEnabled(this.minecraft.level.enabledFeatures())).map(ItemStack::new)
            );
         }

         this.ghostRecipe.addIngredient(this.fuels, var5.x, var5.y);
      }

      Iterator<Ingredient> var6 = var4.iterator();

      for (int var7 = 0; var7 < 2; var7++) {
         if (!var6.hasNext()) {
            return;
         }

         Ingredient var8 = var6.next();
         if (!var8.isEmpty()) {
            Slot var9 = param1.get(var7);
            this.ghostRecipe.addIngredient(var8, var9.x, var9.y);
         }
      }
   }

   protected abstract Set<Item> getFuelItems();
}

package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeCollection {
   private final RegistryAccess registryAccess;
   private final List<Recipe<?>> recipes;
   private final boolean singleResultItem;
   private final Set<Recipe<?>> craftable = Sets.newHashSet();
   private final Set<Recipe<?>> fitsDimensions = Sets.newHashSet();
   private final Set<Recipe<?>> known = Sets.newHashSet();

   public RecipeCollection(RegistryAccess param0, List<Recipe<?>> param1) {
      this.registryAccess = param0;
      this.recipes = ImmutableList.copyOf(param1);
      if (param1.size() <= 1) {
         this.singleResultItem = true;
      } else {
         this.singleResultItem = allRecipesHaveSameResult(param0, param1);
      }
   }

   private static boolean allRecipesHaveSameResult(RegistryAccess param0, List<Recipe<?>> param1) {
      int var2 = param1.size();
      ItemStack var3 = param1.get(0).getResultItem(param0);

      for (int var4 = 1; var4 < var2; var4++) {
         ItemStack var5 = param1.get(var4).getResultItem(param0);
         if (!ItemStack.isSame(var3, var5) || !ItemStack.tagMatches(var3, var5)) {
            return false;
         }
      }

      return true;
   }

   public RegistryAccess registryAccess() {
      return this.registryAccess;
   }

   public boolean hasKnownRecipes() {
      return !this.known.isEmpty();
   }

   public void updateKnownRecipes(RecipeBook param0) {
      for (Recipe<?> var3 : this.recipes) {
         if (param0.contains(var3)) {
            this.known.add(var3);
         }
      }
   }

   public void canCraft(StackedContents param0, int param1, int param2, RecipeBook param3) {
      for (Recipe<?> var6 : this.recipes) {
         boolean var7 = var6.canCraftInDimensions(param1, param2) && param3.contains(var6);
         if (var7) {
            this.fitsDimensions.add(var6);
         } else {
            this.fitsDimensions.remove(var6);
         }

         if (var7 && param0.canCraft(var6, null)) {
            this.craftable.add(var6);
         } else {
            this.craftable.remove(var6);
         }
      }
   }

   public boolean isCraftable(Recipe<?> param0) {
      return this.craftable.contains(param0);
   }

   public boolean hasCraftable() {
      return !this.craftable.isEmpty();
   }

   public boolean hasFitting() {
      return !this.fitsDimensions.isEmpty();
   }

   public List<Recipe<?>> getRecipes() {
      return this.recipes;
   }

   public List<Recipe<?>> getRecipes(boolean param0) {
      List<Recipe<?>> var2 = Lists.newArrayList();
      Set<Recipe<?>> var3 = param0 ? this.craftable : this.fitsDimensions;

      for (Recipe<?> var5 : this.recipes) {
         if (var3.contains(var5)) {
            var2.add(var5);
         }
      }

      return var2;
   }

   public List<Recipe<?>> getDisplayRecipes(boolean param0) {
      List<Recipe<?>> var2 = Lists.newArrayList();

      for (Recipe<?> var4 : this.recipes) {
         if (this.fitsDimensions.contains(var4) && this.craftable.contains(var4) == param0) {
            var2.add(var4);
         }
      }

      return var2;
   }

   public boolean hasSingleResultItem() {
      return this.singleResultItem;
   }
}

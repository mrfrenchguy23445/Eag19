package net.minecraft.client;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.slf4j.Logger;

public class ClientRecipeBook extends RecipeBook {
   private static final Logger LOGGER = LogUtils.getLogger();
   private Map<RecipeBookCategories, List<RecipeCollection>> collectionsByTab = ImmutableMap.of();
   private List<RecipeCollection> allCollections = ImmutableList.of();

   public void setupCollections(Iterable<Recipe<?>> param0, RegistryAccess param1) {
      Map<RecipeBookCategories, List<List<Recipe<?>>>> var3 = categorizeAndGroupRecipes(param0);
      Map<RecipeBookCategories, List<RecipeCollection>> var4 = Maps.newHashMap();
      Builder<RecipeCollection> var5 = ImmutableList.builder();
      var3.forEach(
         (l_param3, l_param4) -> var4.put(
            l_param3,
            (List)l_param4.stream()
               .map(l2_param1 -> new RecipeCollection(param1, (List<Recipe<?>>)l2_param1))
               .peek(var5::add)
               .collect(ImmutableList.toImmutableList())
         )
      );
      RecipeBookCategories.AGGREGATE_CATEGORIES
         .forEach(
            (l_param1, l_param2) -> var4.put(
               l_param1,
               (List)l_param2.stream().flatMap(l2_param1 -> var4.getOrDefault(l2_param1, ImmutableList.of()).stream()).collect(ImmutableList.toImmutableList())
            )
         );
      this.collectionsByTab = ImmutableMap.copyOf(var4);
      this.allCollections = var5.build();
   }

   private static Map<RecipeBookCategories, List<List<Recipe<?>>>> categorizeAndGroupRecipes(Iterable<Recipe<?>> param0) {
      Map<RecipeBookCategories, List<List<Recipe<?>>>> var1 = Maps.newHashMap();
      Table<RecipeBookCategories, String, List<Recipe<?>>> var2 = HashBasedTable.create();

      for (Recipe<?> var4 : param0) {
         if (!var4.isSpecial() && !var4.isIncomplete()) {
            RecipeBookCategories var5 = getCategory(var4);
            String var6 = var4.getGroup();
            if (var6.isEmpty()) {
               var1.computeIfAbsent(var5, l_param0 -> Lists.newArrayList()).add(ImmutableList.of(var4));
            } else {
               List<Recipe<?>> var7 = var2.get(var5, var6);
               if (var7 == null) {
                  var7 = Lists.newArrayList();
                  var2.put(var5, var6, var7);
                  var1.computeIfAbsent(var5, l_param0 -> Lists.newArrayList()).add(var7);
               }

               var7.add(var4);
            }
         }
      }

      return var1;
   }

   private static RecipeBookCategories getCategory(Recipe<?> param0) {
      if (param0 instanceof CraftingRecipe var1) {
         return switch (var1.category()) {
            case BUILDING -> RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
            case EQUIPMENT -> RecipeBookCategories.CRAFTING_EQUIPMENT;
            case REDSTONE -> RecipeBookCategories.CRAFTING_REDSTONE;
            case MISC -> RecipeBookCategories.CRAFTING_MISC;
         };
      } else {
         RecipeType<?> var1 = param0.getType();
         if (param0 instanceof AbstractCookingRecipe var2) {
            CookingBookCategory var3 = var2.category();
            if (var1 == RecipeType.SMELTING) {
               return switch (var3) {
                  case BLOCKS -> RecipeBookCategories.FURNACE_BLOCKS;
                  case FOOD -> RecipeBookCategories.FURNACE_FOOD;
                  case MISC -> RecipeBookCategories.FURNACE_MISC;
               };
            }

            if (var1 == RecipeType.BLASTING) {
               return var3 == CookingBookCategory.BLOCKS ? RecipeBookCategories.BLAST_FURNACE_BLOCKS : RecipeBookCategories.BLAST_FURNACE_MISC;
            }

            if (var1 == RecipeType.SMOKING) {
               return RecipeBookCategories.SMOKER_FOOD;
            }

            if (var1 == RecipeType.CAMPFIRE_COOKING) {
               return RecipeBookCategories.CAMPFIRE;
            }
         }

         if (var1 == RecipeType.STONECUTTING) {
            return RecipeBookCategories.STONECUTTER;
         }

         if (var1 == RecipeType.SMITHING) {
            return RecipeBookCategories.SMITHING;
         }

         LOGGER.warn(
            "Unknown recipe category: {}/{}", LogUtils.defer(() -> BuiltInRegistries.RECIPE_TYPE.getKey(param0.getType())), LogUtils.defer(param0::getId)
         );
         return RecipeBookCategories.UNKNOWN;
      }
   }

   public List<RecipeCollection> getCollections() {
      return this.allCollections;
   }

   public List<RecipeCollection> getCollection(RecipeBookCategories param0) {
      return this.collectionsByTab.getOrDefault(param0, Collections.emptyList());
   }
}

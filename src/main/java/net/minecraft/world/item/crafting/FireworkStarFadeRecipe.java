package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class FireworkStarFadeRecipe extends CustomRecipe {
   private static final Ingredient STAR_INGREDIENT = Ingredient.of(Items.FIREWORK_STAR);

   public FireworkStarFadeRecipe(ResourceLocation param0, CraftingBookCategory param1) {
      super(param0, param1);
   }

   public boolean matches(CraftingContainer param0, Level param1) {
      boolean var3 = false;
      boolean var4 = false;

      for (int var5 = 0; var5 < param0.getContainerSize(); var5++) {
         ItemStack var6 = param0.getItem(var5);
         if (!var6.isEmpty()) {
            if (var6.getItem() instanceof DyeItem) {
               var3 = true;
            } else {
               if (!STAR_INGREDIENT.test(var6)) {
                  return false;
               }

               if (var4) {
                  return false;
               }

               var4 = true;
            }
         }
      }

      return var4 && var3;
   }

   public ItemStack assemble(CraftingContainer param0, RegistryAccess param1) {
      List<Integer> var3 = Lists.newArrayList();
      ItemStack var4 = null;

      for (int var5 = 0; var5 < param0.getContainerSize(); var5++) {
         ItemStack var6 = param0.getItem(var5);
         Item var7 = var6.getItem();
         if (var7 instanceof DyeItem) {
            var3.add(((DyeItem)var7).getDyeColor().getFireworkColor());
         } else if (STAR_INGREDIENT.test(var6)) {
            var4 = var6.copy();
            var4.setCount(1);
         }
      }

      if (var4 != null && !var3.isEmpty()) {
         var4.getOrCreateTagElement("Explosion").putIntArray("FadeColors", var3);
         return var4;
      } else {
         return ItemStack.EMPTY;
      }
   }

   @Override
   public boolean canCraftInDimensions(int param0, int param1) {
      return param0 * param1 >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.FIREWORK_STAR_FADE;
   }
}

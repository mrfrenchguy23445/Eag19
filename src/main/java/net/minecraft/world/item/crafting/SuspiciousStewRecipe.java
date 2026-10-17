package net.minecraft.world.item.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;

public class SuspiciousStewRecipe extends CustomRecipe {
   public SuspiciousStewRecipe(ResourceLocation param0, CraftingBookCategory param1) {
      super(param0, param1);
   }

   public boolean matches(CraftingContainer param0, Level param1) {
      boolean var3 = false;
      boolean var4 = false;
      boolean var5 = false;
      boolean var6 = false;

      for (int var7 = 0; var7 < param0.getContainerSize(); var7++) {
         ItemStack var8 = param0.getItem(var7);
         if (!var8.isEmpty()) {
            if (var8.is(Blocks.BROWN_MUSHROOM.asItem()) && !var5) {
               var5 = true;
            } else if (var8.is(Blocks.RED_MUSHROOM.asItem()) && !var4) {
               var4 = true;
            } else if (var8.is(ItemTags.SMALL_FLOWERS) && !var3) {
               var3 = true;
            } else {
               if (!var8.is(Items.BOWL) || var6) {
                  return false;
               }

               var6 = true;
            }
         }
      }

      return var3 && var5 && var4 && var6;
   }

   public ItemStack assemble(CraftingContainer param0, RegistryAccess param1) {
      ItemStack var3 = new ItemStack(Items.SUSPICIOUS_STEW, 1);

      for (int var4 = 0; var4 < param0.getContainerSize(); var4++) {
         ItemStack var5 = param0.getItem(var4);
         if (!var5.isEmpty()) {
            SuspiciousEffectHolder var6 = SuspiciousEffectHolder.tryGet(var5.getItem());
            if (var6 != null) {
               SuspiciousStewItem.saveMobEffect(var3, var6.getSuspiciousEffect(), var6.getEffectDuration());
               break;
            }
         }
      }

      return var3;
   }

   @Override
   public boolean canCraftInDimensions(int param0, int param1) {
      return param0 >= 2 && param1 >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SUSPICIOUS_STEW;
   }
}

package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class FireworkStarRecipe extends CustomRecipe {
   private static final Ingredient SHAPE_INGREDIENT = Ingredient.of(
      Items.FIRE_CHARGE,
      Items.FEATHER,
      Items.GOLD_NUGGET,
      Items.SKELETON_SKULL,
      Items.WITHER_SKELETON_SKULL,
      Items.CREEPER_HEAD,
      Items.PLAYER_HEAD,
      Items.DRAGON_HEAD,
      Items.ZOMBIE_HEAD,
      Items.PIGLIN_HEAD
   );
   private static final Ingredient TRAIL_INGREDIENT = Ingredient.of(Items.DIAMOND);
   private static final Ingredient FLICKER_INGREDIENT = Ingredient.of(Items.GLOWSTONE_DUST);
   private static final Map<Item, FireworkRocketItem.Shape> SHAPE_BY_ITEM = Util.make(Maps.newHashMap(), param0 -> {
      param0.put(Items.FIRE_CHARGE, FireworkRocketItem.Shape.LARGE_BALL);
      param0.put(Items.FEATHER, FireworkRocketItem.Shape.BURST);
      param0.put(Items.GOLD_NUGGET, FireworkRocketItem.Shape.STAR);
      param0.put(Items.SKELETON_SKULL, FireworkRocketItem.Shape.CREEPER);
      param0.put(Items.WITHER_SKELETON_SKULL, FireworkRocketItem.Shape.CREEPER);
      param0.put(Items.CREEPER_HEAD, FireworkRocketItem.Shape.CREEPER);
      param0.put(Items.PLAYER_HEAD, FireworkRocketItem.Shape.CREEPER);
      param0.put(Items.DRAGON_HEAD, FireworkRocketItem.Shape.CREEPER);
      param0.put(Items.ZOMBIE_HEAD, FireworkRocketItem.Shape.CREEPER);
      param0.put(Items.PIGLIN_HEAD, FireworkRocketItem.Shape.CREEPER);
   });
   private static final Ingredient GUNPOWDER_INGREDIENT = Ingredient.of(Items.GUNPOWDER);

   public FireworkStarRecipe(ResourceLocation param0, CraftingBookCategory param1) {
      super(param0, param1);
   }

   public boolean matches(CraftingContainer param0, Level param1) {
      boolean var3 = false;
      boolean var4 = false;
      boolean var5 = false;
      boolean var6 = false;
      boolean var7 = false;

      for (int var8 = 0; var8 < param0.getContainerSize(); var8++) {
         ItemStack var9 = param0.getItem(var8);
         if (!var9.isEmpty()) {
            if (SHAPE_INGREDIENT.test(var9)) {
               if (var5) {
                  return false;
               }

               var5 = true;
            } else if (FLICKER_INGREDIENT.test(var9)) {
               if (var7) {
                  return false;
               }

               var7 = true;
            } else if (TRAIL_INGREDIENT.test(var9)) {
               if (var6) {
                  return false;
               }

               var6 = true;
            } else if (GUNPOWDER_INGREDIENT.test(var9)) {
               if (var3) {
                  return false;
               }

               var3 = true;
            } else {
               if (!(var9.getItem() instanceof DyeItem)) {
                  return false;
               }

               var4 = true;
            }
         }
      }

      return var3 && var4;
   }

   public ItemStack assemble(CraftingContainer param0, RegistryAccess param1) {
      ItemStack var3 = new ItemStack(Items.FIREWORK_STAR);
      CompoundTag var4 = var3.getOrCreateTagElement("Explosion");
      FireworkRocketItem.Shape var5 = FireworkRocketItem.Shape.SMALL_BALL;
      List<Integer> var6 = Lists.newArrayList();

      for (int var7 = 0; var7 < param0.getContainerSize(); var7++) {
         ItemStack var8 = param0.getItem(var7);
         if (!var8.isEmpty()) {
            if (SHAPE_INGREDIENT.test(var8)) {
               var5 = SHAPE_BY_ITEM.get(var8.getItem());
            } else if (FLICKER_INGREDIENT.test(var8)) {
               var4.putBoolean("Flicker", true);
            } else if (TRAIL_INGREDIENT.test(var8)) {
               var4.putBoolean("Trail", true);
            } else if (var8.getItem() instanceof DyeItem) {
               var6.add(((DyeItem)var8.getItem()).getDyeColor().getFireworkColor());
            }
         }
      }

      var4.putIntArray("Colors", var6);
      var4.putByte("Type", (byte)var5.getId());
      return var3;
   }

   @Override
   public boolean canCraftInDimensions(int param0, int param1) {
      return param0 * param1 >= 2;
   }

   @Override
   public ItemStack getResultItem(RegistryAccess param0) {
      return new ItemStack(Items.FIREWORK_STAR);
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.FIREWORK_STAR;
   }
}

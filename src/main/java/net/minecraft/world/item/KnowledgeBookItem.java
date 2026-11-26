package net.minecraft.world.item;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class KnowledgeBookItem extends Item {
   private static final String RECIPE_TAG = "Recipes";
   private static final Logger LOGGER = LogUtils.getLogger();

   public KnowledgeBookItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      ItemStack var4 = param1.getItemInHand(param2);
      CompoundTag var5 = var4.getTag();
      if (!param1.getAbilities().instabuild) {
         param1.setItemInHand(param2, ItemStack.EMPTY);
      }

      if (var5 != null && var5.contains("Recipes", 9)) {
         if (!param0.isClientSide) {
            ListTag var6 = var5.getList("Recipes", 8);
            List<Recipe<?>> var7 = Lists.newArrayList();
            RecipeManager var8 = param0.getServer().getRecipeManager();

            for (int var9 = 0; var9 < var6.size(); var9++) {
               String var10 = var6.getString(var9);
               Optional<? extends Recipe<?>> var11 = var8.byKey(new ResourceLocation(var10));
               if (!var11.isPresent()) {
                  LOGGER.error("Invalid recipe: {}", var10);
                  return InteractionResultHolder.fail(var4);
               }

               var7.add((Recipe<?>)var11.get());
            }

            param1.awardRecipes(var7);
            param1.awardStat(Stats.ITEM_USED.get(this));
         }

         return InteractionResultHolder.sidedSuccess(var4, param0.isClientSide());
      } else {
         LOGGER.error("Tag not valid: {}", var5);
         return InteractionResultHolder.fail(var4);
      }
   }
}

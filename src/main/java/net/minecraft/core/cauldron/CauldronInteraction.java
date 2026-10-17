package net.minecraft.core.cauldron;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public interface CauldronInteraction {
   Map<Item, CauldronInteraction> EMPTY = newInteractionMap();
   Map<Item, CauldronInteraction> WATER = newInteractionMap();
   Map<Item, CauldronInteraction> LAVA = newInteractionMap();
   Map<Item, CauldronInteraction> POWDER_SNOW = newInteractionMap();
   CauldronInteraction FILL_WATER = (param0, param1, param2, param3, param4, param5) -> emptyBucket(
      param1, param2, param3, param4, param5, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), SoundEvents.BUCKET_EMPTY
   );
   CauldronInteraction FILL_LAVA = (param0, param1, param2, param3, param4, param5) -> emptyBucket(
      param1, param2, param3, param4, param5, Blocks.LAVA_CAULDRON.defaultBlockState(), SoundEvents.BUCKET_EMPTY_LAVA
   );
   CauldronInteraction FILL_POWDER_SNOW = (param0, param1, param2, param3, param4, param5) -> emptyBucket(
      param1,
      param2,
      param3,
      param4,
      param5,
      Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
      SoundEvents.BUCKET_EMPTY_POWDER_SNOW
   );
   CauldronInteraction SHULKER_BOX = (param0, param1, param2, param3, param4, param5) -> {
      Block var6 = Block.byItem(param5.getItem());
      if (!(var6 instanceof ShulkerBoxBlock)) {
         return InteractionResult.PASS;
      }

      if (!param1.isClientSide) {
         ItemStack var7 = new ItemStack(Blocks.SHULKER_BOX);
         if (param5.hasTag()) {
            var7.setTag(param5.getTag().copy());
         }

         param3.setItemInHand(param4, var7);
         param3.awardStat(Stats.CLEAN_SHULKER_BOX);
         LayeredCauldronBlock.lowerFillLevel(param0, param1, param2);
      }

      return InteractionResult.sidedSuccess(param1.isClientSide);
   };
   CauldronInteraction BANNER = (param0, param1, param2, param3, param4, param5) -> {
      if (BannerBlockEntity.getPatternCount(param5) <= 0) {
         return InteractionResult.PASS;
      }

      if (!param1.isClientSide) {
         ItemStack var6 = param5.copy();
         var6.setCount(1);
         BannerBlockEntity.removeLastPattern(var6);
         if (!param3.getAbilities().instabuild) {
            param5.shrink(1);
         }

         if (param5.isEmpty()) {
            param3.setItemInHand(param4, var6);
         } else if (param3.getInventory().add(var6)) {
            param3.inventoryMenu.sendAllDataToRemote();
         } else {
            param3.drop(var6, false);
         }

         param3.awardStat(Stats.CLEAN_BANNER);
         LayeredCauldronBlock.lowerFillLevel(param0, param1, param2);
      }

      return InteractionResult.sidedSuccess(param1.isClientSide);
   };
   CauldronInteraction DYED_ITEM = (param0, param1, param2, param3, param4, param5) -> {
      if (!(param5.getItem() instanceof DyeableLeatherItem var7)) {
         return InteractionResult.PASS;
      } else {
         if (!var7.hasCustomColor(param5)) {
            return InteractionResult.PASS;
         }

         if (!param1.isClientSide) {
            var7.clearColor(param5);
            param3.awardStat(Stats.CLEAN_ARMOR);
            LayeredCauldronBlock.lowerFillLevel(param0, param1, param2);
         }

         return InteractionResult.sidedSuccess(param1.isClientSide);
      }
   };

   static Object2ObjectOpenHashMap<Item, CauldronInteraction> newInteractionMap() {
      return Util.make(
         new Object2ObjectOpenHashMap<>(),
         l_param0 -> l_param0.defaultReturnValue((l2_param0, l2_param1, l2_param2, l2_param3, l2_param4, l2_param5) -> InteractionResult.PASS)
      );
   }

   InteractionResult interact(BlockState var1, Level var2, BlockPos var3, Player var4, InteractionHand var5, ItemStack var6);

   static void bootStrap() {
      addDefaultInteractions(EMPTY);
      EMPTY.put(Items.POTION, (l_param0, l_param1, l_param2, l_param3, l_param4, l_param5) -> {
         if (PotionUtils.getPotion(l_param5) != Potions.WATER) {
            return InteractionResult.PASS;
         }

         if (!l_param1.isClientSide) {
            Item l_var6 = l_param5.getItem();
            l_param3.setItemInHand(l_param4, ItemUtils.createFilledResult(l_param5, l_param3, new ItemStack(Items.GLASS_BOTTLE)));
            l_param3.awardStat(Stats.USE_CAULDRON);
            l_param3.awardStat(Stats.ITEM_USED.get(l_var6));
            l_param1.setBlockAndUpdate(l_param2, Blocks.WATER_CAULDRON.defaultBlockState());
            l_param1.playSound((Player)null, l_param2, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            l_param1.gameEvent((Entity)null, GameEvent.FLUID_PLACE, l_param2);
         }

         return InteractionResult.sidedSuccess(l_param1.isClientSide);
      });
      addDefaultInteractions(WATER);
      WATER.put(
         Items.BUCKET,
         (l_param0, l_param1, l_param2, l_param3, l_param4, l_param5) -> fillBucket(
            l_param0,
            l_param1,
            l_param2,
            l_param3,
            l_param4,
            l_param5,
            new ItemStack(Items.WATER_BUCKET),
            l2_param0 -> l2_param0.getValue(LayeredCauldronBlock.LEVEL) == 3,
            SoundEvents.BUCKET_FILL
         )
      );
      WATER.put(
         Items.GLASS_BOTTLE,
         (l_param0, l_param1, l_param2, l_param3, l_param4, l_param5) -> {
            if (!l_param1.isClientSide) {
               Item l_var6 = l_param5.getItem();
               l_param3.setItemInHand(
                  l_param4, ItemUtils.createFilledResult(l_param5, l_param3, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER))
               );
               l_param3.awardStat(Stats.USE_CAULDRON);
               l_param3.awardStat(Stats.ITEM_USED.get(l_var6));
               LayeredCauldronBlock.lowerFillLevel(l_param0, l_param1, l_param2);
               l_param1.playSound((Player)null, l_param2, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
               l_param1.gameEvent((Entity)null, GameEvent.FLUID_PICKUP, l_param2);
            }

            return InteractionResult.sidedSuccess(l_param1.isClientSide);
         }
      );
      WATER.put(Items.POTION, (l_param0, l_param1, l_param2, l_param3, l_param4, l_param5) -> {
         if (l_param0.getValue(LayeredCauldronBlock.LEVEL) != 3 && PotionUtils.getPotion(l_param5) == Potions.WATER) {
            if (!l_param1.isClientSide) {
               l_param3.setItemInHand(l_param4, ItemUtils.createFilledResult(l_param5, l_param3, new ItemStack(Items.GLASS_BOTTLE)));
               l_param3.awardStat(Stats.USE_CAULDRON);
               l_param3.awardStat(Stats.ITEM_USED.get(l_param5.getItem()));
               l_param1.setBlockAndUpdate(l_param2, l_param0.cycle(LayeredCauldronBlock.LEVEL));
               l_param1.playSound((Player)null, l_param2, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
               l_param1.gameEvent((Entity)null, GameEvent.FLUID_PLACE, l_param2);
            }

            return InteractionResult.sidedSuccess(l_param1.isClientSide);
         } else {
            return InteractionResult.PASS;
         }
      });
      WATER.put(Items.LEATHER_BOOTS, DYED_ITEM);
      WATER.put(Items.LEATHER_LEGGINGS, DYED_ITEM);
      WATER.put(Items.LEATHER_CHESTPLATE, DYED_ITEM);
      WATER.put(Items.LEATHER_HELMET, DYED_ITEM);
      WATER.put(Items.LEATHER_HORSE_ARMOR, DYED_ITEM);
      WATER.put(Items.WHITE_BANNER, BANNER);
      WATER.put(Items.GRAY_BANNER, BANNER);
      WATER.put(Items.BLACK_BANNER, BANNER);
      WATER.put(Items.BLUE_BANNER, BANNER);
      WATER.put(Items.BROWN_BANNER, BANNER);
      WATER.put(Items.CYAN_BANNER, BANNER);
      WATER.put(Items.GREEN_BANNER, BANNER);
      WATER.put(Items.LIGHT_BLUE_BANNER, BANNER);
      WATER.put(Items.LIGHT_GRAY_BANNER, BANNER);
      WATER.put(Items.LIME_BANNER, BANNER);
      WATER.put(Items.MAGENTA_BANNER, BANNER);
      WATER.put(Items.ORANGE_BANNER, BANNER);
      WATER.put(Items.PINK_BANNER, BANNER);
      WATER.put(Items.PURPLE_BANNER, BANNER);
      WATER.put(Items.RED_BANNER, BANNER);
      WATER.put(Items.YELLOW_BANNER, BANNER);
      WATER.put(Items.WHITE_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.GRAY_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.BLACK_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.BLUE_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.BROWN_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.CYAN_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.GREEN_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.LIGHT_BLUE_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.LIGHT_GRAY_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.LIME_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.MAGENTA_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.ORANGE_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.PINK_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.PURPLE_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.RED_SHULKER_BOX, SHULKER_BOX);
      WATER.put(Items.YELLOW_SHULKER_BOX, SHULKER_BOX);
      LAVA.put(
         Items.BUCKET,
         (l_param0, l_param1, l_param2, l_param3, l_param4, l_param5) -> fillBucket(
            l_param0, l_param1, l_param2, l_param3, l_param4, l_param5, new ItemStack(Items.LAVA_BUCKET), l2_param0 -> true, SoundEvents.BUCKET_FILL_LAVA
         )
      );
      addDefaultInteractions(LAVA);
      POWDER_SNOW.put(
         Items.BUCKET,
         (l_param0, l_param1, l_param2, l_param3, l_param4, l_param5) -> fillBucket(
            l_param0,
            l_param1,
            l_param2,
            l_param3,
            l_param4,
            l_param5,
            new ItemStack(Items.POWDER_SNOW_BUCKET),
            l2_param0 -> l2_param0.getValue(LayeredCauldronBlock.LEVEL) == 3,
            SoundEvents.BUCKET_FILL_POWDER_SNOW
         )
      );
      addDefaultInteractions(POWDER_SNOW);
   }

   static void addDefaultInteractions(Map<Item, CauldronInteraction> param0) {
      param0.put(Items.LAVA_BUCKET, FILL_LAVA);
      param0.put(Items.WATER_BUCKET, FILL_WATER);
      param0.put(Items.POWDER_SNOW_BUCKET, FILL_POWDER_SNOW);
   }

   static InteractionResult fillBucket(
      BlockState param0,
      Level param1,
      BlockPos param2,
      Player param3,
      InteractionHand param4,
      ItemStack param5,
      ItemStack param6,
      Predicate<BlockState> param7,
      SoundEvent param8
   ) {
      if (!param7.test(param0)) {
         return InteractionResult.PASS;
      }

      if (!param1.isClientSide) {
         Item var9 = param5.getItem();
         param3.setItemInHand(param4, ItemUtils.createFilledResult(param5, param3, param6));
         param3.awardStat(Stats.USE_CAULDRON);
         param3.awardStat(Stats.ITEM_USED.get(var9));
         param1.setBlockAndUpdate(param2, Blocks.CAULDRON.defaultBlockState());
         param1.playSound((Player)null, param2, param8, SoundSource.BLOCKS, 1.0F, 1.0F);
         param1.gameEvent((Entity)null, GameEvent.FLUID_PICKUP, param2);
      }

      return InteractionResult.sidedSuccess(param1.isClientSide);
   }

   static InteractionResult emptyBucket(
      Level param0, BlockPos param1, Player param2, InteractionHand param3, ItemStack param4, BlockState param5, SoundEvent param6
   ) {
      if (!param0.isClientSide) {
         Item var7 = param4.getItem();
         param2.setItemInHand(param3, ItemUtils.createFilledResult(param4, param2, new ItemStack(Items.BUCKET)));
         param2.awardStat(Stats.FILL_CAULDRON);
         param2.awardStat(Stats.ITEM_USED.get(var7));
         param0.setBlockAndUpdate(param1, param5);
         param0.playSound((Player)null, param1, param6, SoundSource.BLOCKS, 1.0F, 1.0F);
         param0.gameEvent((Entity)null, GameEvent.FLUID_PLACE, param1);
      }

      return InteractionResult.sidedSuccess(param0.isClientSide);
   }
}

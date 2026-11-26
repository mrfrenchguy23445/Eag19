package net.minecraft.client.color.item;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ItemColors {
   private static final int DEFAULT = -1;
   private final IdMapper<ItemColor> itemColors = new IdMapper<>(32);

   public static ItemColors createDefault(BlockColors param0) {
      ItemColors var1 = new ItemColors();
      var1.register(
         (l_param0, l_param1) -> l_param1 > 0 ? -1 : ((DyeableLeatherItem)l_param0.getItem()).getColor(l_param0),
         Items.LEATHER_HELMET,
         Items.LEATHER_CHESTPLATE,
         Items.LEATHER_LEGGINGS,
         Items.LEATHER_BOOTS,
         Items.LEATHER_HORSE_ARMOR
      );
      var1.register((l_param0, l_param1) -> GrassColor.get(0.5, 1.0), Blocks.TALL_GRASS, Blocks.LARGE_FERN);
      var1.register((l_param0, l_param1) -> {
         if (l_param1 != 1) {
            return -1;
         }

         CompoundTag l_var2 = l_param0.getTagElement("Explosion");
         int[] l_var3 = l_var2 != null && l_var2.contains("Colors", 11) ? l_var2.getIntArray("Colors") : null;
         if (l_var3 != null && l_var3.length != 0) {
            if (l_var3.length == 1) {
               return l_var3[0];
            }

            int l_var4 = 0;
            int l_var5 = 0;
            int l_var6 = 0;

            for (int l_var10 : l_var3) {
               l_var4 += (l_var10 & 0xFF0000) >> 16;
               l_var5 += (l_var10 & 0xFF00) >> 8;
               l_var6 += (l_var10 & 0xFF) >> 0;
            }

            l_var4 /= l_var3.length;
            l_var5 /= l_var3.length;
            l_var6 /= l_var3.length;
            return l_var4 << 16 | l_var5 << 8 | l_var6;
         } else {
            return 9079434;
         }
      }, Items.FIREWORK_STAR);
      var1.register((l_param0, l_param1) -> l_param1 > 0 ? -1 : PotionUtils.getColor(l_param0), Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);

      for (SpawnEggItem var3 : SpawnEggItem.eggs()) {
         var1.register((l_param1, l_param2) -> var3.getColor(l_param2), var3);
      }

      var1.register(
         (l_param1, l_param2) -> {
            BlockState l_var3 = ((BlockItem)l_param1.getItem()).getBlock().defaultBlockState();
            return param0.getColor(l_var3, null, null, l_param2);
         },
         Blocks.GRASS_BLOCK,
         Blocks.GRASS,
         Blocks.FERN,
         Blocks.VINE,
         Blocks.OAK_LEAVES,
         Blocks.SPRUCE_LEAVES,
         Blocks.BIRCH_LEAVES,
         Blocks.JUNGLE_LEAVES,
         Blocks.ACACIA_LEAVES,
         Blocks.DARK_OAK_LEAVES,
         Blocks.LILY_PAD
      );
      var1.register((l_param0, l_param1) -> FoliageColor.getMangroveColor(), Blocks.MANGROVE_LEAVES);
      var1.register((l_param0, l_param1) -> l_param1 == 0 ? PotionUtils.getColor(l_param0) : -1, Items.TIPPED_ARROW);
      var1.register((l_param0, l_param1) -> l_param1 == 0 ? -1 : MapItem.getColor(l_param0), Items.FILLED_MAP);
      return var1;
   }

   public int getColor(ItemStack param0, int param1) {
      ItemColor var3 = this.itemColors.byId(BuiltInRegistries.ITEM.getId(param0.getItem()));
      return var3 == null ? -1 : var3.getColor(param0, param1);
   }

   public void register(ItemColor param0, ItemLike... param1) {
      for (ItemLike var6 : param1) {
         this.itemColors.addMapping(param0, Item.getId(var6.asItem()));
      }
   }
}

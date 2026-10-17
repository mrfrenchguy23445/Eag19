package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WrittenBookItem extends Item {
   public static final int TITLE_LENGTH = 16;
   public static final int TITLE_MAX_LENGTH = 32;
   public static final int PAGE_EDIT_LENGTH = 1024;
   public static final int PAGE_LENGTH = 32767;
   public static final int MAX_PAGES = 100;
   public static final int MAX_GENERATION = 2;
   public static final String TAG_TITLE = "title";
   public static final String TAG_FILTERED_TITLE = "filtered_title";
   public static final String TAG_AUTHOR = "author";
   public static final String TAG_PAGES = "pages";
   public static final String TAG_FILTERED_PAGES = "filtered_pages";
   public static final String TAG_GENERATION = "generation";
   public static final String TAG_RESOLVED = "resolved";

   public WrittenBookItem(Item.Properties param0) {
      super(param0);
   }

   public static boolean makeSureTagIsValid(@Nullable CompoundTag param0) {
      if (!WritableBookItem.makeSureTagIsValid(param0)) {
         return false;
      }

      if (!param0.contains("title", 8)) {
         return false;
      }

      String var1 = param0.getString("title");
      return var1.length() > 32 ? false : param0.contains("author", 8);
   }

   public static int getGeneration(ItemStack param0) {
      return param0.getTag().getInt("generation");
   }

   public static int getPageCount(ItemStack param0) {
      CompoundTag var1 = param0.getTag();
      return var1 != null ? var1.getList("pages", 8).size() : 0;
   }

   @Override
   public Component getName(ItemStack param0) {
      CompoundTag var2 = param0.getTag();
      if (var2 != null) {
         String var3 = var2.getString("title");
         if (!StringUtil.isNullOrEmpty(var3)) {
            return Component.literal(var3);
         }
      }

      return super.getName(param0);
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      if (param0.hasTag()) {
         CompoundTag var5 = param0.getTag();
         String var6 = var5.getString("author");
         if (!StringUtil.isNullOrEmpty(var6)) {
            param2.add(Component.translatable("book.byAuthor", var6).withStyle(ChatFormatting.GRAY));
         }

         param2.add(Component.translatable("book.generation." + var5.getInt("generation")).withStyle(ChatFormatting.GRAY));
      }
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      BlockState var4 = var2.getBlockState(var3);
      if (var4.is(Blocks.LECTERN)) {
         return LecternBlock.tryPlaceBook(param0.getPlayer(), var2, var3, var4, param0.getItemInHand())
            ? InteractionResult.sidedSuccess(var2.isClientSide)
            : InteractionResult.PASS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      ItemStack var4 = param1.getItemInHand(param2);
      param1.openItemGui(var4, param2);
      param1.awardStat(Stats.ITEM_USED.get(this));
      return InteractionResultHolder.sidedSuccess(var4, param0.isClientSide());
   }

   public static boolean resolveBookComponents(ItemStack param0, @Nullable CommandSourceStack param1, @Nullable Player param2) {
      CompoundTag var3 = param0.getTag();
      if (var3 != null && !var3.getBoolean("resolved")) {
         var3.putBoolean("resolved", true);
         if (!makeSureTagIsValid(var3)) {
            return false;
         }

         ListTag var4 = var3.getList("pages", 8);
         ListTag var5 = new ListTag();

         for (int var6 = 0; var6 < var4.size(); var6++) {
            String var7 = resolvePage(param1, param2, var4.getString(var6));
            if (var7.length() > 32767) {
               return false;
            }

            var5.add(var6, StringTag.valueOf(var7));
         }

         if (var3.contains("filtered_pages", 10)) {
            CompoundTag var6 = var3.getCompound("filtered_pages");
            CompoundTag var7 = new CompoundTag();

            for (String var9 : var6.getAllKeys()) {
               String var10 = resolvePage(param1, param2, var6.getString(var9));
               if (var10.length() > 32767) {
                  return false;
               }

               var7.putString(var9, var10);
            }

            var3.put("filtered_pages", var7);
         }

         var3.put("pages", var5);
         return true;
      } else {
         return false;
      }
   }

   private static String resolvePage(@Nullable CommandSourceStack param0, @Nullable Player param1, String param2) {
      Component var3;
      try {
         var3 = Component.Serializer.fromJsonLenient(param2);
         var3 = ComponentUtils.updateForEntity(param0, var3, param1, 0);
      } catch (Exception var4) {
         var3 = Component.literal(param2);
      }

      return Component.Serializer.toJson(var3);
   }

   @Override
   public boolean isFoil(ItemStack param0) {
      return true;
   }
}

package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WritableBookItem extends Item {
   public WritableBookItem(Item.Properties param0) {
      super(param0);
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

   public static boolean makeSureTagIsValid(@Nullable CompoundTag param0) {
      if (param0 == null) {
         return false;
      }

      if (!param0.contains("pages", 9)) {
         return false;
      }

      ListTag var1 = param0.getList("pages", 8);

      for (int var2 = 0; var2 < var1.size(); var2++) {
         String var3 = var1.getString(var2);
         if (var3.length() > 32767) {
            return false;
         }
      }

      return true;
   }
}

package net.minecraft.world;

import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface Container extends Clearable {
   int LARGE_MAX_STACK_SIZE = 64;
   int DEFAULT_DISTANCE_LIMIT = 8;

   int getContainerSize();

   boolean isEmpty();

   ItemStack getItem(int var1);

   ItemStack removeItem(int var1, int var2);

   ItemStack removeItemNoUpdate(int var1);

   void setItem(int var1, ItemStack var2);

   default int getMaxStackSize() {
      return 64;
   }

   void setChanged();

   boolean stillValid(Player var1);

   default void startOpen(Player param0) {
   }

   default void stopOpen(Player param0) {
   }

   default boolean canPlaceItem(int param0, ItemStack param1) {
      return true;
   }

   default boolean canTakeItem(Container param0, int param1, ItemStack param2) {
      return true;
   }

   default int countItem(Item param0) {
      int var2 = 0;

      for (int var3 = 0; var3 < this.getContainerSize(); var3++) {
         ItemStack var4 = this.getItem(var3);
         if (var4.getItem().equals(param0)) {
            var2 += var4.getCount();
         }
      }

      return var2;
   }

   default boolean hasAnyOf(Set<Item> param0) {
      return this.hasAnyMatching(l_param1 -> !l_param1.isEmpty() && param0.contains(l_param1.getItem()));
   }

   default boolean hasAnyMatching(Predicate<ItemStack> param0) {
      for (int var2 = 0; var2 < this.getContainerSize(); var2++) {
         ItemStack var3 = this.getItem(var2);
         if (param0.test(var3)) {
            return true;
         }
      }

      return false;
   }

   static boolean stillValidBlockEntity(BlockEntity param0, Player param1) {
      return stillValidBlockEntity(param0, param1, 8);
   }

   static boolean stillValidBlockEntity(BlockEntity param0, Player param1, int param2) {
      Level var3 = param0.getLevel();
      BlockPos var4 = param0.getBlockPos();
      if (var3 == null) {
         return false;
      } else {
         return var3.getBlockEntity(var4) != param0 ? false : param1.distanceToSqr(var4.getX() + 0.5, var4.getY() + 0.5, var4.getZ() + 0.5) <= param2 * param2;
      }
   }
}

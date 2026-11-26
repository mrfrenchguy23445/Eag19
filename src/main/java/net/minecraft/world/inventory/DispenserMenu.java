package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DispenserMenu extends AbstractContainerMenu {
   private static final int SLOT_COUNT = 9;
   private static final int INV_SLOT_START = 9;
   private static final int INV_SLOT_END = 36;
   private static final int USE_ROW_SLOT_START = 36;
   private static final int USE_ROW_SLOT_END = 45;
   private final Container dispenser;

   public DispenserMenu(int param0, Inventory param1) {
      this(param0, param1, new SimpleContainer(9));
   }

   public DispenserMenu(int param0, Inventory param1, Container param2) {
      super(MenuType.GENERIC_3x3, param0);
      checkContainerSize(param2, 9);
      this.dispenser = param2;
      param2.startOpen(param1.player);

      for (int var4 = 0; var4 < 3; var4++) {
         for (int var5 = 0; var5 < 3; var5++) {
            this.addSlot(new Slot(param2, var5 + var4 * 3, 62 + var5 * 18, 17 + var4 * 18));
         }
      }

      for (int var4 = 0; var4 < 3; var4++) {
         for (int var5 = 0; var5 < 9; var5++) {
            this.addSlot(new Slot(param1, var5 + var4 * 9 + 9, 8 + var5 * 18, 84 + var4 * 18));
         }
      }

      for (int var4 = 0; var4 < 9; var4++) {
         this.addSlot(new Slot(param1, var4, 8 + var4 * 18, 142));
      }
   }

   @Override
   public boolean stillValid(Player param0) {
      return this.dispenser.stillValid(param0);
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         var3 = var5.copy();
         if (param1 < 9) {
            if (!this.moveItemStackTo(var5, 9, 45, true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(var5, 0, 9, false)) {
            return ItemStack.EMPTY;
         }

         if (var5.isEmpty()) {
            var4.setByPlayer(ItemStack.EMPTY);
         } else {
            var4.setChanged();
         }

         if (var5.getCount() == var3.getCount()) {
            return ItemStack.EMPTY;
         }

         var4.onTake(param0, var5);
      }

      return var3;
   }

   @Override
   public void removed(Player param0) {
      super.removed(param0);
      this.dispenser.stopOpen(param0);
   }
}

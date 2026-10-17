package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HopperMenu extends AbstractContainerMenu {
   public static final int CONTAINER_SIZE = 5;
   private final Container hopper;

   public HopperMenu(int param0, Inventory param1) {
      this(param0, param1, new SimpleContainer(5));
   }

   public HopperMenu(int param0, Inventory param1, Container param2) {
      super(MenuType.HOPPER, param0);
      this.hopper = param2;
      checkContainerSize(param2, 5);
      param2.startOpen(param1.player);
      int var4 = 51;

      for (int var5 = 0; var5 < 5; var5++) {
         this.addSlot(new Slot(param2, var5, 44 + var5 * 18, 20));
      }

      for (int var5 = 0; var5 < 3; var5++) {
         for (int var6 = 0; var6 < 9; var6++) {
            this.addSlot(new Slot(param1, var6 + var5 * 9 + 9, 8 + var6 * 18, var5 * 18 + 51));
         }
      }

      for (int var5 = 0; var5 < 9; var5++) {
         this.addSlot(new Slot(param1, var5, 8 + var5 * 18, 109));
      }
   }

   @Override
   public boolean stillValid(Player param0) {
      return this.hopper.stillValid(param0);
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         var3 = var5.copy();
         if (param1 < this.hopper.getContainerSize()) {
            if (!this.moveItemStackTo(var5, this.hopper.getContainerSize(), this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(var5, 0, this.hopper.getContainerSize(), false)) {
            return ItemStack.EMPTY;
         }

         if (var5.isEmpty()) {
            var4.setByPlayer(ItemStack.EMPTY);
         } else {
            var4.setChanged();
         }
      }

      return var3;
   }

   @Override
   public void removed(Player param0) {
      super.removed(param0);
      this.hopper.stopOpen(param0);
   }
}

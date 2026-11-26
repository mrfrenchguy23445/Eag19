package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ShulkerBoxMenu extends AbstractContainerMenu {
   private static final int CONTAINER_SIZE = 27;
   private final Container container;

   public ShulkerBoxMenu(int param0, Inventory param1) {
      this(param0, param1, new SimpleContainer(27));
   }

   public ShulkerBoxMenu(int param0, Inventory param1, Container param2) {
      super(MenuType.SHULKER_BOX, param0);
      checkContainerSize(param2, 27);
      this.container = param2;
      param2.startOpen(param1.player);
      int var4 = 3;
      int var5 = 9;

      for (int var6 = 0; var6 < 3; var6++) {
         for (int var7 = 0; var7 < 9; var7++) {
            this.addSlot(new ShulkerBoxSlot(param2, var7 + var6 * 9, 8 + var7 * 18, 18 + var6 * 18));
         }
      }

      for (int var6 = 0; var6 < 3; var6++) {
         for (int var7 = 0; var7 < 9; var7++) {
            this.addSlot(new Slot(param1, var7 + var6 * 9 + 9, 8 + var7 * 18, 84 + var6 * 18));
         }
      }

      for (int var6 = 0; var6 < 9; var6++) {
         this.addSlot(new Slot(param1, var6, 8 + var6 * 18, 142));
      }
   }

   @Override
   public boolean stillValid(Player param0) {
      return this.container.stillValid(param0);
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         var3 = var5.copy();
         if (param1 < this.container.getContainerSize()) {
            if (!this.moveItemStackTo(var5, this.container.getContainerSize(), this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(var5, 0, this.container.getContainerSize(), false)) {
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
      this.container.stopOpen(param0);
   }
}

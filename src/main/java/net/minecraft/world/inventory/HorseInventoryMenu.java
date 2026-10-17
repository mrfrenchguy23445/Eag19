package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class HorseInventoryMenu extends AbstractContainerMenu {
   private final Container horseContainer;
   private final AbstractHorse horse;

   public HorseInventoryMenu(int param0, Inventory param1, Container param2, final AbstractHorse param3) {
      super(null, param0);
      this.horseContainer = param2;
      this.horse = param3;
      int var5 = 3;
      param2.startOpen(param1.player);
      int var6 = -18;
      this.addSlot(new Slot(param2, 0, 8, 18) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return l_param0.is(Items.SADDLE) && !this.hasItem() && param3.isSaddleable();
         }

         @Override
         public boolean isActive() {
            return param3.isSaddleable();
         }
      });
      this.addSlot(new Slot(param2, 1, 8, 36) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return param3.isArmor(l_param0);
         }

         @Override
         public boolean isActive() {
            return param3.canWearArmor();
         }

         @Override
         public int getMaxStackSize() {
            return 1;
         }
      });
      if (this.hasChest(param3)) {
         for (int var7 = 0; var7 < 3; var7++) {
            for (int var8 = 0; var8 < ((AbstractChestedHorse)param3).getInventoryColumns(); var8++) {
               this.addSlot(new Slot(param2, 2 + var8 + var7 * ((AbstractChestedHorse)param3).getInventoryColumns(), 80 + var8 * 18, 18 + var7 * 18));
            }
         }
      }

      for (int var7 = 0; var7 < 3; var7++) {
         for (int var8 = 0; var8 < 9; var8++) {
            this.addSlot(new Slot(param1, var8 + var7 * 9 + 9, 8 + var8 * 18, 102 + var7 * 18 + -18));
         }
      }

      for (int var7 = 0; var7 < 9; var7++) {
         this.addSlot(new Slot(param1, var7, 8 + var7 * 18, 142));
      }
   }

   @Override
   public boolean stillValid(Player param0) {
      return !this.horse.hasInventoryChanged(this.horseContainer)
         && this.horseContainer.stillValid(param0)
         && this.horse.isAlive()
         && this.horse.distanceTo(param0) < 8.0F;
   }

   private boolean hasChest(AbstractHorse param0) {
      return param0 instanceof AbstractChestedHorse && ((AbstractChestedHorse)param0).hasChest();
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         var3 = var5.copy();
         int var6 = this.horseContainer.getContainerSize();
         if (param1 < var6) {
            if (!this.moveItemStackTo(var5, var6, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(1).mayPlace(var5) && !this.getSlot(1).hasItem()) {
            if (!this.moveItemStackTo(var5, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(0).mayPlace(var5)) {
            if (!this.moveItemStackTo(var5, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (var6 <= 2 || !this.moveItemStackTo(var5, 2, var6, false)) {
            int var7 = var6;
            int var8 = var7 + 27;
            int var9 = var8;
            int var10 = var9 + 9;
            if (param1 >= var9 && param1 < var10) {
               if (!this.moveItemStackTo(var5, var7, var8, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (param1 >= var7 && param1 < var8) {
               if (!this.moveItemStackTo(var5, var9, var10, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(var5, var9, var8, false)) {
               return ItemStack.EMPTY;
            }

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
      this.horseContainer.stopOpen(param0);
   }
}

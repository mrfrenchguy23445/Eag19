package net.minecraft.world.entity.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public interface InventoryCarrier {
   String TAG_INVENTORY = "Inventory";

   SimpleContainer getInventory();

   static void pickUpItem(Mob param0, InventoryCarrier param1, ItemEntity param2) {
      ItemStack var3 = param2.getItem();
      if (param0.wantsToPickUp(var3)) {
         SimpleContainer var4 = param1.getInventory();
         boolean var5 = var4.canAddItem(var3);
         if (!var5) {
            return;
         }

         param0.onItemPickup(param2);
         int var6 = var3.getCount();
         ItemStack var7 = var4.addItem(var3);
         param0.take(param2, var6 - var7.getCount());
         if (var7.isEmpty()) {
            param2.discard();
         } else {
            var3.setCount(var7.getCount());
         }
      }
   }

   default void readInventoryFromTag(CompoundTag param0) {
      if (param0.contains("Inventory", 9)) {
         this.getInventory().fromTag(param0.getList("Inventory", 10));
      }
   }

   default void writeInventoryToTag(CompoundTag param0) {
      param0.put("Inventory", this.getInventory().createTag());
   }
}

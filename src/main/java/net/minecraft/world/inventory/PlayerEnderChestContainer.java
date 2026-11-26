package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;

public class PlayerEnderChestContainer extends SimpleContainer {
   @Nullable
   private EnderChestBlockEntity activeChest;

   public PlayerEnderChestContainer() {
      super(27);
   }

   public void setActiveChest(EnderChestBlockEntity param0) {
      this.activeChest = param0;
   }

   public boolean isActiveChest(EnderChestBlockEntity param0) {
      return this.activeChest == param0;
   }

   @Override
   public void fromTag(ListTag param0) {
      for (int var2 = 0; var2 < this.getContainerSize(); var2++) {
         this.setItem(var2, ItemStack.EMPTY);
      }

      for (int var2 = 0; var2 < param0.size(); var2++) {
         CompoundTag var3 = param0.getCompound(var2);
         int var4 = var3.getByte("Slot") & 255;
         if (var4 >= 0 && var4 < this.getContainerSize()) {
            this.setItem(var4, ItemStack.of(var3));
         }
      }
   }

   @Override
   public ListTag createTag() {
      ListTag var1 = new ListTag();

      for (int var2 = 0; var2 < this.getContainerSize(); var2++) {
         ItemStack var3 = this.getItem(var2);
         if (!var3.isEmpty()) {
            CompoundTag var4 = new CompoundTag();
            var4.putByte("Slot", (byte)var2);
            var3.save(var4);
            var1.add(var4);
         }
      }

      return var1;
   }

   @Override
   public boolean stillValid(Player param0) {
      return this.activeChest != null && !this.activeChest.stillValid(param0) ? false : super.stillValid(param0);
   }

   @Override
   public void startOpen(Player param0) {
      if (this.activeChest != null) {
         this.activeChest.startOpen(param0);
      }

      super.startOpen(param0);
   }

   @Override
   public void stopOpen(Player param0) {
      if (this.activeChest != null) {
         this.activeChest.stopOpen(param0);
      }

      super.stopOpen(param0);
      this.activeChest = null;
   }
}

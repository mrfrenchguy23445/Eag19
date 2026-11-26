package net.minecraft.world;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SimpleContainer implements Container, StackedContentsCompatible {
   private final int size;
   private final NonNullList<ItemStack> items;
   @Nullable
   private List<ContainerListener> listeners;

   public SimpleContainer(int param0) {
      this.size = param0;
      this.items = NonNullList.withSize(param0, ItemStack.EMPTY);
   }

   public SimpleContainer(ItemStack... param0) {
      this.size = param0.length;
      this.items = NonNullList.of(ItemStack.EMPTY, param0);
   }

   public void addListener(ContainerListener param0) {
      if (this.listeners == null) {
         this.listeners = Lists.newArrayList();
      }

      this.listeners.add(param0);
   }

   public void removeListener(ContainerListener param0) {
      if (this.listeners != null) {
         this.listeners.remove(param0);
      }
   }

   @Override
   public ItemStack getItem(int param0) {
      return param0 >= 0 && param0 < this.items.size() ? this.items.get(param0) : ItemStack.EMPTY;
   }

   public List<ItemStack> removeAllItems() {
      List<ItemStack> var1 = this.items.stream().filter(l_param0 -> !l_param0.isEmpty()).collect(Collectors.toList());
      this.clearContent();
      return var1;
   }

   @Override
   public ItemStack removeItem(int param0, int param1) {
      ItemStack var3 = ContainerHelper.removeItem(this.items, param0, param1);
      if (!var3.isEmpty()) {
         this.setChanged();
      }

      return var3;
   }

   public ItemStack removeItemType(Item param0, int param1) {
      ItemStack var3 = new ItemStack(param0, 0);

      for (int var4 = this.size - 1; var4 >= 0; var4--) {
         ItemStack var5 = this.getItem(var4);
         if (var5.getItem().equals(param0)) {
            int var6 = param1 - var3.getCount();
            ItemStack var7 = var5.split(var6);
            var3.grow(var7.getCount());
            if (var3.getCount() == param1) {
               break;
            }
         }
      }

      if (!var3.isEmpty()) {
         this.setChanged();
      }

      return var3;
   }

   public ItemStack addItem(ItemStack param0) {
      ItemStack var2 = param0.copy();
      this.moveItemToOccupiedSlotsWithSameType(var2);
      if (var2.isEmpty()) {
         return ItemStack.EMPTY;
      }

      this.moveItemToEmptySlots(var2);
      return var2.isEmpty() ? ItemStack.EMPTY : var2;
   }

   public boolean canAddItem(ItemStack param0) {
      boolean var2 = false;

      for (ItemStack var4 : this.items) {
         if (var4.isEmpty() || ItemStack.isSameItemSameTags(var4, param0) && var4.getCount() < var4.getMaxStackSize()) {
            var2 = true;
            break;
         }
      }

      return var2;
   }

   @Override
   public ItemStack removeItemNoUpdate(int param0) {
      ItemStack var2 = this.items.get(param0);
      if (var2.isEmpty()) {
         return ItemStack.EMPTY;
      }

      this.items.set(param0, ItemStack.EMPTY);
      return var2;
   }

   @Override
   public void setItem(int param0, ItemStack param1) {
      this.items.set(param0, param1);
      if (!param1.isEmpty() && param1.getCount() > this.getMaxStackSize()) {
         param1.setCount(this.getMaxStackSize());
      }

      this.setChanged();
   }

   @Override
   public int getContainerSize() {
      return this.size;
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack var2 : this.items) {
         if (!var2.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public void setChanged() {
      if (this.listeners != null) {
         for (ContainerListener var2 : this.listeners) {
            var2.containerChanged(this);
         }
      }
   }

   @Override
   public boolean stillValid(Player param0) {
      return true;
   }

   @Override
   public void clearContent() {
      this.items.clear();
      this.setChanged();
   }

   @Override
   public void fillStackedContents(StackedContents param0) {
      for (ItemStack var3 : this.items) {
         param0.accountStack(var3);
      }
   }

   @Override
   public String toString() {
      return this.items.stream().filter(l_param0 -> !l_param0.isEmpty()).collect(Collectors.toList()).toString();
   }

   private void moveItemToEmptySlots(ItemStack param0) {
      for (int var2 = 0; var2 < this.size; var2++) {
         ItemStack var3 = this.getItem(var2);
         if (var3.isEmpty()) {
            this.setItem(var2, param0.copy());
            param0.setCount(0);
            return;
         }
      }
   }

   private void moveItemToOccupiedSlotsWithSameType(ItemStack param0) {
      for (int var2 = 0; var2 < this.size; var2++) {
         ItemStack var3 = this.getItem(var2);
         if (ItemStack.isSameItemSameTags(var3, param0)) {
            this.moveItemsBetweenStacks(param0, var3);
            if (param0.isEmpty()) {
               return;
            }
         }
      }
   }

   private void moveItemsBetweenStacks(ItemStack param0, ItemStack param1) {
      int var3 = Math.min(this.getMaxStackSize(), param1.getMaxStackSize());
      int var4 = Math.min(param0.getCount(), var3 - param1.getCount());
      if (var4 > 0) {
         param1.grow(var4);
         param0.shrink(var4);
         this.setChanged();
      }
   }

   public void fromTag(ListTag param0) {
      this.clearContent();

      for (int var2 = 0; var2 < param0.size(); var2++) {
         ItemStack var3 = ItemStack.of(param0.getCompound(var2));
         if (!var3.isEmpty()) {
            this.addItem(var3);
         }
      }
   }

   public ListTag createTag() {
      ListTag var1 = new ListTag();

      for (int var2 = 0; var2 < this.getContainerSize(); var2++) {
         ItemStack var3 = this.getItem(var2);
         if (!var3.isEmpty()) {
            var1.add(var3.save(new CompoundTag()));
         }
      }

      return var1;
   }
}

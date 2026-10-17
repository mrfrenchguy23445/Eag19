package net.minecraft.client.player.inventory;

import com.google.common.collect.ForwardingList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class Hotbar extends ForwardingList<ItemStack> {
   private final NonNullList<ItemStack> items = NonNullList.withSize(Inventory.getSelectionSize(), ItemStack.EMPTY);

   @Override
   protected List<ItemStack> delegate() {
      return this.items;
   }

   public ListTag createTag() {
      ListTag var1 = new ListTag();

      for (ItemStack var3 : this.delegate()) {
         var1.add(var3.save(new CompoundTag()));
      }

      return var1;
   }

   public void fromTag(ListTag param0) {
      List<ItemStack> var2 = this.delegate();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         var2.set(var3, ItemStack.of(param0.getCompound(var3)));
      }
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack var2 : this.delegate()) {
         if (!var2.isEmpty()) {
            return false;
         }
      }

      return true;
   }
}

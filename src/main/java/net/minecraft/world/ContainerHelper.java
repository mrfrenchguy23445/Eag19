package net.minecraft.world;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class ContainerHelper {
   public static ItemStack removeItem(List<ItemStack> param0, int param1, int param2) {
      return param1 >= 0 && param1 < param0.size() && !param0.get(param1).isEmpty() && param2 > 0 ? param0.get(param1).split(param2) : ItemStack.EMPTY;
   }

   public static ItemStack takeItem(List<ItemStack> param0, int param1) {
      return param1 >= 0 && param1 < param0.size() ? param0.set(param1, ItemStack.EMPTY) : ItemStack.EMPTY;
   }

   public static CompoundTag saveAllItems(CompoundTag param0, NonNullList<ItemStack> param1) {
      return saveAllItems(param0, param1, true);
   }

   public static CompoundTag saveAllItems(CompoundTag param0, NonNullList<ItemStack> param1, boolean param2) {
      ListTag var3 = new ListTag();

      for (int var4 = 0; var4 < param1.size(); var4++) {
         ItemStack var5 = param1.get(var4);
         if (!var5.isEmpty()) {
            CompoundTag var6 = new CompoundTag();
            var6.putByte("Slot", (byte)var4);
            var5.save(var6);
            var3.add(var6);
         }
      }

      if (!var3.isEmpty() || param2) {
         param0.put("Items", var3);
      }

      return param0;
   }

   public static void loadAllItems(CompoundTag param0, NonNullList<ItemStack> param1) {
      ListTag var2 = param0.getList("Items", 10);

      for (int var3 = 0; var3 < var2.size(); var3++) {
         CompoundTag var4 = var2.getCompound(var3);
         int var5 = var4.getByte("Slot") & 255;
         if (var5 >= 0 && var5 < param1.size()) {
            param1.set(var5, ItemStack.of(var4));
         }
      }
   }

   public static int clearOrCountMatchingItems(Container param0, Predicate<ItemStack> param1, int param2, boolean param3) {
      int var4 = 0;

      for (int var5 = 0; var5 < param0.getContainerSize(); var5++) {
         ItemStack var6 = param0.getItem(var5);
         int var7 = clearOrCountMatchingItems(var6, param1, param2 - var4, param3);
         if (var7 > 0 && !param3 && var6.isEmpty()) {
            param0.setItem(var5, ItemStack.EMPTY);
         }

         var4 += var7;
      }

      return var4;
   }

   public static int clearOrCountMatchingItems(ItemStack param0, Predicate<ItemStack> param1, int param2, boolean param3) {
      if (param0.isEmpty() || !param1.test(param0)) {
         return 0;
      }

      if (param3) {
         return param0.getCount();
      }

      int var4 = param2 < 0 ? param0.getCount() : Math.min(param2, param0.getCount());
      param0.shrink(var4);
      return var4;
   }
}

package net.minecraft.world.item;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;

public class ItemStackLinkedSet {
   private static final Strategy<? super ItemStack> TYPE_AND_TAG = new Strategy<ItemStack>() {
      public int hashCode(@Nullable ItemStack param0) {
         return ItemStackLinkedSet.hashStackAndTag(param0);
      }

      public boolean equals(@Nullable ItemStack param0, @Nullable ItemStack param1) {
         return param0 == param1 || param0 != null && param1 != null && param0.isEmpty() == param1.isEmpty() && ItemStack.isSameItemSameTags(param0, param1);
      }
   };

   static int hashStackAndTag(@Nullable ItemStack param0) {
      if (param0 != null) {
         CompoundTag var1 = param0.getTag();
         int var2 = 31 + param0.getItem().hashCode();
         return 31 * var2 + (var1 == null ? 0 : var1.hashCode());
      } else {
         return 0;
      }
   }

   public static Set<ItemStack> createTypeAndTagSet() {
      return new ObjectLinkedOpenCustomHashSet<>(TYPE_AND_TAG);
   }
}

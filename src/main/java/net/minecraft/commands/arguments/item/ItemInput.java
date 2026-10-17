package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemInput implements Predicate<ItemStack> {
   private static final Dynamic2CommandExceptionType ERROR_STACK_TOO_BIG = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("arguments.item.overstacked", param0, param1)
   );
   private final Holder<Item> item;
   @Nullable
   private final CompoundTag tag;

   public ItemInput(Holder<Item> param0, @Nullable CompoundTag param1) {
      this.item = param0;
      this.tag = param1;
   }

   public Item getItem() {
      return this.item.value();
   }

   public boolean test(ItemStack param0) {
      return param0.is(this.item) && NbtUtils.compareNbt(this.tag, param0.getTag(), true);
   }

   public ItemStack createItemStack(int param0, boolean param1) throws CommandSyntaxException {
      ItemStack var3 = new ItemStack(this.item, param0);
      if (this.tag != null) {
         var3.setTag(this.tag);
      }

      if (param1 && param0 > var3.getMaxStackSize()) {
         throw ERROR_STACK_TOO_BIG.create(this.getItemName(), var3.getMaxStackSize());
      } else {
         return var3;
      }
   }

   public String serialize() {
      StringBuilder var1 = new StringBuilder(this.getItemName());
      if (this.tag != null) {
         var1.append(this.tag);
      }

      return var1.toString();
   }

   private String getItemName() {
      return this.item.unwrapKey().map(ResourceKey::location).map(net.minecraft.resources.ResourceLocation::toString).orElse("unknown[" + this.item + "]");
   }
}

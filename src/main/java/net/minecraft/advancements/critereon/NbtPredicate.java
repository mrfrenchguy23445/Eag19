package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NbtPredicate {
   public static final NbtPredicate ANY = new NbtPredicate(null);
   @Nullable
   private final CompoundTag tag;

   public NbtPredicate(@Nullable CompoundTag param0) {
      this.tag = param0;
   }

   public boolean matches(ItemStack param0) {
      return this == ANY ? true : this.matches(param0.getTag());
   }

   public boolean matches(Entity param0) {
      return this == ANY ? true : this.matches(getEntityTagToCompare(param0));
   }

   public boolean matches(@Nullable Tag param0) {
      return param0 == null ? this == ANY : this.tag == null || NbtUtils.compareNbt(this.tag, param0, true);
   }

   public JsonElement serializeToJson() {
      return this != ANY && this.tag != null ? new JsonPrimitive(this.tag.toString()) : JsonNull.INSTANCE;
   }

   public static NbtPredicate fromJson(@Nullable JsonElement param0) {
      if (param0 != null && !param0.isJsonNull()) {
         CompoundTag var1;
         try {
            var1 = TagParser.parseTag(GsonHelper.convertToString(param0, "nbt"));
         } catch (CommandSyntaxException var2) {
            throw new JsonSyntaxException("Invalid nbt tag: " + var2.getMessage());
         }

         return new NbtPredicate(var1);
      } else {
         return ANY;
      }
   }

   public static CompoundTag getEntityTagToCompare(Entity param0) {
      CompoundTag var1 = param0.saveWithoutId(new CompoundTag());
      if (param0 instanceof Player) {
         ItemStack var2 = ((Player)param0).getInventory().getSelected();
         if (!var2.isEmpty()) {
            var1.put("SelectedItem", var2.save(new CompoundTag()));
         }
      }

      return var1;
   }
}

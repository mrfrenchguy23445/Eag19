package net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentPredicate {
   public static final EnchantmentPredicate ANY = new EnchantmentPredicate();
   public static final EnchantmentPredicate[] NONE = new EnchantmentPredicate[0];
   @Nullable
   private final Enchantment enchantment;
   private final MinMaxBounds.Ints level;

   public EnchantmentPredicate() {
      this.enchantment = null;
      this.level = MinMaxBounds.Ints.ANY;
   }

   public EnchantmentPredicate(@Nullable Enchantment param0, MinMaxBounds.Ints param1) {
      this.enchantment = param0;
      this.level = param1;
   }

   public boolean containedIn(Map<Enchantment, Integer> param0) {
      if (this.enchantment != null) {
         if (!param0.containsKey(this.enchantment)) {
            return false;
         }

         int var2 = param0.get(this.enchantment);
         if (this.level != MinMaxBounds.Ints.ANY && !this.level.matches(var2)) {
            return false;
         }
      } else if (this.level != MinMaxBounds.Ints.ANY) {
         for (Integer var3 : param0.values()) {
            if (this.level.matches(var3)) {
               return true;
            }
         }

         return false;
      }

      return true;
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      }

      JsonObject var1 = new JsonObject();
      if (this.enchantment != null) {
         var1.addProperty("enchantment", BuiltInRegistries.ENCHANTMENT.getKey(this.enchantment).toString());
      }

      var1.add("levels", this.level.serializeToJson());
      return var1;
   }

   public static EnchantmentPredicate fromJson(@Nullable JsonElement param0) {
      if (param0 != null && !param0.isJsonNull()) {
         JsonObject var1 = GsonHelper.convertToJsonObject(param0, "enchantment");
         Enchantment var2 = null;
         if (var1.has("enchantment")) {
            ResourceLocation var3 = new ResourceLocation(GsonHelper.getAsString(var1, "enchantment"));
            var2 = BuiltInRegistries.ENCHANTMENT.getOptional(var3).orElseThrow(() -> new JsonSyntaxException("Unknown enchantment '" + var3 + "'"));
         }

         MinMaxBounds.Ints var3 = MinMaxBounds.Ints.fromJson(var1.get("levels"));
         return new EnchantmentPredicate(var2, var3);
      } else {
         return ANY;
      }
   }

   public static EnchantmentPredicate[] fromJsonArray(@Nullable JsonElement param0) {
      if (param0 != null && !param0.isJsonNull()) {
         JsonArray var1 = GsonHelper.convertToJsonArray(param0, "enchantments");
         EnchantmentPredicate[] var2 = new EnchantmentPredicate[var1.size()];

         for (int var3 = 0; var3 < var2.length; var3++) {
            var2[var3] = fromJson(var1.get(var3));
         }

         return var2;
      } else {
         return NONE;
      }
   }
}

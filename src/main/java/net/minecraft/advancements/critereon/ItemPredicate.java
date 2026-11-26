package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ItemLike;

public class ItemPredicate {
   public static final ItemPredicate ANY = new ItemPredicate();
   @Nullable
   private final TagKey<Item> tag;
   @Nullable
   private final Set<Item> items;
   private final MinMaxBounds.Ints count;
   private final MinMaxBounds.Ints durability;
   private final EnchantmentPredicate[] enchantments;
   private final EnchantmentPredicate[] storedEnchantments;
   @Nullable
   private final Potion potion;
   private final NbtPredicate nbt;

   public ItemPredicate() {
      this.tag = null;
      this.items = null;
      this.potion = null;
      this.count = MinMaxBounds.Ints.ANY;
      this.durability = MinMaxBounds.Ints.ANY;
      this.enchantments = EnchantmentPredicate.NONE;
      this.storedEnchantments = EnchantmentPredicate.NONE;
      this.nbt = NbtPredicate.ANY;
   }

   public ItemPredicate(
      @Nullable TagKey<Item> param0,
      @Nullable Set<Item> param1,
      MinMaxBounds.Ints param2,
      MinMaxBounds.Ints param3,
      EnchantmentPredicate[] param4,
      EnchantmentPredicate[] param5,
      @Nullable Potion param6,
      NbtPredicate param7
   ) {
      this.tag = param0;
      this.items = param1;
      this.count = param2;
      this.durability = param3;
      this.enchantments = param4;
      this.storedEnchantments = param5;
      this.potion = param6;
      this.nbt = param7;
   }

   public boolean matches(ItemStack param0) {
      if (this == ANY) {
         return true;
      }

      if (this.tag != null && !param0.is(this.tag)) {
         return false;
      }

      if (this.items != null && !this.items.contains(param0.getItem())) {
         return false;
      }

      if (!this.count.matches(param0.getCount())) {
         return false;
      }

      if (!this.durability.isAny() && !param0.isDamageableItem()) {
         return false;
      }

      if (!this.durability.matches(param0.getMaxDamage() - param0.getDamageValue())) {
         return false;
      }

      if (!this.nbt.matches(param0)) {
         return false;
      }

      if (this.enchantments.length > 0) {
         Map<Enchantment, Integer> var2 = EnchantmentHelper.deserializeEnchantments(param0.getEnchantmentTags());

         for (EnchantmentPredicate var6 : this.enchantments) {
            if (!var6.containedIn(var2)) {
               return false;
            }
         }
      }

      if (this.storedEnchantments.length > 0) {
         Map<Enchantment, Integer> var2 = EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(param0));

         for (EnchantmentPredicate var6 : this.storedEnchantments) {
            if (!var6.containedIn(var2)) {
               return false;
            }
         }
      }

      Potion var2 = PotionUtils.getPotion(param0);
      return this.potion == null || this.potion == var2;
   }

   public static ItemPredicate fromJson(@Nullable JsonElement param0) {
      if (param0 != null && !param0.isJsonNull()) {
         JsonObject var1 = GsonHelper.convertToJsonObject(param0, "item");
         MinMaxBounds.Ints var2 = MinMaxBounds.Ints.fromJson(var1.get("count"));
         MinMaxBounds.Ints var3 = MinMaxBounds.Ints.fromJson(var1.get("durability"));
         if (var1.has("data")) {
            throw new JsonParseException("Disallowed data tag found");
         }

         NbtPredicate var4 = NbtPredicate.fromJson(var1.get("nbt"));
         Set<Item> var5 = null;
         JsonArray var6 = GsonHelper.getAsJsonArray(var1, "items", null);
         if (var6 != null) {
            ImmutableSet.Builder<Item> var7 = ImmutableSet.builder();

            for (JsonElement var9 : var6) {
               ResourceLocation var10 = new ResourceLocation(GsonHelper.convertToString(var9, "item"));
               var7.add(BuiltInRegistries.ITEM.getOptional(var10).orElseThrow(() -> new JsonSyntaxException("Unknown item id '" + var10 + "'")));
            }

            var5 = var7.build();
         }

         TagKey<Item> var7 = null;
         if (var1.has("tag")) {
            ResourceLocation var8 = new ResourceLocation(GsonHelper.getAsString(var1, "tag"));
            var7 = TagKey.create(Registries.ITEM, var8);
         }

         Potion var8 = null;
         if (var1.has("potion")) {
            ResourceLocation var9 = new ResourceLocation(GsonHelper.getAsString(var1, "potion"));
            var8 = BuiltInRegistries.POTION.getOptional(var9).orElseThrow(() -> new JsonSyntaxException("Unknown potion '" + var9 + "'"));
         }

         EnchantmentPredicate[] var9 = EnchantmentPredicate.fromJsonArray(var1.get("enchantments"));
         EnchantmentPredicate[] var10 = EnchantmentPredicate.fromJsonArray(var1.get("stored_enchantments"));
         return new ItemPredicate(var7, var5, var2, var3, var9, var10, var8, var4);
      } else {
         return ANY;
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      }

      JsonObject var1 = new JsonObject();
      if (this.items != null) {
         JsonArray var2 = new JsonArray();

         for (Item var4 : this.items) {
            var2.add(BuiltInRegistries.ITEM.getKey(var4).toString());
         }

         var1.add("items", var2);
      }

      if (this.tag != null) {
         var1.addProperty("tag", this.tag.location().toString());
      }

      var1.add("count", this.count.serializeToJson());
      var1.add("durability", this.durability.serializeToJson());
      var1.add("nbt", this.nbt.serializeToJson());
      if (this.enchantments.length > 0) {
         JsonArray var2 = new JsonArray();

         for (EnchantmentPredicate var6 : this.enchantments) {
            var2.add(var6.serializeToJson());
         }

         var1.add("enchantments", var2);
      }

      if (this.storedEnchantments.length > 0) {
         JsonArray var2 = new JsonArray();

         for (EnchantmentPredicate var6 : this.storedEnchantments) {
            var2.add(var6.serializeToJson());
         }

         var1.add("stored_enchantments", var2);
      }

      if (this.potion != null) {
         var1.addProperty("potion", BuiltInRegistries.POTION.getKey(this.potion).toString());
      }

      return var1;
   }

   public static ItemPredicate[] fromJsonArray(@Nullable JsonElement param0) {
      if (param0 != null && !param0.isJsonNull()) {
         JsonArray var1 = GsonHelper.convertToJsonArray(param0, "items");
         ItemPredicate[] var2 = new ItemPredicate[var1.size()];

         for (int var3 = 0; var3 < var2.length; var3++) {
            var2[var3] = fromJson(var1.get(var3));
         }

         return var2;
      } else {
         return new ItemPredicate[0];
      }
   }

   public static class Builder {
      private final List<EnchantmentPredicate> enchantments = Lists.newArrayList();
      private final List<EnchantmentPredicate> storedEnchantments = Lists.newArrayList();
      @Nullable
      private Set<Item> items;
      @Nullable
      private TagKey<Item> tag;
      private MinMaxBounds.Ints count = MinMaxBounds.Ints.ANY;
      private MinMaxBounds.Ints durability = MinMaxBounds.Ints.ANY;
      @Nullable
      private Potion potion;
      private NbtPredicate nbt = NbtPredicate.ANY;

      private Builder() {
      }

      public static ItemPredicate.Builder item() {
         return new ItemPredicate.Builder();
      }

      public ItemPredicate.Builder of(ItemLike... param0) {
         this.items = Stream.of(param0).map(ItemLike::asItem).collect(ImmutableSet.toImmutableSet());
         return this;
      }

      public ItemPredicate.Builder of(TagKey<Item> param0) {
         this.tag = param0;
         return this;
      }

      public ItemPredicate.Builder withCount(MinMaxBounds.Ints param0) {
         this.count = param0;
         return this;
      }

      public ItemPredicate.Builder hasDurability(MinMaxBounds.Ints param0) {
         this.durability = param0;
         return this;
      }

      public ItemPredicate.Builder isPotion(Potion param0) {
         this.potion = param0;
         return this;
      }

      public ItemPredicate.Builder hasNbt(CompoundTag param0) {
         this.nbt = new NbtPredicate(param0);
         return this;
      }

      public ItemPredicate.Builder hasEnchantment(EnchantmentPredicate param0) {
         this.enchantments.add(param0);
         return this;
      }

      public ItemPredicate.Builder hasStoredEnchantment(EnchantmentPredicate param0) {
         this.storedEnchantments.add(param0);
         return this;
      }

      public ItemPredicate build() {
         return new ItemPredicate(
            this.tag,
            this.items,
            this.count,
            this.durability,
            this.enchantments.toArray(EnchantmentPredicate.NONE),
            this.storedEnchantments.toArray(EnchantmentPredicate.NONE),
            this.potion,
            this.nbt
         );
      }
   }
}

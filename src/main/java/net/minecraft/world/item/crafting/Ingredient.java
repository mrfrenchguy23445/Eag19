package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class Ingredient implements Predicate<ItemStack> {
   public static final Ingredient EMPTY = new Ingredient(Stream.empty());
   private final Ingredient.Value[] values;
   @Nullable
   private ItemStack[] itemStacks;
   @Nullable
   private IntList stackingIds;

   private Ingredient(Stream<? extends Ingredient.Value> param0) {
      this.values = param0.toArray(Ingredient.Value[]::new);
   }

   public ItemStack[] getItems() {
      if (this.itemStacks == null) {
         this.itemStacks = Arrays.stream(this.values).flatMap(l_param0 -> l_param0.getItems().stream()).distinct().toArray(ItemStack[]::new);
      }

      return this.itemStacks;
   }

   public boolean test(@Nullable ItemStack param0) {
      if (param0 == null) {
         return false;
      }

      if (this.isEmpty()) {
         return param0.isEmpty();
      }

      for (ItemStack var5 : this.getItems()) {
         if (var5.is(param0.getItem())) {
            return true;
         }
      }

      return false;
   }

   public IntList getStackingIds() {
      if (this.stackingIds == null) {
         ItemStack[] var1 = this.getItems();
         this.stackingIds = new IntArrayList(var1.length);

         for (ItemStack var5 : var1) {
            this.stackingIds.add(StackedContents.getStackingIndex(var5));
         }

         this.stackingIds.sort(IntComparators.NATURAL_COMPARATOR);
      }

      return this.stackingIds;
   }

   public void toNetwork(FriendlyByteBuf param0) {
      param0.writeCollection(Arrays.asList(this.getItems()), FriendlyByteBuf::writeItem);
   }

   public JsonElement toJson() {
      if (this.values.length == 1) {
         return this.values[0].serialize();
      }

      JsonArray var1 = new JsonArray();

      for (Ingredient.Value var5 : this.values) {
         var1.add(var5.serialize());
      }

      return var1;
   }

   public boolean isEmpty() {
      return this.values.length == 0;
   }

   private static Ingredient fromValues(Stream<? extends Ingredient.Value> param0) {
      Ingredient var1 = new Ingredient(param0);
      return var1.isEmpty() ? EMPTY : var1;
   }

   public static Ingredient of() {
      return EMPTY;
   }

   public static Ingredient of(ItemLike... param0) {
      return of(Arrays.stream(param0).map(ItemStack::new));
   }

   public static Ingredient of(ItemStack... param0) {
      return of(Arrays.stream(param0));
   }

   public static Ingredient of(Stream<ItemStack> param0) {
      return fromValues(param0.filter(l_param0 -> !l_param0.isEmpty()).map(Ingredient.ItemValue::new));
   }

   public static Ingredient of(TagKey<Item> param0) {
      return fromValues(Stream.of(new Ingredient.TagValue(param0)));
   }

   public static Ingredient fromNetwork(FriendlyByteBuf param0) {
      return fromValues(param0.<ItemStack>readList(FriendlyByteBuf::readItem).stream().map(Ingredient.ItemValue::new));
   }

   public static Ingredient fromJson(@Nullable JsonElement param0) {
      if (param0 == null || param0.isJsonNull()) {
         throw new JsonSyntaxException("Item cannot be null");
      }

      if (param0.isJsonObject()) {
         return fromValues(Stream.of(valueFromJson(param0.getAsJsonObject())));
      }

      if (param0.isJsonArray()) {
         JsonArray var1 = param0.getAsJsonArray();
         if (var1.size() == 0) {
            throw new JsonSyntaxException("Item array cannot be empty, at least one item must be defined");
         } else {
            return fromValues(StreamSupport.stream(var1.spliterator(), false).map(l_param0 -> valueFromJson(GsonHelper.convertToJsonObject(l_param0, "item"))));
         }
      } else {
         throw new JsonSyntaxException("Expected item to be object or array of objects");
      }
   }

   private static Ingredient.Value valueFromJson(JsonObject param0) {
      if (param0.has("item") && param0.has("tag")) {
         throw new JsonParseException("An ingredient entry is either a tag or an item, not both");
      } else if (param0.has("item")) {
         Item var1 = ShapedRecipe.itemFromJson(param0);
         return new Ingredient.ItemValue(new ItemStack(var1));
      } else if (param0.has("tag")) {
         ResourceLocation var1 = new ResourceLocation(GsonHelper.getAsString(param0, "tag"));
         TagKey<Item> var2 = TagKey.create(Registries.ITEM, var1);
         return new Ingredient.TagValue(var2);
      } else {
         throw new JsonParseException("An ingredient entry needs either a tag or an item");
      }
   }

   static class ItemValue implements Ingredient.Value {
      private final ItemStack item;

      ItemValue(ItemStack param0) {
         this.item = param0;
      }

      @Override
      public Collection<ItemStack> getItems() {
         return Collections.singleton(this.item);
      }

      @Override
      public JsonObject serialize() {
         JsonObject var1 = new JsonObject();
         var1.addProperty("item", BuiltInRegistries.ITEM.getKey(this.item.getItem()).toString());
         return var1;
      }
   }

   static class TagValue implements Ingredient.Value {
      private final TagKey<Item> tag;

      TagValue(TagKey<Item> param0) {
         this.tag = param0;
      }

      @Override
      public Collection<ItemStack> getItems() {
         List<ItemStack> var1 = Lists.newArrayList();

         for (Holder<Item> var3 : BuiltInRegistries.ITEM.getTagOrEmpty(this.tag)) {
            var1.add(new ItemStack(var3));
         }

         return var1;
      }

      @Override
      public JsonObject serialize() {
         JsonObject var1 = new JsonObject();
         var1.addProperty("tag", this.tag.location().toString());
         return var1;
      }
   }

   interface Value {
      Collection<ItemStack> getItems();

      JsonObject serialize();
   }
}

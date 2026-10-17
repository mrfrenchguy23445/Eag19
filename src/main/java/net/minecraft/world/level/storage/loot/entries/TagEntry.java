package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class TagEntry extends LootPoolSingletonContainer {
   final TagKey<Item> tag;
   final boolean expand;

   TagEntry(TagKey<Item> param0, boolean param1, int param2, int param3, LootItemCondition[] param4, LootItemFunction[] param5) {
      super(param2, param3, param4, param5);
      this.tag = param0;
      this.expand = param1;
   }

   @Override
   public LootPoolEntryType getType() {
      return LootPoolEntries.TAG;
   }

   @Override
   public void createItemStack(Consumer<ItemStack> param0, LootContext param1) {
      BuiltInRegistries.ITEM.getTagOrEmpty(this.tag).forEach(l_param1 -> param0.accept(new ItemStack((Holder<Item>)l_param1)));
   }

   private boolean expandTag(LootContext param0, Consumer<LootPoolEntry> param1) {
      if (!this.canRun(param0)) {
         return false;
      }

      for (final Holder<Item> var4 : BuiltInRegistries.ITEM.getTagOrEmpty(this.tag)) {
         param1.accept(new LootPoolSingletonContainer.EntryBase() {
            @Override
            public void createItemStack(Consumer<ItemStack> l_param0, LootContext l_param1) {
               l_param0.accept(new ItemStack(var4));
            }
         });
      }

      return true;
   }

   @Override
   public boolean expand(LootContext param0, Consumer<LootPoolEntry> param1) {
      return this.expand ? this.expandTag(param0, param1) : super.expand(param0, param1);
   }

   public static LootPoolSingletonContainer.Builder<?> tagContents(TagKey<Item> param0) {
      return simpleBuilder((l_param1, l_param2, l_param3, l_param4) -> new TagEntry(param0, false, l_param1, l_param2, l_param3, l_param4));
   }

   public static LootPoolSingletonContainer.Builder<?> expandTag(TagKey<Item> param0) {
      return simpleBuilder((l_param1, l_param2, l_param3, l_param4) -> new TagEntry(param0, true, l_param1, l_param2, l_param3, l_param4));
   }

   public static class Serializer extends LootPoolSingletonContainer.Serializer<TagEntry> {
      public void serializeCustom(JsonObject param0, TagEntry param1, JsonSerializationContext param2) {
         super.serializeCustom(param0, param1, param2);
         param0.addProperty("name", param1.tag.location().toString());
         param0.addProperty("expand", param1.expand);
      }

      protected TagEntry deserialize(
         JsonObject param0, JsonDeserializationContext param1, int param2, int param3, LootItemCondition[] param4, LootItemFunction[] param5
      ) {
         ResourceLocation var7 = new ResourceLocation(GsonHelper.getAsString(param0, "name"));
         TagKey<Item> var8 = TagKey.create(Registries.ITEM, var7);
         boolean var9 = GsonHelper.getAsBoolean(param0, "expand");
         return new TagEntry(var8, var9, param2, param3, param4, param5);
      }
   }
}

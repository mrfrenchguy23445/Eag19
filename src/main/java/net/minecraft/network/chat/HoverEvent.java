package net.minecraft.network.chat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class HoverEvent {
   static final Logger LOGGER = LogUtils.getLogger();
   private final HoverEvent.Action<?> action;
   private final Object value;

   public <T> HoverEvent(HoverEvent.Action<T> param0, T param1) {
      this.action = param0;
      this.value = param1;
   }

   public HoverEvent.Action<?> getAction() {
      return this.action;
   }

   @Nullable
   public <T> T getValue(HoverEvent.Action<T> param0) {
      return this.action == param0 ? param0.cast(this.value) : null;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else if (param0 != null && this.getClass() == param0.getClass()) {
         HoverEvent var2 = (HoverEvent)param0;
         return this.action == var2.action && Objects.equals(this.value, var2.value);
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      return "HoverEvent{action=" + this.action + ", value='" + this.value + "'}";
   }

   @Override
   public int hashCode() {
      int var1 = this.action.hashCode();
      return 31 * var1 + (this.value != null ? this.value.hashCode() : 0);
   }

   @Nullable
   public static HoverEvent deserialize(JsonObject param0) {
      String var1 = GsonHelper.getAsString(param0, "action", null);
      if (var1 == null) {
         return null;
      }

      HoverEvent.Action<?> var2 = HoverEvent.Action.getByName(var1);
      if (var2 == null) {
         return null;
      }

      JsonElement var3 = param0.get("contents");
      if (var3 != null) {
         return var2.deserialize(var3);
      }

      Component var4 = Component.Serializer.fromJson(param0.get("value"));
      return var4 != null ? var2.deserializeFromLegacy(var4) : null;
   }

   public JsonObject serialize() {
      JsonObject var1 = new JsonObject();
      var1.addProperty("action", this.action.getName());
      var1.add("contents", this.action.serializeArg(this.value));
      return var1;
   }

   public static class Action<T> {
      public static final HoverEvent.Action<Component> SHOW_TEXT = new HoverEvent.Action<>(
         "show_text", true, Component.Serializer::fromJson, Component.Serializer::toJsonTree, Function.identity()
      );
      public static final HoverEvent.Action<HoverEvent.ItemStackInfo> SHOW_ITEM = new HoverEvent.Action<>(
         "show_item", true, HoverEvent.ItemStackInfo::create, HoverEvent.ItemStackInfo::serialize, HoverEvent.ItemStackInfo::create
      );
      public static final HoverEvent.Action<HoverEvent.EntityTooltipInfo> SHOW_ENTITY = new HoverEvent.Action<>(
         "show_entity", true, HoverEvent.EntityTooltipInfo::create, HoverEvent.EntityTooltipInfo::serialize, HoverEvent.EntityTooltipInfo::create
      );
      private static final Map<String, HoverEvent.Action<?>> LOOKUP = Stream.of(SHOW_TEXT, SHOW_ITEM, SHOW_ENTITY)
         .collect(ImmutableMap.toImmutableMap(HoverEvent.Action::getName, param0 -> param0));
      private final String name;
      private final boolean allowFromServer;
      private final Function<JsonElement, T> argDeserializer;
      private final Function<T, JsonElement> argSerializer;
      private final Function<Component, T> legacyArgDeserializer;

      public Action(String param0, boolean param1, Function<JsonElement, T> param2, Function<T, JsonElement> param3, Function<Component, T> param4) {
         this.name = param0;
         this.allowFromServer = param1;
         this.argDeserializer = param2;
         this.argSerializer = param3;
         this.legacyArgDeserializer = param4;
      }

      public boolean isAllowedFromServer() {
         return this.allowFromServer;
      }

      public String getName() {
         return this.name;
      }

      @Nullable
      public static HoverEvent.Action<?> getByName(String param0) {
         return LOOKUP.get(param0);
      }

      T cast(Object param0) {
         return (T)param0;
      }

      @Nullable
      public HoverEvent deserialize(JsonElement param0) {
         T var2 = this.argDeserializer.apply(param0);
         return var2 == null ? null : new HoverEvent(this, var2);
      }

      @Nullable
      public HoverEvent deserializeFromLegacy(Component param0) {
         T var2 = this.legacyArgDeserializer.apply(param0);
         return var2 == null ? null : new HoverEvent(this, var2);
      }

      public JsonElement serializeArg(Object param0) {
         return this.argSerializer.apply(this.cast(param0));
      }

      @Override
      public String toString() {
         return "<action " + this.name + ">";
      }
   }

   public static class EntityTooltipInfo {
      public final EntityType<?> type;
      public final UUID id;
      @Nullable
      public final Component name;
      @Nullable
      private List<Component> linesCache;

      public EntityTooltipInfo(EntityType<?> param0, UUID param1, @Nullable Component param2) {
         this.type = param0;
         this.id = param1;
         this.name = param2;
      }

      @Nullable
      public static HoverEvent.EntityTooltipInfo create(JsonElement param0) {
         if (!param0.isJsonObject()) {
            return null;
         }

         JsonObject var1 = param0.getAsJsonObject();
         EntityType<?> var2 = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(GsonHelper.getAsString(var1, "type")));
         UUID var3 = UUID.fromString(GsonHelper.getAsString(var1, "id"));
         Component var4 = Component.Serializer.fromJson(var1.get("name"));
         return new HoverEvent.EntityTooltipInfo(var2, var3, var4);
      }

      @Nullable
      public static HoverEvent.EntityTooltipInfo create(Component param0) {
         try {
            CompoundTag var1 = TagParser.parseTag(param0.getString());
            Component var2 = Component.Serializer.fromJson(var1.getString("name"));
            EntityType<?> var3 = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(var1.getString("type")));
            UUID var4 = UUID.fromString(var1.getString("id"));
            return new HoverEvent.EntityTooltipInfo(var3, var4, var2);
         } catch (Exception var1) {
            return null;
         }
      }

      public JsonElement serialize() {
         JsonObject var1 = new JsonObject();
         var1.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(this.type).toString());
         var1.addProperty("id", this.id.toString());
         if (this.name != null) {
            var1.add("name", Component.Serializer.toJsonTree(this.name));
         }

         return var1;
      }

      public List<Component> getTooltipLines() {
         if (this.linesCache == null) {
            this.linesCache = Lists.newArrayList();
            if (this.name != null) {
               this.linesCache.add(this.name);
            }

            this.linesCache.add(Component.translatable("gui.entity_tooltip.type", this.type.getDescription()));
            this.linesCache.add(Component.literal(this.id.toString()));
         }

         return this.linesCache;
      }

      @Override
      public boolean equals(Object param0) {
         if (this == param0) {
            return true;
         } else if (param0 != null && this.getClass() == param0.getClass()) {
            HoverEvent.EntityTooltipInfo var2 = (HoverEvent.EntityTooltipInfo)param0;
            return this.type.equals(var2.type) && this.id.equals(var2.id) && Objects.equals(this.name, var2.name);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int var1 = this.type.hashCode();
         var1 = 31 * var1 + this.id.hashCode();
         return 31 * var1 + (this.name != null ? this.name.hashCode() : 0);
      }
   }

   public static class ItemStackInfo {
      private final Item item;
      private final int count;
      @Nullable
      private final CompoundTag tag;
      @Nullable
      private ItemStack itemStack;

      ItemStackInfo(Item param0, int param1, @Nullable CompoundTag param2) {
         this.item = param0;
         this.count = param1;
         this.tag = param2;
      }

      public ItemStackInfo(ItemStack param0) {
         this(param0.getItem(), param0.getCount(), param0.getTag() != null ? param0.getTag().copy() : null);
      }

      @Override
      public boolean equals(Object param0) {
         if (this == param0) {
            return true;
         } else if (param0 != null && this.getClass() == param0.getClass()) {
            HoverEvent.ItemStackInfo var2 = (HoverEvent.ItemStackInfo)param0;
            return this.count == var2.count && this.item.equals(var2.item) && Objects.equals(this.tag, var2.tag);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int var1 = this.item.hashCode();
         var1 = 31 * var1 + this.count;
         return 31 * var1 + (this.tag != null ? this.tag.hashCode() : 0);
      }

      public ItemStack getItemStack() {
         if (this.itemStack == null) {
            this.itemStack = new ItemStack(this.item, this.count);
            if (this.tag != null) {
               this.itemStack.setTag(this.tag);
            }
         }

         return this.itemStack;
      }

      private static HoverEvent.ItemStackInfo create(JsonElement param0) {
         if (param0.isJsonPrimitive()) {
            return new HoverEvent.ItemStackInfo(BuiltInRegistries.ITEM.get(new ResourceLocation(param0.getAsString())), 1, null);
         }

         JsonObject var1 = GsonHelper.convertToJsonObject(param0, "item");
         Item var2 = BuiltInRegistries.ITEM.get(new ResourceLocation(GsonHelper.getAsString(var1, "id")));
         int var3 = GsonHelper.getAsInt(var1, "count", 1);
         if (var1.has("tag")) {
            String var4 = GsonHelper.getAsString(var1, "tag");

            try {
               CompoundTag var5 = TagParser.parseTag(var4);
               return new HoverEvent.ItemStackInfo(var2, var3, var5);
            } catch (CommandSyntaxException var5) {
               HoverEvent.LOGGER.warn("Failed to parse tag: {}", var4, var5);
            }
         }

         return new HoverEvent.ItemStackInfo(var2, var3, null);
      }

      @Nullable
      private static HoverEvent.ItemStackInfo create(Component param0) {
         try {
            CompoundTag var1 = TagParser.parseTag(param0.getString());
            return new HoverEvent.ItemStackInfo(ItemStack.of(var1));
         } catch (CommandSyntaxException var1) {
            HoverEvent.LOGGER.warn("Failed to parse item tag: {}", param0, var1);
            return null;
         }
      }

      private JsonElement serialize() {
         JsonObject var1 = new JsonObject();
         var1.addProperty("id", BuiltInRegistries.ITEM.getKey(this.item).toString());
         if (this.count != 1) {
            var1.addProperty("count", this.count);
         }

         if (this.tag != null) {
            var1.addProperty("tag", this.tag.toString());
         }

         return var1;
      }
   }
}

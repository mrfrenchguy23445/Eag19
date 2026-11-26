package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetLoreFunction extends LootItemConditionalFunction {
   final boolean replace;
   final List<Component> lore;
   @Nullable
   final LootContext.EntityTarget resolutionContext;

   public SetLoreFunction(LootItemCondition[] param0, boolean param1, List<Component> param2, @Nullable LootContext.EntityTarget param3) {
      super(param0);
      this.replace = param1;
      this.lore = ImmutableList.copyOf(param2);
      this.resolutionContext = param3;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_LORE;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.resolutionContext != null ? ImmutableSet.of(this.resolutionContext.getParam()) : ImmutableSet.of();
   }

   @Override
   public ItemStack run(ItemStack param0, LootContext param1) {
      ListTag var3 = this.getLoreTag(param0, !this.lore.isEmpty());
      if (var3 != null) {
         if (this.replace) {
            var3.clear();
         }

         UnaryOperator<Component> var4 = SetNameFunction.createResolver(param1, this.resolutionContext);
         this.lore.stream().map(var4).map(Component.Serializer::toJson).map(StringTag::valueOf).forEach(var3::add);
      }

      return param0;
   }

   @Nullable
   private ListTag getLoreTag(ItemStack param0, boolean param1) {
      CompoundTag var3;
      if (param0.hasTag()) {
         var3 = param0.getTag();
      } else {
         if (!param1) {
            return null;
         }

         var3 = new CompoundTag();
         param0.setTag(var3);
      }

      CompoundTag var4;
      if (var3.contains("display", 10)) {
         var4 = var3.getCompound("display");
      } else {
         if (!param1) {
            return null;
         }

         var4 = new CompoundTag();
         var3.put("display", var4);
      }

      if (var4.contains("Lore", 9)) {
         return var4.getList("Lore", 8);
      } else if (param1) {
         ListTag var5 = new ListTag();
         var4.put("Lore", var5);
         return var5;
      } else {
         return null;
      }
   }

   public static SetLoreFunction.Builder setLore() {
      return new SetLoreFunction.Builder();
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetLoreFunction.Builder> {
      private boolean replace;
      private LootContext.EntityTarget resolutionContext;
      private final List<Component> lore = Lists.newArrayList();

      public SetLoreFunction.Builder setReplace(boolean param0) {
         this.replace = param0;
         return this;
      }

      public SetLoreFunction.Builder setResolutionContext(LootContext.EntityTarget param0) {
         this.resolutionContext = param0;
         return this;
      }

      public SetLoreFunction.Builder addLine(Component param0) {
         this.lore.add(param0);
         return this;
      }

      protected SetLoreFunction.Builder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetLoreFunction(this.getConditions(), this.replace, this.lore, this.resolutionContext);
      }
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<SetLoreFunction> {
      public void serialize(JsonObject param0, SetLoreFunction param1, JsonSerializationContext param2) {
         super.serialize(param0, param1, param2);
         param0.addProperty("replace", param1.replace);
         JsonArray var4 = new JsonArray();

         for (Component var6 : param1.lore) {
            var4.add(Component.Serializer.toJsonTree(var6));
         }

         param0.add("lore", var4);
         if (param1.resolutionContext != null) {
            param0.add("entity", param2.serialize(param1.resolutionContext));
         }
      }

      public SetLoreFunction deserialize(JsonObject param0, JsonDeserializationContext param1, LootItemCondition[] param2) {
         boolean var4 = GsonHelper.getAsBoolean(param0, "replace", false);
         List<Component> var5 = Streams.stream(GsonHelper.getAsJsonArray(param0, "lore"))
            .map(Component.Serializer::fromJson)
            .collect(ImmutableList.toImmutableList());
         LootContext.EntityTarget var6 = GsonHelper.getAsObject(param0, "entity", null, param1, LootContext.EntityTarget.class);
         return new SetLoreFunction(param2, var4, var5, var6);
      }
   }
}

package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class SetAttributesFunction extends LootItemConditionalFunction {
   final List<SetAttributesFunction.Modifier> modifiers;

   SetAttributesFunction(LootItemCondition[] param0, List<SetAttributesFunction.Modifier> param1) {
      super(param0);
      this.modifiers = ImmutableList.copyOf(param1);
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_ATTRIBUTES;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.modifiers.stream().flatMap(l_param0 -> l_param0.amount.getReferencedContextParams().stream()).collect(ImmutableSet.toImmutableSet());
   }

   @Override
   public ItemStack run(ItemStack param0, LootContext param1) {
      RandomSource var3 = param1.getRandom();

      for (SetAttributesFunction.Modifier var5 : this.modifiers) {
         UUID var6 = var5.id;
         if (var6 == null) {
            var6 = UUID.randomUUID();
         }

         EquipmentSlot var7 = Util.getRandom(var5.slots, var3);
         param0.addAttributeModifier(var5.attribute, new AttributeModifier(var6, var5.name, var5.amount.getFloat(param1), var5.operation), var7);
      }

      return param0;
   }

   public static SetAttributesFunction.ModifierBuilder modifier(String param0, Attribute param1, AttributeModifier.Operation param2, NumberProvider param3) {
      return new SetAttributesFunction.ModifierBuilder(param0, param1, param2, param3);
   }

   public static SetAttributesFunction.Builder setAttributes() {
      return new SetAttributesFunction.Builder();
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetAttributesFunction.Builder> {
      private final List<SetAttributesFunction.Modifier> modifiers = Lists.newArrayList();

      protected SetAttributesFunction.Builder getThis() {
         return this;
      }

      public SetAttributesFunction.Builder withModifier(SetAttributesFunction.ModifierBuilder param0) {
         this.modifiers.add(param0.build());
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetAttributesFunction(this.getConditions(), this.modifiers);
      }
   }

   static class Modifier {
      final String name;
      final Attribute attribute;
      final AttributeModifier.Operation operation;
      final NumberProvider amount;
      @Nullable
      final UUID id;
      final EquipmentSlot[] slots;

      Modifier(String param0, Attribute param1, AttributeModifier.Operation param2, NumberProvider param3, EquipmentSlot[] param4, @Nullable UUID param5) {
         this.name = param0;
         this.attribute = param1;
         this.operation = param2;
         this.amount = param3;
         this.id = param5;
         this.slots = param4;
      }

      public JsonObject serialize(JsonSerializationContext param0) {
         JsonObject var2 = new JsonObject();
         var2.addProperty("name", this.name);
         var2.addProperty("attribute", BuiltInRegistries.ATTRIBUTE.getKey(this.attribute).toString());
         var2.addProperty("operation", operationToString(this.operation));
         var2.add("amount", param0.serialize(this.amount));
         if (this.id != null) {
            var2.addProperty("id", this.id.toString());
         }

         if (this.slots.length == 1) {
            var2.addProperty("slot", this.slots[0].getName());
         } else {
            JsonArray var3 = new JsonArray();

            for (EquipmentSlot var7 : this.slots) {
               var3.add(new JsonPrimitive(var7.getName()));
            }

            var2.add("slot", var3);
         }

         return var2;
      }

      public static SetAttributesFunction.Modifier deserialize(JsonObject param0, JsonDeserializationContext param1) {
         String var2 = GsonHelper.getAsString(param0, "name");
         ResourceLocation var3 = new ResourceLocation(GsonHelper.getAsString(param0, "attribute"));
         Attribute var4 = BuiltInRegistries.ATTRIBUTE.get(var3);
         if (var4 == null) {
            throw new JsonSyntaxException("Unknown attribute: " + var3);
         }

         AttributeModifier.Operation var5 = operationFromString(GsonHelper.getAsString(param0, "operation"));
         NumberProvider var6 = GsonHelper.getAsObject(param0, "amount", param1, NumberProvider.class);
         UUID var8 = null;
         EquipmentSlot[] var7;
         if (GsonHelper.isStringValue(param0, "slot")) {
            var7 = new EquipmentSlot[]{EquipmentSlot.byName(GsonHelper.getAsString(param0, "slot"))};
         } else {
            if (!GsonHelper.isArrayNode(param0, "slot")) {
               throw new JsonSyntaxException("Invalid or missing attribute modifier slot; must be either string or array of strings.");
            }

            JsonArray var9 = GsonHelper.getAsJsonArray(param0, "slot");
            var7 = new EquipmentSlot[var9.size()];
            int var10 = 0;

            for (JsonElement var12 : var9) {
               var7[var10++] = EquipmentSlot.byName(GsonHelper.convertToString(var12, "slot"));
            }

            if (var7.length == 0) {
               throw new JsonSyntaxException("Invalid attribute modifier slot; must contain at least one entry.");
            }
         }

         if (param0.has("id")) {
            String var9 = GsonHelper.getAsString(param0, "id");

            try {
               var8 = UUID.fromString(var9);
            } catch (IllegalArgumentException var10) {
               throw new JsonSyntaxException("Invalid attribute modifier id '" + var9 + "' (must be UUID format, with dashes)");
            }
         }

         return new SetAttributesFunction.Modifier(var2, var4, var5, var6, var7, var8);
      }

      private static String operationToString(AttributeModifier.Operation param0) {
         switch (param0) {
            case ADDITION:
               return "addition";
            case MULTIPLY_BASE:
               return "multiply_base";
            case MULTIPLY_TOTAL:
               return "multiply_total";
            default:
               throw new IllegalArgumentException("Unknown operation " + param0);
         }
      }

      private static AttributeModifier.Operation operationFromString(String param0) {
         switch (param0) {
            case "addition":
               return AttributeModifier.Operation.ADDITION;
            case "multiply_base":
               return AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total":
               return AttributeModifier.Operation.MULTIPLY_TOTAL;
            default:
               throw new JsonSyntaxException("Unknown attribute modifier operation " + param0);
         }
      }
   }

   public static class ModifierBuilder {
      private final String name;
      private final Attribute attribute;
      private final AttributeModifier.Operation operation;
      private final NumberProvider amount;
      @Nullable
      private UUID id;
      private final Set<EquipmentSlot> slots = EnumSet.noneOf(EquipmentSlot.class);

      public ModifierBuilder(String param0, Attribute param1, AttributeModifier.Operation param2, NumberProvider param3) {
         this.name = param0;
         this.attribute = param1;
         this.operation = param2;
         this.amount = param3;
      }

      public SetAttributesFunction.ModifierBuilder forSlot(EquipmentSlot param0) {
         this.slots.add(param0);
         return this;
      }

      public SetAttributesFunction.ModifierBuilder withUuid(UUID param0) {
         this.id = param0;
         return this;
      }

      public SetAttributesFunction.Modifier build() {
         return new SetAttributesFunction.Modifier(this.name, this.attribute, this.operation, this.amount, this.slots.toArray(new EquipmentSlot[0]), this.id);
      }
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<SetAttributesFunction> {
      public void serialize(JsonObject param0, SetAttributesFunction param1, JsonSerializationContext param2) {
         super.serialize(param0, param1, param2);
         JsonArray var4 = new JsonArray();

         for (SetAttributesFunction.Modifier var6 : param1.modifiers) {
            var4.add(var6.serialize(param2));
         }

         param0.add("modifiers", var4);
      }

      public SetAttributesFunction deserialize(JsonObject param0, JsonDeserializationContext param1, LootItemCondition[] param2) {
         JsonArray var4 = GsonHelper.getAsJsonArray(param0, "modifiers");
         List<SetAttributesFunction.Modifier> var5 = Lists.newArrayListWithExpectedSize(var4.size());

         for (JsonElement var7 : var4) {
            var5.add(SetAttributesFunction.Modifier.deserialize(GsonHelper.convertToJsonObject(var7, "modifier"), param1));
         }

         if (var5.isEmpty()) {
            throw new JsonSyntaxException("Invalid attribute modifiers array; cannot be empty");
         } else {
            return new SetAttributesFunction(param2, var5);
         }
      }
   }
}

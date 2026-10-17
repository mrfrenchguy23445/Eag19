package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetContainerLootTable extends LootItemConditionalFunction {
   final ResourceLocation name;
   final long seed;
   final BlockEntityType<?> type;

   SetContainerLootTable(LootItemCondition[] param0, ResourceLocation param1, long param2, BlockEntityType<?> param4) {
      super(param0);
      this.name = param1;
      this.seed = param2;
      this.type = param4;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_LOOT_TABLE;
   }

   @Override
   public ItemStack run(ItemStack param0, LootContext param1) {
      if (param0.isEmpty()) {
         return param0;
      }

      CompoundTag var3 = BlockItem.getBlockEntityData(param0);
      if (var3 == null) {
         var3 = new CompoundTag();
      }

      var3.putString("LootTable", this.name.toString());
      if (this.seed != 0L) {
         var3.putLong("LootTableSeed", this.seed);
      }

      BlockItem.setBlockEntityData(param0, this.type, var3);
      return param0;
   }

   @Override
   public void validate(ValidationContext param0) {
      if (param0.hasVisitedTable(this.name)) {
         param0.reportProblem("Table " + this.name + " is recursively called");
      } else {
         super.validate(param0);
         LootTable var2 = param0.resolveLootTable(this.name);
         if (var2 == null) {
            param0.reportProblem("Unknown loot table called " + this.name);
         } else {
            var2.validate(param0.enterTable("->{" + this.name + "}", this.name));
         }
      }
   }

   public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> param0, ResourceLocation param1) {
      return simpleBuilder(l_param2 -> new SetContainerLootTable(l_param2, param1, 0L, param0));
   }

   public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> param0, ResourceLocation param1, long param2) {
      return simpleBuilder(l_param4 -> new SetContainerLootTable(l_param4, param1, param2, param0));
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<SetContainerLootTable> {
      public void serialize(JsonObject param0, SetContainerLootTable param1, JsonSerializationContext param2) {
         super.serialize(param0, param1, param2);
         param0.addProperty("name", param1.name.toString());
         param0.addProperty("type", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(param1.type).toString());
         if (param1.seed != 0L) {
            param0.addProperty("seed", param1.seed);
         }
      }

      public SetContainerLootTable deserialize(JsonObject param0, JsonDeserializationContext param1, LootItemCondition[] param2) {
         ResourceLocation var4 = new ResourceLocation(GsonHelper.getAsString(param0, "name"));
         long var5 = GsonHelper.getAsLong(param0, "seed", 0L);
         ResourceLocation var7 = new ResourceLocation(GsonHelper.getAsString(param0, "type"));
         BlockEntityType<?> var8 = BuiltInRegistries.BLOCK_ENTITY_TYPE
            .getOptional(var7)
            .orElseThrow(() -> new JsonSyntaxException("Unknown block entity type id '" + var7 + "'"));
         return new SetContainerLootTable(param2, var4, var5, var8);
      }
   }
}

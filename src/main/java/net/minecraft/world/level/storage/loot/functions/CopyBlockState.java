package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyBlockState extends LootItemConditionalFunction {
   final Block block;
   final Set<Property<?>> properties;

   CopyBlockState(LootItemCondition[] param0, Block param1, Set<Property<?>> param2) {
      super(param0);
      this.block = param1;
      this.properties = param2;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.COPY_STATE;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(LootContextParams.BLOCK_STATE);
   }

   @Override
   protected ItemStack run(ItemStack param0, LootContext param1) {
      BlockState var3 = param1.getParamOrNull(LootContextParams.BLOCK_STATE);
      if (var3 != null) {
         CompoundTag var4 = param0.getOrCreateTag();
         CompoundTag var5;
         if (var4.contains("BlockStateTag", 10)) {
            var5 = var4.getCompound("BlockStateTag");
         } else {
            var5 = new CompoundTag();
            var4.put("BlockStateTag", var5);
         }

         this.properties.stream().filter(var3::hasProperty).forEach(l_param2 -> var5.putString(l_param2.getName(), serialize(var3, (Property<?>)l_param2)));
      }

      return param0;
   }

   public static CopyBlockState.Builder copyState(Block param0) {
      return new CopyBlockState.Builder(param0);
   }

   private static <T extends Comparable<T>> String serialize(BlockState param0, Property<T> param1) {
      T var2 = param0.getValue(param1);
      return param1.getName(var2);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<CopyBlockState.Builder> {
      private final Block block;
      private final Set<Property<?>> properties = Sets.newHashSet();

      Builder(Block param0) {
         this.block = param0;
      }

      public CopyBlockState.Builder copy(Property<?> param0) {
         if (!this.block.getStateDefinition().getProperties().contains(param0)) {
            throw new IllegalStateException("Property " + param0 + " is not present on block " + this.block);
         }

         this.properties.add(param0);
         return this;
      }

      protected CopyBlockState.Builder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new CopyBlockState(this.getConditions(), this.block, this.properties);
      }
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<CopyBlockState> {
      public void serialize(JsonObject param0, CopyBlockState param1, JsonSerializationContext param2) {
         super.serialize(param0, param1, param2);
         param0.addProperty("block", BuiltInRegistries.BLOCK.getKey(param1.block).toString());
         JsonArray var4 = new JsonArray();
         param1.properties.forEach(l_param1 -> var4.add(l_param1.getName()));
         param0.add("properties", var4);
      }

      public CopyBlockState deserialize(JsonObject param0, JsonDeserializationContext param1, LootItemCondition[] param2) {
         ResourceLocation var4 = new ResourceLocation(GsonHelper.getAsString(param0, "block"));
         Block var5 = BuiltInRegistries.BLOCK.getOptional(var4).orElseThrow(() -> new IllegalArgumentException("Can't find block " + var4));
         StateDefinition<Block, BlockState> var6 = var5.getStateDefinition();
         Set<Property<?>> var7 = Sets.newHashSet();
         JsonArray var8 = GsonHelper.getAsJsonArray(param0, "properties", null);
         if (var8 != null) {
            var8.forEach(l_param2 -> var7.add(var6.getProperty(GsonHelper.convertToString(l_param2, "property"))));
         }

         return new CopyBlockState(param2, var5, var7);
      }
   }
}

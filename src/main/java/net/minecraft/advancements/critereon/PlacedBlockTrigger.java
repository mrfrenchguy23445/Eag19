package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PlacedBlockTrigger extends SimpleCriterionTrigger<PlacedBlockTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("placed_block");

   @Override
   public ResourceLocation getId() {
      return ID;
   }

   public PlacedBlockTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      Block var4 = deserializeBlock(param0);
      StatePropertiesPredicate var5 = StatePropertiesPredicate.fromJson(param0.get("state"));
      if (var4 != null) {
         var5.checkState(var4.getStateDefinition(), l_param1 -> {
            throw new JsonSyntaxException("Block " + var4 + " has no property " + l_param1 + ":");
         });
      }

      LocationPredicate var6 = LocationPredicate.fromJson(param0.get("location"));
      ItemPredicate var7 = ItemPredicate.fromJson(param0.get("item"));
      return new PlacedBlockTrigger.TriggerInstance(param1, var4, var5, var6, var7);
   }

   @Nullable
   private static Block deserializeBlock(JsonObject param0) {
      if (param0.has("block")) {
         ResourceLocation var1 = new ResourceLocation(GsonHelper.getAsString(param0, "block"));
         return BuiltInRegistries.BLOCK.getOptional(var1).orElseThrow(() -> new JsonSyntaxException("Unknown block type '" + var1 + "'"));
      } else {
         return null;
      }
   }

   public void trigger(ServerPlayer param0, BlockPos param1, ItemStack param2) {
      BlockState var4 = param0.getLevel().getBlockState(param1);
      this.trigger(param0, l_param4 -> l_param4.matches(var4, param1, param0.getLevel(), param2));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      @Nullable
      private final Block block;
      private final StatePropertiesPredicate state;
      private final LocationPredicate location;
      private final ItemPredicate item;

      public TriggerInstance(
         EntityPredicate.Composite param0, @Nullable Block param1, StatePropertiesPredicate param2, LocationPredicate param3, ItemPredicate param4
      ) {
         super(PlacedBlockTrigger.ID, param0);
         this.block = param1;
         this.state = param2;
         this.location = param3;
         this.item = param4;
      }

      public static PlacedBlockTrigger.TriggerInstance placedBlock(Block param0) {
         return new PlacedBlockTrigger.TriggerInstance(
            EntityPredicate.Composite.ANY, param0, StatePropertiesPredicate.ANY, LocationPredicate.ANY, ItemPredicate.ANY
         );
      }

      public boolean matches(BlockState param0, BlockPos param1, ServerLevel param2, ItemStack param3) {
         if (this.block != null && !param0.is(this.block)) {
            return false;
         } else if (!this.state.matches(param0)) {
            return false;
         } else {
            return !this.location.matches(param2, param1.getX(), param1.getY(), param1.getZ()) ? false : this.item.matches(param3);
         }
      }

      @Override
      public JsonObject serializeToJson(SerializationContext param0) {
         JsonObject var2 = super.serializeToJson(param0);
         if (this.block != null) {
            var2.addProperty("block", BuiltInRegistries.BLOCK.getKey(this.block).toString());
         }

         var2.add("state", this.state.serializeToJson());
         var2.add("location", this.location.serializeToJson());
         var2.add("item", this.item.serializeToJson());
         return var2;
      }
   }
}

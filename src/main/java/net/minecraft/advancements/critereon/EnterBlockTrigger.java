package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EnterBlockTrigger extends SimpleCriterionTrigger<EnterBlockTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("enter_block");

   @Override
   public ResourceLocation getId() {
      return ID;
   }

   public EnterBlockTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      Block var4 = deserializeBlock(param0);
      StatePropertiesPredicate var5 = StatePropertiesPredicate.fromJson(param0.get("state"));
      if (var4 != null) {
         var5.checkState(var4.getStateDefinition(), l_param1 -> {
            throw new JsonSyntaxException("Block " + var4 + " has no property " + l_param1);
         });
      }

      return new EnterBlockTrigger.TriggerInstance(param1, var4, var5);
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

   public void trigger(ServerPlayer param0, BlockState param1) {
      this.trigger(param0, l_param1 -> l_param1.matches(param1));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      @Nullable
      private final Block block;
      private final StatePropertiesPredicate state;

      public TriggerInstance(EntityPredicate.Composite param0, @Nullable Block param1, StatePropertiesPredicate param2) {
         super(EnterBlockTrigger.ID, param0);
         this.block = param1;
         this.state = param2;
      }

      public static EnterBlockTrigger.TriggerInstance entersBlock(Block param0) {
         return new EnterBlockTrigger.TriggerInstance(EntityPredicate.Composite.ANY, param0, StatePropertiesPredicate.ANY);
      }

      @Override
      public JsonObject serializeToJson(SerializationContext param0) {
         JsonObject var2 = super.serializeToJson(param0);
         if (this.block != null) {
            var2.addProperty("block", BuiltInRegistries.BLOCK.getKey(this.block).toString());
         }

         var2.add("state", this.state.serializeToJson());
         return var2;
      }

      public boolean matches(BlockState param0) {
         return this.block != null && !param0.is(this.block) ? false : this.state.matches(param0);
      }
   }
}

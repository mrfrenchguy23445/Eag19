package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BeeNestDestroyedTrigger extends SimpleCriterionTrigger<BeeNestDestroyedTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("bee_nest_destroyed");

   @Override
   public ResourceLocation getId() {
      return ID;
   }

   public BeeNestDestroyedTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      Block var4 = deserializeBlock(param0);
      ItemPredicate var5 = ItemPredicate.fromJson(param0.get("item"));
      MinMaxBounds.Ints var6 = MinMaxBounds.Ints.fromJson(param0.get("num_bees_inside"));
      return new BeeNestDestroyedTrigger.TriggerInstance(param1, var4, var5, var6);
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

   public void trigger(ServerPlayer param0, BlockState param1, ItemStack param2, int param3) {
      this.trigger(param0, l_param3 -> l_param3.matches(param1, param2, param3));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      @Nullable
      private final Block block;
      private final ItemPredicate item;
      private final MinMaxBounds.Ints numBees;

      public TriggerInstance(EntityPredicate.Composite param0, @Nullable Block param1, ItemPredicate param2, MinMaxBounds.Ints param3) {
         super(BeeNestDestroyedTrigger.ID, param0);
         this.block = param1;
         this.item = param2;
         this.numBees = param3;
      }

      public static BeeNestDestroyedTrigger.TriggerInstance destroyedBeeNest(Block param0, ItemPredicate.Builder param1, MinMaxBounds.Ints param2) {
         return new BeeNestDestroyedTrigger.TriggerInstance(EntityPredicate.Composite.ANY, param0, param1.build(), param2);
      }

      public boolean matches(BlockState param0, ItemStack param1, int param2) {
         if (this.block != null && !param0.is(this.block)) {
            return false;
         } else {
            return !this.item.matches(param1) ? false : this.numBees.matches(param2);
         }
      }

      @Override
      public JsonObject serializeToJson(SerializationContext param0) {
         JsonObject var2 = super.serializeToJson(param0);
         if (this.block != null) {
            var2.addProperty("block", BuiltInRegistries.BLOCK.getKey(this.block).toString());
         }

         var2.add("item", this.item.serializeToJson());
         var2.add("num_bees_inside", this.numBees.serializeToJson());
         return var2;
      }
   }
}

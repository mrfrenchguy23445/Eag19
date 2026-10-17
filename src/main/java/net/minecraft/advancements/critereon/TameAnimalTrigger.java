package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class TameAnimalTrigger extends SimpleCriterionTrigger<TameAnimalTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("tame_animal");

   @Override
   public ResourceLocation getId() {
      return ID;
   }

   public TameAnimalTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      EntityPredicate.Composite var4 = EntityPredicate.Composite.fromJson(param0, "entity", param2);
      return new TameAnimalTrigger.TriggerInstance(param1, var4);
   }

   public void trigger(ServerPlayer param0, Animal param1) {
      LootContext var3 = EntityPredicate.createContext(param0, param1);
      this.trigger(param0, l_param1 -> l_param1.matches(var3));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final EntityPredicate.Composite entity;

      public TriggerInstance(EntityPredicate.Composite param0, EntityPredicate.Composite param1) {
         super(TameAnimalTrigger.ID, param0);
         this.entity = param1;
      }

      public static TameAnimalTrigger.TriggerInstance tamedAnimal() {
         return new TameAnimalTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY);
      }

      public static TameAnimalTrigger.TriggerInstance tamedAnimal(EntityPredicate param0) {
         return new TameAnimalTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(param0));
      }

      public boolean matches(LootContext param0) {
         return this.entity.matches(param0);
      }

      @Override
      public JsonObject serializeToJson(SerializationContext param0) {
         JsonObject var2 = super.serializeToJson(param0);
         var2.add("entity", this.entity.toJson(param0));
         return var2;
      }
   }
}

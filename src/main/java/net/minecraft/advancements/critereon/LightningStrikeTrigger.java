package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.storage.loot.LootContext;

public class LightningStrikeTrigger extends SimpleCriterionTrigger<LightningStrikeTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("lightning_strike");

   @Override
   public ResourceLocation getId() {
      return ID;
   }

   public LightningStrikeTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      EntityPredicate.Composite var4 = EntityPredicate.Composite.fromJson(param0, "lightning", param2);
      EntityPredicate.Composite var5 = EntityPredicate.Composite.fromJson(param0, "bystander", param2);
      return new LightningStrikeTrigger.TriggerInstance(param1, var4, var5);
   }

   public void trigger(ServerPlayer param0, LightningBolt param1, List<Entity> param2) {
      List<LootContext> var4 = param2.stream().map(l_param1 -> EntityPredicate.createContext(param0, l_param1)).collect(Collectors.toList());
      LootContext var5 = EntityPredicate.createContext(param0, param1);
      this.trigger(param0, l_param2 -> l_param2.matches(var5, var4));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final EntityPredicate.Composite lightning;
      private final EntityPredicate.Composite bystander;

      public TriggerInstance(EntityPredicate.Composite param0, EntityPredicate.Composite param1, EntityPredicate.Composite param2) {
         super(LightningStrikeTrigger.ID, param0);
         this.lightning = param1;
         this.bystander = param2;
      }

      public static LightningStrikeTrigger.TriggerInstance lighthingStrike(EntityPredicate param0, EntityPredicate param1) {
         return new LightningStrikeTrigger.TriggerInstance(
            EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(param0), EntityPredicate.Composite.wrap(param1)
         );
      }

      public boolean matches(LootContext param0, List<LootContext> param1) {
         return !this.lightning.matches(param0)
            ? false
            : this.bystander == EntityPredicate.Composite.ANY || !param1.stream().noneMatch(this.bystander::matches);
      }

      @Override
      public JsonObject serializeToJson(SerializationContext param0) {
         JsonObject var2 = super.serializeToJson(param0);
         var2.add("lightning", this.lightning.toJson(param0));
         var2.add("bystander", this.bystander.toJson(param0));
         return var2;
      }
   }
}

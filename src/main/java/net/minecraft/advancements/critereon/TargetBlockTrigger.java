package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;

public class TargetBlockTrigger extends SimpleCriterionTrigger<TargetBlockTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("target_hit");

   @Override
   public ResourceLocation getId() {
      return ID;
   }

   public TargetBlockTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      MinMaxBounds.Ints var4 = MinMaxBounds.Ints.fromJson(param0.get("signal_strength"));
      EntityPredicate.Composite var5 = EntityPredicate.Composite.fromJson(param0, "projectile", param2);
      return new TargetBlockTrigger.TriggerInstance(param1, var4, var5);
   }

   public void trigger(ServerPlayer param0, Entity param1, Vec3 param2, int param3) {
      LootContext var5 = EntityPredicate.createContext(param0, param1);
      this.trigger(param0, l_param3 -> l_param3.matches(var5, param2, param3));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final MinMaxBounds.Ints signalStrength;
      private final EntityPredicate.Composite projectile;

      public TriggerInstance(EntityPredicate.Composite param0, MinMaxBounds.Ints param1, EntityPredicate.Composite param2) {
         super(TargetBlockTrigger.ID, param0);
         this.signalStrength = param1;
         this.projectile = param2;
      }

      public static TargetBlockTrigger.TriggerInstance targetHit(MinMaxBounds.Ints param0, EntityPredicate.Composite param1) {
         return new TargetBlockTrigger.TriggerInstance(EntityPredicate.Composite.ANY, param0, param1);
      }

      @Override
      public JsonObject serializeToJson(SerializationContext param0) {
         JsonObject var2 = super.serializeToJson(param0);
         var2.add("signal_strength", this.signalStrength.serializeToJson());
         var2.add("projectile", this.projectile.toJson(param0));
         return var2;
      }

      public boolean matches(LootContext param0, Vec3 param1, int param2) {
         return !this.signalStrength.matches(param2) ? false : this.projectile.matches(param0);
      }
   }
}

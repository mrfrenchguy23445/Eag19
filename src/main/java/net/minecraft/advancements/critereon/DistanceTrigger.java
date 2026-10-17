package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class DistanceTrigger extends SimpleCriterionTrigger<DistanceTrigger.TriggerInstance> {
   final ResourceLocation id;

   public DistanceTrigger(ResourceLocation param0) {
      this.id = param0;
   }

   @Override
   public ResourceLocation getId() {
      return this.id;
   }

   public DistanceTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      LocationPredicate var4 = LocationPredicate.fromJson(param0.get("start_position"));
      DistancePredicate var5 = DistancePredicate.fromJson(param0.get("distance"));
      return new DistanceTrigger.TriggerInstance(this.id, param1, var4, var5);
   }

   public void trigger(ServerPlayer param0, Vec3 param1) {
      Vec3 var3 = param0.position();
      this.trigger(param0, l_param3 -> l_param3.matches(param0.getLevel(), param1, var3));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final LocationPredicate startPosition;
      private final DistancePredicate distance;

      public TriggerInstance(ResourceLocation param0, EntityPredicate.Composite param1, LocationPredicate param2, DistancePredicate param3) {
         super(param0, param1);
         this.startPosition = param2;
         this.distance = param3;
      }

      public static DistanceTrigger.TriggerInstance fallFromHeight(EntityPredicate.Builder param0, DistancePredicate param1, LocationPredicate param2) {
         return new DistanceTrigger.TriggerInstance(CriteriaTriggers.FALL_FROM_HEIGHT.id, EntityPredicate.Composite.wrap(param0.build()), param2, param1);
      }

      public static DistanceTrigger.TriggerInstance rideEntityInLava(EntityPredicate.Builder param0, DistancePredicate param1) {
         return new DistanceTrigger.TriggerInstance(
            CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.id, EntityPredicate.Composite.wrap(param0.build()), LocationPredicate.ANY, param1
         );
      }

      public static DistanceTrigger.TriggerInstance travelledThroughNether(DistancePredicate param0) {
         return new DistanceTrigger.TriggerInstance(CriteriaTriggers.NETHER_TRAVEL.id, EntityPredicate.Composite.ANY, LocationPredicate.ANY, param0);
      }

      @Override
      public JsonObject serializeToJson(SerializationContext param0) {
         JsonObject var2 = super.serializeToJson(param0);
         var2.add("start_position", this.startPosition.serializeToJson());
         var2.add("distance", this.distance.serializeToJson());
         return var2;
      }

      public boolean matches(ServerLevel param0, Vec3 param1, Vec3 param2) {
         return !this.startPosition.matches(param0, param1.x, param1.y, param1.z)
            ? false
            : this.distance.matches(param1.x, param1.y, param1.z, param2.x, param2.y, param2.z);
      }
   }
}

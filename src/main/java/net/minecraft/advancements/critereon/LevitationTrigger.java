package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class LevitationTrigger extends SimpleCriterionTrigger<LevitationTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("levitation");

   @Override
   public ResourceLocation getId() {
      return ID;
   }

   public LevitationTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      DistancePredicate var4 = DistancePredicate.fromJson(param0.get("distance"));
      MinMaxBounds.Ints var5 = MinMaxBounds.Ints.fromJson(param0.get("duration"));
      return new LevitationTrigger.TriggerInstance(param1, var4, var5);
   }

   public void trigger(ServerPlayer param0, Vec3 param1, int param2) {
      this.trigger(param0, l_param3 -> l_param3.matches(param0, param1, param2));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final DistancePredicate distance;
      private final MinMaxBounds.Ints duration;

      public TriggerInstance(EntityPredicate.Composite param0, DistancePredicate param1, MinMaxBounds.Ints param2) {
         super(LevitationTrigger.ID, param0);
         this.distance = param1;
         this.duration = param2;
      }

      public static LevitationTrigger.TriggerInstance levitated(DistancePredicate param0) {
         return new LevitationTrigger.TriggerInstance(EntityPredicate.Composite.ANY, param0, MinMaxBounds.Ints.ANY);
      }

      public boolean matches(ServerPlayer param0, Vec3 param1, int param2) {
         return !this.distance.matches(param1.x, param1.y, param1.z, param0.getX(), param0.getY(), param0.getZ()) ? false : this.duration.matches(param2);
      }

      @Override
      public JsonObject serializeToJson(SerializationContext param0) {
         JsonObject var2 = super.serializeToJson(param0);
         var2.add("distance", this.distance.serializeToJson());
         var2.add("duration", this.duration.serializeToJson());
         return var2;
      }
   }
}

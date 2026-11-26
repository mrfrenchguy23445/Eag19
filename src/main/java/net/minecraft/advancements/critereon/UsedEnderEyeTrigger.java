package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class UsedEnderEyeTrigger extends SimpleCriterionTrigger<UsedEnderEyeTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("used_ender_eye");

   @Override
   public ResourceLocation getId() {
      return ID;
   }

   public UsedEnderEyeTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      MinMaxBounds.Doubles var4 = MinMaxBounds.Doubles.fromJson(param0.get("distance"));
      return new UsedEnderEyeTrigger.TriggerInstance(param1, var4);
   }

   public void trigger(ServerPlayer param0, BlockPos param1) {
      double var3 = param0.getX() - param1.getX();
      double var5 = param0.getZ() - param1.getZ();
      double var7 = var3 * var3 + var5 * var5;
      this.trigger(param0, l_param2 -> l_param2.matches(var7));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final MinMaxBounds.Doubles level;

      public TriggerInstance(EntityPredicate.Composite param0, MinMaxBounds.Doubles param1) {
         super(UsedEnderEyeTrigger.ID, param0);
         this.level = param1;
      }

      public boolean matches(double param0) {
         return this.level.matchesSqr(param0);
      }
   }
}

package net.minecraft.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;

public interface CriterionTrigger<T extends CriterionTriggerInstance> {
   ResourceLocation getId();

   void addPlayerListener(PlayerAdvancements var1, CriterionTrigger.Listener<T> var2);

   void removePlayerListener(PlayerAdvancements var1, CriterionTrigger.Listener<T> var2);

   void removePlayerListeners(PlayerAdvancements var1);

   T createInstance(JsonObject var1, DeserializationContext var2);

   class Listener<T extends CriterionTriggerInstance> {
      private final T trigger;
      private final Advancement advancement;
      private final String criterion;

      public Listener(T param0, Advancement param1, String param2) {
         this.trigger = param0;
         this.advancement = param1;
         this.criterion = param2;
      }

      public T getTriggerInstance() {
         return this.trigger;
      }

      public void run(PlayerAdvancements param0) {
         param0.award(this.advancement, this.criterion);
      }

      @Override
      public boolean equals(Object param0) {
         if (this == param0) {
            return true;
         }

         if (param0 != null && this.getClass() == param0.getClass()) {
            CriterionTrigger.Listener<?> var2 = (CriterionTrigger.Listener<?>)param0;
            if (!this.trigger.equals(var2.trigger)) {
               return false;
            } else {
               return !this.advancement.equals(var2.advancement) ? false : this.criterion.equals(var2.criterion);
            }
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int var1 = this.trigger.hashCode();
         var1 = 31 * var1 + this.advancement.hashCode();
         return 31 * var1 + this.criterion.hashCode();
      }
   }
}

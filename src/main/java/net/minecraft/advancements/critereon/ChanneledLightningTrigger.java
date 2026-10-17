package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class ChanneledLightningTrigger extends SimpleCriterionTrigger<ChanneledLightningTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("channeled_lightning");

   @Override
   public ResourceLocation getId() {
      return ID;
   }

   public ChanneledLightningTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      EntityPredicate.Composite[] var4 = EntityPredicate.Composite.fromJsonArray(param0, "victims", param2);
      return new ChanneledLightningTrigger.TriggerInstance(param1, var4);
   }

   public void trigger(ServerPlayer param0, Collection<? extends Entity> param1) {
      List<LootContext> var3 = param1.stream().map(l_param1 -> EntityPredicate.createContext(param0, l_param1)).collect(Collectors.toList());
      this.trigger(param0, l_param1 -> l_param1.matches(var3));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final EntityPredicate.Composite[] victims;

      public TriggerInstance(EntityPredicate.Composite param0, EntityPredicate.Composite[] param1) {
         super(ChanneledLightningTrigger.ID, param0);
         this.victims = param1;
      }

      public static ChanneledLightningTrigger.TriggerInstance channeledLightning(EntityPredicate... param0) {
         return new ChanneledLightningTrigger.TriggerInstance(
            EntityPredicate.Composite.ANY, Stream.of(param0).map(EntityPredicate.Composite::wrap).toArray(EntityPredicate.Composite[]::new)
         );
      }

      public boolean matches(Collection<? extends LootContext> param0) {
         for (EntityPredicate.Composite var5 : this.victims) {
            boolean var6 = false;

            for (LootContext var8 : param0) {
               if (var5.matches(var8)) {
                  var6 = true;
                  break;
               }
            }

            if (!var6) {
               return false;
            }
         }

         return true;
      }

      @Override
      public JsonObject serializeToJson(SerializationContext param0) {
         JsonObject var2 = super.serializeToJson(param0);
         var2.add("victims", EntityPredicate.Composite.toJson(this.victims, param0));
         return var2;
      }
   }
}

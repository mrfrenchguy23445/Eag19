package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootContext;

public class KilledByCrossbowTrigger extends SimpleCriterionTrigger<KilledByCrossbowTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("killed_by_crossbow");

   @Override
   public ResourceLocation getId() {
      return ID;
   }

   public KilledByCrossbowTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      EntityPredicate.Composite[] var4 = EntityPredicate.Composite.fromJsonArray(param0, "victims", param2);
      MinMaxBounds.Ints var5 = MinMaxBounds.Ints.fromJson(param0.get("unique_entity_types"));
      return new KilledByCrossbowTrigger.TriggerInstance(param1, var4, var5);
   }

   public void trigger(ServerPlayer param0, Collection<Entity> param1) {
      List<LootContext> var3 = Lists.newArrayList();
      Set<EntityType<?>> var4 = Sets.newHashSet();

      for (Entity var6 : param1) {
         var4.add(var6.getType());
         var3.add(EntityPredicate.createContext(param0, var6));
      }

      this.trigger(param0, l_param2 -> l_param2.matches(var3, var4.size()));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final EntityPredicate.Composite[] victims;
      private final MinMaxBounds.Ints uniqueEntityTypes;

      public TriggerInstance(EntityPredicate.Composite param0, EntityPredicate.Composite[] param1, MinMaxBounds.Ints param2) {
         super(KilledByCrossbowTrigger.ID, param0);
         this.victims = param1;
         this.uniqueEntityTypes = param2;
      }

      public static KilledByCrossbowTrigger.TriggerInstance crossbowKilled(EntityPredicate.Builder... param0) {
         EntityPredicate.Composite[] var1 = new EntityPredicate.Composite[param0.length];

         for (int var2 = 0; var2 < param0.length; var2++) {
            EntityPredicate.Builder var3 = param0[var2];
            var1[var2] = EntityPredicate.Composite.wrap(var3.build());
         }

         return new KilledByCrossbowTrigger.TriggerInstance(EntityPredicate.Composite.ANY, var1, MinMaxBounds.Ints.ANY);
      }

      public static KilledByCrossbowTrigger.TriggerInstance crossbowKilled(MinMaxBounds.Ints param0) {
         EntityPredicate.Composite[] var1 = new EntityPredicate.Composite[0];
         return new KilledByCrossbowTrigger.TriggerInstance(EntityPredicate.Composite.ANY, var1, param0);
      }

      public boolean matches(Collection<LootContext> param0, int param1) {
         if (this.victims.length > 0) {
            List<LootContext> var3 = Lists.newArrayList(param0);

            for (EntityPredicate.Composite var7 : this.victims) {
               boolean var8 = false;
               Iterator<LootContext> var9 = var3.iterator();

               while (var9.hasNext()) {
                  LootContext var10 = var9.next();
                  if (var7.matches(var10)) {
                     var9.remove();
                     var8 = true;
                     break;
                  }
               }

               if (!var8) {
                  return false;
               }
            }
         }

         return this.uniqueEntityTypes.matches(param1);
      }

      @Override
      public JsonObject serializeToJson(SerializationContext param0) {
         JsonObject var2 = super.serializeToJson(param0);
         var2.add("victims", EntityPredicate.Composite.toJson(this.victims, param0));
         var2.add("unique_entity_types", this.uniqueEntityTypes.serializeToJson());
         return var2;
      }
   }
}

package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class BredAnimalsTrigger extends SimpleCriterionTrigger<BredAnimalsTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("bred_animals");

   @Override
   public ResourceLocation getId() {
      return ID;
   }

   public BredAnimalsTrigger.TriggerInstance createInstance(JsonObject param0, EntityPredicate.Composite param1, DeserializationContext param2) {
      EntityPredicate.Composite var4 = EntityPredicate.Composite.fromJson(param0, "parent", param2);
      EntityPredicate.Composite var5 = EntityPredicate.Composite.fromJson(param0, "partner", param2);
      EntityPredicate.Composite var6 = EntityPredicate.Composite.fromJson(param0, "child", param2);
      return new BredAnimalsTrigger.TriggerInstance(param1, var4, var5, var6);
   }

   public void trigger(ServerPlayer param0, Animal param1, Animal param2, @Nullable AgeableMob param3) {
      LootContext var5 = EntityPredicate.createContext(param0, param1);
      LootContext var6 = EntityPredicate.createContext(param0, param2);
      LootContext var7 = param3 != null ? EntityPredicate.createContext(param0, param3) : null;
      this.trigger(param0, l_param3 -> l_param3.matches(var5, var6, var7));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final EntityPredicate.Composite parent;
      private final EntityPredicate.Composite partner;
      private final EntityPredicate.Composite child;

      public TriggerInstance(
         EntityPredicate.Composite param0, EntityPredicate.Composite param1, EntityPredicate.Composite param2, EntityPredicate.Composite param3
      ) {
         super(BredAnimalsTrigger.ID, param0);
         this.parent = param1;
         this.partner = param2;
         this.child = param3;
      }

      public static BredAnimalsTrigger.TriggerInstance bredAnimals() {
         return new BredAnimalsTrigger.TriggerInstance(
            EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY
         );
      }

      public static BredAnimalsTrigger.TriggerInstance bredAnimals(EntityPredicate.Builder param0) {
         return new BredAnimalsTrigger.TriggerInstance(
            EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(param0.build())
         );
      }

      public static BredAnimalsTrigger.TriggerInstance bredAnimals(EntityPredicate param0, EntityPredicate param1, EntityPredicate param2) {
         return new BredAnimalsTrigger.TriggerInstance(
            EntityPredicate.Composite.ANY,
            EntityPredicate.Composite.wrap(param0),
            EntityPredicate.Composite.wrap(param1),
            EntityPredicate.Composite.wrap(param2)
         );
      }

      public boolean matches(LootContext param0, LootContext param1, @Nullable LootContext param2) {
         return this.child == EntityPredicate.Composite.ANY || param2 != null && this.child.matches(param2)
            ? this.parent.matches(param0) && this.partner.matches(param1) || this.parent.matches(param1) && this.partner.matches(param0)
            : false;
      }

      @Override
      public JsonObject serializeToJson(SerializationContext param0) {
         JsonObject var2 = super.serializeToJson(param0);
         var2.add("parent", this.parent.toJson(param0));
         var2.add("partner", this.partner.toJson(param0));
         var2.add("child", this.child.toJson(param0));
         return var2;
      }
   }
}

package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;

public class DamagePredicate {
   public static final DamagePredicate ANY = DamagePredicate.Builder.damageInstance().build();
   private final MinMaxBounds.Doubles dealtDamage;
   private final MinMaxBounds.Doubles takenDamage;
   private final EntityPredicate sourceEntity;
   @Nullable
   private final Boolean blocked;
   private final DamageSourcePredicate type;

   public DamagePredicate() {
      this.dealtDamage = MinMaxBounds.Doubles.ANY;
      this.takenDamage = MinMaxBounds.Doubles.ANY;
      this.sourceEntity = EntityPredicate.ANY;
      this.blocked = null;
      this.type = DamageSourcePredicate.ANY;
   }

   public DamagePredicate(
      MinMaxBounds.Doubles param0, MinMaxBounds.Doubles param1, EntityPredicate param2, @Nullable Boolean param3, DamageSourcePredicate param4
   ) {
      this.dealtDamage = param0;
      this.takenDamage = param1;
      this.sourceEntity = param2;
      this.blocked = param3;
      this.type = param4;
   }

   public boolean matches(ServerPlayer param0, DamageSource param1, float param2, float param3, boolean param4) {
      if (this == ANY) {
         return true;
      } else if (!this.dealtDamage.matches(param2)) {
         return false;
      } else if (!this.takenDamage.matches(param3)) {
         return false;
      } else if (!this.sourceEntity.matches(param0, param1.getEntity())) {
         return false;
      } else {
         return this.blocked != null && this.blocked != param4 ? false : this.type.matches(param0, param1);
      }
   }

   public static DamagePredicate fromJson(@Nullable JsonElement param0) {
      if (param0 != null && !param0.isJsonNull()) {
         JsonObject var1 = GsonHelper.convertToJsonObject(param0, "damage");
         MinMaxBounds.Doubles var2 = MinMaxBounds.Doubles.fromJson(var1.get("dealt"));
         MinMaxBounds.Doubles var3 = MinMaxBounds.Doubles.fromJson(var1.get("taken"));
         Boolean var4 = var1.has("blocked") ? GsonHelper.getAsBoolean(var1, "blocked") : null;
         EntityPredicate var5 = EntityPredicate.fromJson(var1.get("source_entity"));
         DamageSourcePredicate var6 = DamageSourcePredicate.fromJson(var1.get("type"));
         return new DamagePredicate(var2, var3, var5, var4, var6);
      } else {
         return ANY;
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      }

      JsonObject var1 = new JsonObject();
      var1.add("dealt", this.dealtDamage.serializeToJson());
      var1.add("taken", this.takenDamage.serializeToJson());
      var1.add("source_entity", this.sourceEntity.serializeToJson());
      var1.add("type", this.type.serializeToJson());
      if (this.blocked != null) {
         var1.addProperty("blocked", this.blocked);
      }

      return var1;
   }

   public static class Builder {
      private MinMaxBounds.Doubles dealtDamage = MinMaxBounds.Doubles.ANY;
      private MinMaxBounds.Doubles takenDamage = MinMaxBounds.Doubles.ANY;
      private EntityPredicate sourceEntity = EntityPredicate.ANY;
      @Nullable
      private Boolean blocked;
      private DamageSourcePredicate type = DamageSourcePredicate.ANY;

      public static DamagePredicate.Builder damageInstance() {
         return new DamagePredicate.Builder();
      }

      public DamagePredicate.Builder dealtDamage(MinMaxBounds.Doubles param0) {
         this.dealtDamage = param0;
         return this;
      }

      public DamagePredicate.Builder takenDamage(MinMaxBounds.Doubles param0) {
         this.takenDamage = param0;
         return this;
      }

      public DamagePredicate.Builder sourceEntity(EntityPredicate param0) {
         this.sourceEntity = param0;
         return this;
      }

      public DamagePredicate.Builder blocked(Boolean param0) {
         this.blocked = param0;
         return this;
      }

      public DamagePredicate.Builder type(DamageSourcePredicate param0) {
         this.type = param0;
         return this;
      }

      public DamagePredicate.Builder type(DamageSourcePredicate.Builder param0) {
         this.type = param0.build();
         return this;
      }

      public DamagePredicate build() {
         return new DamagePredicate(this.dealtDamage, this.takenDamage, this.sourceEntity, this.blocked, this.type);
      }
   }
}

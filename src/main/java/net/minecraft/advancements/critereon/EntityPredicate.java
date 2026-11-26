package net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

public class EntityPredicate {
   public static final EntityPredicate ANY = new EntityPredicate(
      EntityTypePredicate.ANY,
      DistancePredicate.ANY,
      LocationPredicate.ANY,
      LocationPredicate.ANY,
      MobEffectsPredicate.ANY,
      NbtPredicate.ANY,
      EntityFlagsPredicate.ANY,
      EntityEquipmentPredicate.ANY,
      EntitySubPredicate.ANY,
      null
   );
   private final EntityTypePredicate entityType;
   private final DistancePredicate distanceToPlayer;
   private final LocationPredicate location;
   private final LocationPredicate steppingOnLocation;
   private final MobEffectsPredicate effects;
   private final NbtPredicate nbt;
   private final EntityFlagsPredicate flags;
   private final EntityEquipmentPredicate equipment;
   private final EntitySubPredicate subPredicate;
   private final EntityPredicate vehicle;
   private final EntityPredicate passenger;
   private final EntityPredicate targetedEntity;
   @Nullable
   private final String team;

   private EntityPredicate(
      EntityTypePredicate param0,
      DistancePredicate param1,
      LocationPredicate param2,
      LocationPredicate param3,
      MobEffectsPredicate param4,
      NbtPredicate param5,
      EntityFlagsPredicate param6,
      EntityEquipmentPredicate param7,
      EntitySubPredicate param8,
      @Nullable String param9
   ) {
      this.entityType = param0;
      this.distanceToPlayer = param1;
      this.location = param2;
      this.steppingOnLocation = param3;
      this.effects = param4;
      this.nbt = param5;
      this.flags = param6;
      this.equipment = param7;
      this.subPredicate = param8;
      this.passenger = this;
      this.vehicle = this;
      this.targetedEntity = this;
      this.team = param9;
   }

   EntityPredicate(
      EntityTypePredicate param0,
      DistancePredicate param1,
      LocationPredicate param2,
      LocationPredicate param3,
      MobEffectsPredicate param4,
      NbtPredicate param5,
      EntityFlagsPredicate param6,
      EntityEquipmentPredicate param7,
      EntitySubPredicate param8,
      EntityPredicate param9,
      EntityPredicate param10,
      EntityPredicate param11,
      @Nullable String param12
   ) {
      this.entityType = param0;
      this.distanceToPlayer = param1;
      this.location = param2;
      this.steppingOnLocation = param3;
      this.effects = param4;
      this.nbt = param5;
      this.flags = param6;
      this.equipment = param7;
      this.subPredicate = param8;
      this.vehicle = param9;
      this.passenger = param10;
      this.targetedEntity = param11;
      this.team = param12;
   }

   public boolean matches(ServerPlayer param0, @Nullable Entity param1) {
      return this.matches(param0.getLevel(), param0.position(), param1);
   }

   public boolean matches(ServerLevel param0, @Nullable Vec3 param1, @Nullable Entity param2) {
      if (this == ANY) {
         return true;
      }

      if (param2 == null) {
         return false;
      }

      if (!this.entityType.matches(param2.getType())) {
         return false;
      }

      if (param1 == null) {
         if (this.distanceToPlayer != DistancePredicate.ANY) {
            return false;
         }
      } else if (!this.distanceToPlayer.matches(param1.x, param1.y, param1.z, param2.getX(), param2.getY(), param2.getZ())) {
         return false;
      }

      if (!this.location.matches(param0, param2.getX(), param2.getY(), param2.getZ())) {
         return false;
      }

      if (this.steppingOnLocation != LocationPredicate.ANY) {
         Vec3 var4 = Vec3.atCenterOf(param2.getOnPosLegacy());
         if (!this.steppingOnLocation.matches(param0, var4.x(), var4.y(), var4.z())) {
            return false;
         }
      }

      if (!this.effects.matches(param2)) {
         return false;
      }

      if (!this.nbt.matches(param2)) {
         return false;
      }

      if (!this.flags.matches(param2)) {
         return false;
      }

      if (!this.equipment.matches(param2)) {
         return false;
      }

      if (!this.subPredicate.matches(param2, param0, param1)) {
         return false;
      }

      if (!this.vehicle.matches(param0, param1, param2.getVehicle())) {
         return false;
      }

      if (this.passenger != ANY && param2.getPassengers().stream().noneMatch(l_param2 -> this.passenger.matches(param0, param1, l_param2))) {
         return false;
      }

      if (!this.targetedEntity.matches(param0, param1, param2 instanceof Mob ? ((Mob)param2).getTarget() : null)) {
         return false;
      }

      if (this.team != null) {
         Team var4 = param2.getTeam();
         if (var4 == null || !this.team.equals(var4.getName())) {
            return false;
         }
      }

      return true;
   }

   public static EntityPredicate fromJson(@Nullable JsonElement param0) {
      if (param0 != null && !param0.isJsonNull()) {
         JsonObject var1 = GsonHelper.convertToJsonObject(param0, "entity");
         EntityTypePredicate var2 = EntityTypePredicate.fromJson(var1.get("type"));
         DistancePredicate var3 = DistancePredicate.fromJson(var1.get("distance"));
         LocationPredicate var4 = LocationPredicate.fromJson(var1.get("location"));
         LocationPredicate var5 = LocationPredicate.fromJson(var1.get("stepping_on"));
         MobEffectsPredicate var6 = MobEffectsPredicate.fromJson(var1.get("effects"));
         NbtPredicate var7 = NbtPredicate.fromJson(var1.get("nbt"));
         EntityFlagsPredicate var8 = EntityFlagsPredicate.fromJson(var1.get("flags"));
         EntityEquipmentPredicate var9 = EntityEquipmentPredicate.fromJson(var1.get("equipment"));
         EntitySubPredicate var10 = EntitySubPredicate.fromJson(var1.get("type_specific"));
         EntityPredicate var11 = fromJson(var1.get("vehicle"));
         EntityPredicate var12 = fromJson(var1.get("passenger"));
         EntityPredicate var13 = fromJson(var1.get("targeted_entity"));
         String var14 = GsonHelper.getAsString(var1, "team", null);
         return new EntityPredicate.Builder()
            .entityType(var2)
            .distance(var3)
            .located(var4)
            .steppingOn(var5)
            .effects(var6)
            .nbt(var7)
            .flags(var8)
            .equipment(var9)
            .subPredicate(var10)
            .team(var14)
            .vehicle(var11)
            .passenger(var12)
            .targetedEntity(var13)
            .build();
      } else {
         return ANY;
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      }

      JsonObject var1 = new JsonObject();
      var1.add("type", this.entityType.serializeToJson());
      var1.add("distance", this.distanceToPlayer.serializeToJson());
      var1.add("location", this.location.serializeToJson());
      var1.add("stepping_on", this.steppingOnLocation.serializeToJson());
      var1.add("effects", this.effects.serializeToJson());
      var1.add("nbt", this.nbt.serializeToJson());
      var1.add("flags", this.flags.serializeToJson());
      var1.add("equipment", this.equipment.serializeToJson());
      var1.add("type_specific", this.subPredicate.serialize());
      var1.add("vehicle", this.vehicle.serializeToJson());
      var1.add("passenger", this.passenger.serializeToJson());
      var1.add("targeted_entity", this.targetedEntity.serializeToJson());
      var1.addProperty("team", this.team);
      return var1;
   }

   public static LootContext createContext(ServerPlayer param0, Entity param1) {
      return new LootContext.Builder(param0.getLevel())
         .withParameter(LootContextParams.THIS_ENTITY, param1)
         .withParameter(LootContextParams.ORIGIN, param0.position())
         .withRandom(param0.getRandom())
         .create(LootContextParamSets.ADVANCEMENT_ENTITY);
   }

   public static class Builder {
      private EntityTypePredicate entityType = EntityTypePredicate.ANY;
      private DistancePredicate distanceToPlayer = DistancePredicate.ANY;
      private LocationPredicate location = LocationPredicate.ANY;
      private LocationPredicate steppingOnLocation = LocationPredicate.ANY;
      private MobEffectsPredicate effects = MobEffectsPredicate.ANY;
      private NbtPredicate nbt = NbtPredicate.ANY;
      private EntityFlagsPredicate flags = EntityFlagsPredicate.ANY;
      private EntityEquipmentPredicate equipment = EntityEquipmentPredicate.ANY;
      private EntitySubPredicate subPredicate = EntitySubPredicate.ANY;
      private EntityPredicate vehicle = EntityPredicate.ANY;
      private EntityPredicate passenger = EntityPredicate.ANY;
      private EntityPredicate targetedEntity = EntityPredicate.ANY;
      @Nullable
      private String team;

      public static EntityPredicate.Builder entity() {
         return new EntityPredicate.Builder();
      }

      public EntityPredicate.Builder of(EntityType<?> param0) {
         this.entityType = EntityTypePredicate.of(param0);
         return this;
      }

      public EntityPredicate.Builder of(TagKey<EntityType<?>> param0) {
         this.entityType = EntityTypePredicate.of(param0);
         return this;
      }

      public EntityPredicate.Builder entityType(EntityTypePredicate param0) {
         this.entityType = param0;
         return this;
      }

      public EntityPredicate.Builder distance(DistancePredicate param0) {
         this.distanceToPlayer = param0;
         return this;
      }

      public EntityPredicate.Builder located(LocationPredicate param0) {
         this.location = param0;
         return this;
      }

      public EntityPredicate.Builder steppingOn(LocationPredicate param0) {
         this.steppingOnLocation = param0;
         return this;
      }

      public EntityPredicate.Builder effects(MobEffectsPredicate param0) {
         this.effects = param0;
         return this;
      }

      public EntityPredicate.Builder nbt(NbtPredicate param0) {
         this.nbt = param0;
         return this;
      }

      public EntityPredicate.Builder flags(EntityFlagsPredicate param0) {
         this.flags = param0;
         return this;
      }

      public EntityPredicate.Builder equipment(EntityEquipmentPredicate param0) {
         this.equipment = param0;
         return this;
      }

      public EntityPredicate.Builder subPredicate(EntitySubPredicate param0) {
         this.subPredicate = param0;
         return this;
      }

      public EntityPredicate.Builder vehicle(EntityPredicate param0) {
         this.vehicle = param0;
         return this;
      }

      public EntityPredicate.Builder passenger(EntityPredicate param0) {
         this.passenger = param0;
         return this;
      }

      public EntityPredicate.Builder targetedEntity(EntityPredicate param0) {
         this.targetedEntity = param0;
         return this;
      }

      public EntityPredicate.Builder team(@Nullable String param0) {
         this.team = param0;
         return this;
      }

      public EntityPredicate build() {
         return new EntityPredicate(
            this.entityType,
            this.distanceToPlayer,
            this.location,
            this.steppingOnLocation,
            this.effects,
            this.nbt,
            this.flags,
            this.equipment,
            this.subPredicate,
            this.vehicle,
            this.passenger,
            this.targetedEntity,
            this.team
         );
      }
   }

   public static class Composite {
      public static final EntityPredicate.Composite ANY = new EntityPredicate.Composite(new LootItemCondition[0]);
      private final LootItemCondition[] conditions;
      private final Predicate<LootContext> compositePredicates;

      private Composite(LootItemCondition[] param0) {
         this.conditions = param0;
         this.compositePredicates = LootItemConditions.andConditions(param0);
      }

      public static EntityPredicate.Composite create(LootItemCondition... param0) {
         return new EntityPredicate.Composite(param0);
      }

      public static EntityPredicate.Composite fromJson(JsonObject param0, String param1, DeserializationContext param2) {
         JsonElement var3 = param0.get(param1);
         return fromElement(param1, param2, var3);
      }

      public static EntityPredicate.Composite[] fromJsonArray(JsonObject param0, String param1, DeserializationContext param2) {
         JsonElement var3 = param0.get(param1);
         if (var3 != null && !var3.isJsonNull()) {
            JsonArray var4 = GsonHelper.convertToJsonArray(var3, param1);
            EntityPredicate.Composite[] var5 = new EntityPredicate.Composite[var4.size()];

            for (int var6 = 0; var6 < var4.size(); var6++) {
               var5[var6] = fromElement(param1 + "[" + var6 + "]", param2, var4.get(var6));
            }

            return var5;
         } else {
            return new EntityPredicate.Composite[0];
         }
      }

      private static EntityPredicate.Composite fromElement(String param0, DeserializationContext param1, @Nullable JsonElement param2) {
         if (param2 != null && param2.isJsonArray()) {
            LootItemCondition[] var3 = param1.deserializeConditions(
               param2.getAsJsonArray(), param1.getAdvancementId() + "/" + param0, LootContextParamSets.ADVANCEMENT_ENTITY
            );
            return new EntityPredicate.Composite(var3);
         } else {
            EntityPredicate var3 = EntityPredicate.fromJson(param2);
            return wrap(var3);
         }
      }

      public static EntityPredicate.Composite wrap(EntityPredicate param0) {
         if (param0 == EntityPredicate.ANY) {
            return ANY;
         }

         LootItemCondition var1 = LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, param0).build();
         return new EntityPredicate.Composite(new LootItemCondition[]{var1});
      }

      public boolean matches(LootContext param0) {
         return this.compositePredicates.test(param0);
      }

      public JsonElement toJson(SerializationContext param0) {
         return this.conditions.length == 0 ? JsonNull.INSTANCE : param0.serializeConditions(this.conditions);
      }

      public static JsonElement toJson(EntityPredicate.Composite[] param0, SerializationContext param1) {
         if (param0.length == 0) {
            return JsonNull.INSTANCE;
         }

         JsonArray var2 = new JsonArray();

         for (EntityPredicate.Composite var6 : param0) {
            var2.add(var6.toJson(param1));
         }

         return var2;
      }
   }
}

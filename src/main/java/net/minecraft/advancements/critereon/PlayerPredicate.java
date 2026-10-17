package net.minecraft.advancements.critereon;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PlayerPredicate implements EntitySubPredicate {
   public static final int LOOKING_AT_RANGE = 100;
   private final MinMaxBounds.Ints level;
   @Nullable
   private final GameType gameType;
   private final Map<Stat<?>, MinMaxBounds.Ints> stats;
   private final Object2BooleanMap<ResourceLocation> recipes;
   private final Map<ResourceLocation, PlayerPredicate.AdvancementPredicate> advancements;
   private final EntityPredicate lookingAt;

   private static PlayerPredicate.AdvancementPredicate advancementPredicateFromJson(JsonElement param0) {
      if (param0.isJsonPrimitive()) {
         boolean var1 = param0.getAsBoolean();
         return new PlayerPredicate.AdvancementDonePredicate(var1);
      } else {
         Object2BooleanMap<String> var1 = new Object2BooleanOpenHashMap<>();
         JsonObject var2 = GsonHelper.convertToJsonObject(param0, "criterion data");
         var2.entrySet().forEach(l_param1 -> {
            boolean l_var2 = GsonHelper.convertToBoolean(l_param1.getValue(), "criterion test");
            var1.put(l_param1.getKey(), l_var2);
         });
         return new PlayerPredicate.AdvancementCriterionsPredicate(var1);
      }
   }

   PlayerPredicate(
      MinMaxBounds.Ints param0,
      @Nullable GameType param1,
      Map<Stat<?>, MinMaxBounds.Ints> param2,
      Object2BooleanMap<ResourceLocation> param3,
      Map<ResourceLocation, PlayerPredicate.AdvancementPredicate> param4,
      EntityPredicate param5
   ) {
      this.level = param0;
      this.gameType = param1;
      this.stats = param2;
      this.recipes = param3;
      this.advancements = param4;
      this.lookingAt = param5;
   }

   @Override
   public boolean matches(Entity param0, ServerLevel param1, @Nullable Vec3 param2) {
      if (!(param0 instanceof ServerPlayer var4)) {
         return false;
      } else {
         if (!this.level.matches(var4.experienceLevel)) {
            return false;
         }

         if (this.gameType != null && this.gameType != var4.gameMode.getGameModeForPlayer()) {
            return false;
         }

         StatsCounter var5 = var4.getStats();

         for (Entry<Stat<?>, MinMaxBounds.Ints> var7 : this.stats.entrySet()) {
            int var8 = var5.getValue(var7.getKey());
            if (!var7.getValue().matches(var8)) {
               return false;
            }
         }

         RecipeBook var6 = var4.getRecipeBook();

         for (it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry<ResourceLocation> var8 : this.recipes.object2BooleanEntrySet()) {
            if (var6.contains(var8.getKey()) != var8.getBooleanValue()) {
               return false;
            }
         }

         if (!this.advancements.isEmpty()) {
            PlayerAdvancements var7 = var4.getAdvancements();
            ServerAdvancementManager var8 = var4.getServer().getAdvancements();

            for (Entry<ResourceLocation, PlayerPredicate.AdvancementPredicate> var10 : this.advancements.entrySet()) {
               Advancement var11 = var8.getAdvancement(var10.getKey());
               if (var11 == null || !var10.getValue().test(var7.getOrStartProgress(var11))) {
                  return false;
               }
            }
         }

         if (this.lookingAt != EntityPredicate.ANY) {
            Vec3 var7 = var4.getEyePosition();
            Vec3 var8 = var4.getViewVector(1.0F);
            Vec3 var9 = var7.add(var8.x * 100.0, var8.y * 100.0, var8.z * 100.0);
            EntityHitResult var10 = ProjectileUtil.getEntityHitResult(
               var4.level, var4, var7, var9, new AABB(var7, var9).inflate(1.0), l_param0 -> !l_param0.isSpectator(), 0.0F
            );
            if (var10 == null || var10.getType() != HitResult.Type.ENTITY) {
               return false;
            }

            Entity var11 = var10.getEntity();
            if (!this.lookingAt.matches(var4, var11) || !var4.hasLineOfSight(var11)) {
               return false;
            }
         }

         return true;
      }
   }

   public static PlayerPredicate fromJson(JsonObject param0) {
      MinMaxBounds.Ints var1 = MinMaxBounds.Ints.fromJson(param0.get("level"));
      String var2 = GsonHelper.getAsString(param0, "gamemode", "");
      GameType var3 = GameType.byName(var2, null);
      Map<Stat<?>, MinMaxBounds.Ints> var4 = Maps.newHashMap();
      JsonArray var5 = GsonHelper.getAsJsonArray(param0, "stats", null);
      if (var5 != null) {
         for (JsonElement var7 : var5) {
            JsonObject var8 = GsonHelper.convertToJsonObject(var7, "stats entry");
            ResourceLocation var9 = new ResourceLocation(GsonHelper.getAsString(var8, "type"));
            StatType<?> var10 = BuiltInRegistries.STAT_TYPE.get(var9);
            if (var10 == null) {
               throw new JsonParseException("Invalid stat type: " + var9);
            }

            ResourceLocation var11 = new ResourceLocation(GsonHelper.getAsString(var8, "stat"));
            Stat<?> var12 = getStat(var10, var11);
            MinMaxBounds.Ints var13 = MinMaxBounds.Ints.fromJson(var8.get("value"));
            var4.put(var12, var13);
         }
      }

      Object2BooleanMap<ResourceLocation> var6 = new Object2BooleanOpenHashMap<>();
      JsonObject var7 = GsonHelper.getAsJsonObject(param0, "recipes", new JsonObject());

      for (Entry<String, JsonElement> var9 : var7.entrySet()) {
         ResourceLocation var10 = new ResourceLocation(var9.getKey());
         boolean var11 = GsonHelper.convertToBoolean(var9.getValue(), "recipe present");
         var6.put(var10, var11);
      }

      Map<ResourceLocation, PlayerPredicate.AdvancementPredicate> var8 = Maps.newHashMap();
      JsonObject var9 = GsonHelper.getAsJsonObject(param0, "advancements", new JsonObject());

      for (Entry<String, JsonElement> var11 : var9.entrySet()) {
         ResourceLocation var12 = new ResourceLocation(var11.getKey());
         PlayerPredicate.AdvancementPredicate var13 = advancementPredicateFromJson(var11.getValue());
         var8.put(var12, var13);
      }

      EntityPredicate var10 = EntityPredicate.fromJson(param0.get("looking_at"));
      return new PlayerPredicate(var1, var3, var4, var6, var8, var10);
   }

   private static <T> Stat<T> getStat(StatType<T> param0, ResourceLocation param1) {
      Registry<T> var2 = param0.getRegistry();
      T var3 = var2.get(param1);
      if (var3 == null) {
         throw new JsonParseException("Unknown object " + param1 + " for stat type " + BuiltInRegistries.STAT_TYPE.getKey(param0));
      } else {
         return param0.get(var3);
      }
   }

   private static <T> ResourceLocation getStatValueId(Stat<T> param0) {
      return param0.getType().getRegistry().getKey(param0.getValue());
   }

   @Override
   public JsonObject serializeCustomData() {
      JsonObject var1 = new JsonObject();
      var1.add("level", this.level.serializeToJson());
      if (this.gameType != null) {
         var1.addProperty("gamemode", this.gameType.getName());
      }

      if (!this.stats.isEmpty()) {
         JsonArray var2 = new JsonArray();
         this.stats.forEach((l_param1, l_param2) -> {
            JsonObject l_var3 = new JsonObject();
            l_var3.addProperty("type", BuiltInRegistries.STAT_TYPE.getKey(l_param1.getType()).toString());
            l_var3.addProperty("stat", getStatValueId((Stat<?>)l_param1).toString());
            l_var3.add("value", l_param2.serializeToJson());
            var2.add(l_var3);
         });
         var1.add("stats", var2);
      }

      if (!this.recipes.isEmpty()) {
         JsonObject var2 = new JsonObject();
         this.recipes.forEach((l_param1, l_param2) -> var2.addProperty(l_param1.toString(), l_param2));
         var1.add("recipes", var2);
      }

      if (!this.advancements.isEmpty()) {
         JsonObject var2 = new JsonObject();
         this.advancements.forEach((l_param1, l_param2) -> var2.add(l_param1.toString(), l_param2.toJson()));
         var1.add("advancements", var2);
      }

      var1.add("looking_at", this.lookingAt.serializeToJson());
      return var1;
   }

   @Override
   public EntitySubPredicate.Type type() {
      return EntitySubPredicate.Types.PLAYER;
   }

   static class AdvancementCriterionsPredicate implements PlayerPredicate.AdvancementPredicate {
      private final Object2BooleanMap<String> criterions;

      public AdvancementCriterionsPredicate(Object2BooleanMap<String> param0) {
         this.criterions = param0;
      }

      @Override
      public JsonElement toJson() {
         JsonObject var1 = new JsonObject();
         this.criterions.forEach(var1::addProperty);
         return var1;
      }

      public boolean test(AdvancementProgress param0) {
         for (it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry<String> var3 : this.criterions.object2BooleanEntrySet()) {
            CriterionProgress var4 = param0.getCriterion(var3.getKey());
            if (var4 == null || var4.isDone() != var3.getBooleanValue()) {
               return false;
            }
         }

         return true;
      }
   }

   static class AdvancementDonePredicate implements PlayerPredicate.AdvancementPredicate {
      private final boolean state;

      public AdvancementDonePredicate(boolean param0) {
         this.state = param0;
      }

      @Override
      public JsonElement toJson() {
         return new JsonPrimitive(this.state);
      }

      public boolean test(AdvancementProgress param0) {
         return param0.isDone() == this.state;
      }
   }

   interface AdvancementPredicate extends Predicate<AdvancementProgress> {
      JsonElement toJson();
   }

   public static class Builder {
      private MinMaxBounds.Ints level = MinMaxBounds.Ints.ANY;
      @Nullable
      private GameType gameType;
      private final Map<Stat<?>, MinMaxBounds.Ints> stats = Maps.newHashMap();
      private final Object2BooleanMap<ResourceLocation> recipes = new Object2BooleanOpenHashMap<>();
      private final Map<ResourceLocation, PlayerPredicate.AdvancementPredicate> advancements = Maps.newHashMap();
      private EntityPredicate lookingAt = EntityPredicate.ANY;

      public static PlayerPredicate.Builder player() {
         return new PlayerPredicate.Builder();
      }

      public PlayerPredicate.Builder setLevel(MinMaxBounds.Ints param0) {
         this.level = param0;
         return this;
      }

      public PlayerPredicate.Builder addStat(Stat<?> param0, MinMaxBounds.Ints param1) {
         this.stats.put(param0, param1);
         return this;
      }

      public PlayerPredicate.Builder addRecipe(ResourceLocation param0, boolean param1) {
         this.recipes.put(param0, param1);
         return this;
      }

      public PlayerPredicate.Builder setGameType(GameType param0) {
         this.gameType = param0;
         return this;
      }

      public PlayerPredicate.Builder setLookingAt(EntityPredicate param0) {
         this.lookingAt = param0;
         return this;
      }

      public PlayerPredicate.Builder checkAdvancementDone(ResourceLocation param0, boolean param1) {
         this.advancements.put(param0, new PlayerPredicate.AdvancementDonePredicate(param1));
         return this;
      }

      public PlayerPredicate.Builder checkAdvancementCriterions(ResourceLocation param0, Map<String, Boolean> param1) {
         this.advancements.put(param0, new PlayerPredicate.AdvancementCriterionsPredicate(new Object2BooleanOpenHashMap<>(param1)));
         return this;
      }

      public PlayerPredicate build() {
         return new PlayerPredicate(this.level, this.gameType, this.stats, this.recipes, this.advancements, this.lookingAt);
      }
   }
}

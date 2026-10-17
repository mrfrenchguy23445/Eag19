package net.minecraft.advancements.critereon;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class MobEffectsPredicate {
   public static final MobEffectsPredicate ANY = new MobEffectsPredicate(Collections.emptyMap());
   private final Map<MobEffect, MobEffectsPredicate.MobEffectInstancePredicate> effects;

   public MobEffectsPredicate(Map<MobEffect, MobEffectsPredicate.MobEffectInstancePredicate> param0) {
      this.effects = param0;
   }

   public static MobEffectsPredicate effects() {
      return new MobEffectsPredicate(Maps.newLinkedHashMap());
   }

   public MobEffectsPredicate and(MobEffect param0) {
      this.effects.put(param0, new MobEffectsPredicate.MobEffectInstancePredicate());
      return this;
   }

   public MobEffectsPredicate and(MobEffect param0, MobEffectsPredicate.MobEffectInstancePredicate param1) {
      this.effects.put(param0, param1);
      return this;
   }

   public boolean matches(Entity param0) {
      if (this == ANY) {
         return true;
      } else {
         return param0 instanceof LivingEntity ? this.matches(((LivingEntity)param0).getActiveEffectsMap()) : false;
      }
   }

   public boolean matches(LivingEntity param0) {
      return this == ANY ? true : this.matches(param0.getActiveEffectsMap());
   }

   public boolean matches(Map<MobEffect, MobEffectInstance> param0) {
      if (this == ANY) {
         return true;
      }

      for (Entry<MobEffect, MobEffectsPredicate.MobEffectInstancePredicate> var3 : this.effects.entrySet()) {
         MobEffectInstance var4 = param0.get(var3.getKey());
         if (!var3.getValue().matches(var4)) {
            return false;
         }
      }

      return true;
   }

   public static MobEffectsPredicate fromJson(@Nullable JsonElement param0) {
      if (param0 != null && !param0.isJsonNull()) {
         JsonObject var1 = GsonHelper.convertToJsonObject(param0, "effects");
         Map<MobEffect, MobEffectsPredicate.MobEffectInstancePredicate> var2 = Maps.newLinkedHashMap();

         for (Entry<String, JsonElement> var4 : var1.entrySet()) {
            ResourceLocation var5 = new ResourceLocation(var4.getKey());
            MobEffect var6 = BuiltInRegistries.MOB_EFFECT.getOptional(var5).orElseThrow(() -> new JsonSyntaxException("Unknown effect '" + var5 + "'"));
            MobEffectsPredicate.MobEffectInstancePredicate var7 = MobEffectsPredicate.MobEffectInstancePredicate.fromJson(
               GsonHelper.convertToJsonObject(var4.getValue(), var4.getKey())
            );
            var2.put(var6, var7);
         }

         return new MobEffectsPredicate(var2);
      } else {
         return ANY;
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      }

      JsonObject var1 = new JsonObject();

      for (Entry<MobEffect, MobEffectsPredicate.MobEffectInstancePredicate> var3 : this.effects.entrySet()) {
         var1.add(BuiltInRegistries.MOB_EFFECT.getKey(var3.getKey()).toString(), var3.getValue().serializeToJson());
      }

      return var1;
   }

   public static class MobEffectInstancePredicate {
      private final MinMaxBounds.Ints amplifier;
      private final MinMaxBounds.Ints duration;
      @Nullable
      private final Boolean ambient;
      @Nullable
      private final Boolean visible;

      public MobEffectInstancePredicate(MinMaxBounds.Ints param0, MinMaxBounds.Ints param1, @Nullable Boolean param2, @Nullable Boolean param3) {
         this.amplifier = param0;
         this.duration = param1;
         this.ambient = param2;
         this.visible = param3;
      }

      public MobEffectInstancePredicate() {
         this(MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, null, null);
      }

      public boolean matches(@Nullable MobEffectInstance param0) {
         if (param0 == null) {
            return false;
         } else if (!this.amplifier.matches(param0.getAmplifier())) {
            return false;
         } else if (!this.duration.matches(param0.getDuration())) {
            return false;
         } else {
            return this.ambient != null && this.ambient != param0.isAmbient() ? false : this.visible == null || this.visible == param0.isVisible();
         }
      }

      public JsonElement serializeToJson() {
         JsonObject var1 = new JsonObject();
         var1.add("amplifier", this.amplifier.serializeToJson());
         var1.add("duration", this.duration.serializeToJson());
         var1.addProperty("ambient", this.ambient);
         var1.addProperty("visible", this.visible);
         return var1;
      }

      public static MobEffectsPredicate.MobEffectInstancePredicate fromJson(JsonObject param0) {
         MinMaxBounds.Ints var1 = MinMaxBounds.Ints.fromJson(param0.get("amplifier"));
         MinMaxBounds.Ints var2 = MinMaxBounds.Ints.fromJson(param0.get("duration"));
         Boolean var3 = param0.has("ambient") ? GsonHelper.getAsBoolean(param0, "ambient") : null;
         Boolean var4 = param0.has("visible") ? GsonHelper.getAsBoolean(param0, "visible") : null;
         return new MobEffectsPredicate.MobEffectInstancePredicate(var1, var2, var3, var4);
      }
   }
}

package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import org.slf4j.Logger;

public class PredicateManager extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = Deserializers.createConditionSerializer().create();
   private Map<ResourceLocation, LootItemCondition> conditions = ImmutableMap.of();

   public PredicateManager() {
      super(GSON, "predicates");
   }

   @Nullable
   public LootItemCondition get(ResourceLocation param0) {
      return this.conditions.get(param0);
   }

   protected void apply(Map<ResourceLocation, JsonElement> param0, ResourceManager param1, ProfilerFiller param2) {
      Builder<ResourceLocation, LootItemCondition> var4 = ImmutableMap.builder();
      param0.forEach((l_param1, l_param2) -> {
         try {
            if (l_param2.isJsonArray()) {
               LootItemCondition[] l_var3 = GSON.fromJson(l_param2, LootItemCondition[].class);
               var4.put(l_param1, new PredicateManager.CompositePredicate(l_var3));
            } else {
               LootItemCondition l_var3 = GSON.fromJson(l_param2, LootItemCondition.class);
               var4.put(l_param1, l_var3);
            }
         } catch (Exception l_var3) {
            LOGGER.error("Couldn't parse loot table {}", l_param1, l_var3);
         }
      });
      Map<ResourceLocation, LootItemCondition> var5 = var4.build();
      ValidationContext var6 = new ValidationContext(LootContextParamSets.ALL_PARAMS, var5::get, l_param0 -> null);
      var5.forEach((l_param1, l_param2) -> l_param2.validate(var6.enterCondition("{" + l_param1 + "}", l_param1)));
      var6.getProblems().forEach((l_param0, l_param1) -> LOGGER.warn("Found validation problem in {}: {}", l_param0, l_param1));
      this.conditions = var5;
   }

   public Set<ResourceLocation> getKeys() {
      return Collections.unmodifiableSet(this.conditions.keySet());
   }

   static class CompositePredicate implements LootItemCondition {
      private final LootItemCondition[] terms;
      private final Predicate<LootContext> composedPredicate;

      CompositePredicate(LootItemCondition[] param0) {
         this.terms = param0;
         this.composedPredicate = LootItemConditions.andConditions(param0);
      }

      public final boolean test(LootContext param0) {
         return this.composedPredicate.test(param0);
      }

      @Override
      public void validate(ValidationContext param0) {
         LootItemCondition.super.validate(param0);

         for (int var2 = 0; var2 < this.terms.length; var2++) {
            this.terms[var2].validate(param0.forChild(".term[" + var2 + "]"));
         }
      }

      @Override
      public LootItemConditionType getType() {
         throw new UnsupportedOperationException();
      }
   }
}

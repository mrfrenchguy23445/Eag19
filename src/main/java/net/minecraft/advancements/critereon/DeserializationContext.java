package net.minecraft.advancements.critereon;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.PredicateManager;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class DeserializationContext {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourceLocation id;
   private final PredicateManager predicateManager;
   private final Gson predicateGson = Deserializers.createConditionSerializer().create();

   public DeserializationContext(ResourceLocation param0, PredicateManager param1) {
      this.id = param0;
      this.predicateManager = param1;
   }

   public final LootItemCondition[] deserializeConditions(JsonArray param0, String param1, LootContextParamSet param2) {
      LootItemCondition[] var4 = this.predicateGson.fromJson(param0, LootItemCondition[].class);
      ValidationContext var5 = new ValidationContext(param2, this.predicateManager::get, l_param0 -> null);

      for (LootItemCondition var9 : var4) {
         var9.validate(var5);
         var5.getProblems()
            .forEach((l_param1, l_param2) -> LOGGER.warn("Found validation problem in advancement trigger {}/{}: {}", param1, l_param1, l_param2));
      }

      return var4;
   }

   public ResourceLocation getAdvancementId() {
      return this.id;
   }
}

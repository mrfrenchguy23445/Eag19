package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootTables extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = Deserializers.createLootTableSerializer().create();
   private Map<ResourceLocation, LootTable> tables = ImmutableMap.of();
   private final PredicateManager predicateManager;

   public LootTables(PredicateManager param0) {
      super(GSON, "loot_tables");
      this.predicateManager = param0;
   }

   public LootTable get(ResourceLocation param0) {
      return this.tables.getOrDefault(param0, LootTable.EMPTY);
   }

   protected void apply(Map<ResourceLocation, JsonElement> param0, ResourceManager param1, ProfilerFiller param2) {
      Builder<ResourceLocation, LootTable> var4 = ImmutableMap.builder();
      JsonElement var5 = param0.remove(BuiltInLootTables.EMPTY);
      if (var5 != null) {
         LOGGER.warn("Datapack tried to redefine {} loot table, ignoring", BuiltInLootTables.EMPTY);
      }

      param0.forEach((l_param1, l_param2) -> {
         try {
            LootTable l_var3 = GSON.fromJson(l_param2, LootTable.class);
            var4.put(l_param1, l_var3);
         } catch (Exception l_var3) {
            LOGGER.error("Couldn't parse loot table {}", l_param1, l_var3);
         }
      });
      var4.put(BuiltInLootTables.EMPTY, LootTable.EMPTY);
      ImmutableMap<ResourceLocation, LootTable> var6 = var4.build();
      ValidationContext var7 = new ValidationContext(LootContextParamSets.ALL_PARAMS, this.predicateManager::get, var6::get);
      var6.forEach((l_param1, l_param2) -> validate(var7, l_param1, l_param2));
      var7.getProblems().forEach((l_param0, l_param1) -> LOGGER.warn("Found validation problem in {}: {}", l_param0, l_param1));
      this.tables = var6;
   }

   public static void validate(ValidationContext param0, ResourceLocation param1, LootTable param2) {
      param2.validate(param0.setParams(param2.getParamSet()).enterTable("{" + param1 + "}", param1));
   }

   public static JsonElement serialize(LootTable param0) {
      return GSON.toJsonTree(param0);
   }

   public Set<ResourceLocation> getIds() {
      return this.tables.keySet();
   }
}

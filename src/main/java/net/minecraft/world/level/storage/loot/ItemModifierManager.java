package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class ItemModifierManager extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = Deserializers.createFunctionSerializer().create();
   private final PredicateManager predicateManager;
   private final LootTables lootTables;
   private Map<ResourceLocation, LootItemFunction> functions = ImmutableMap.of();

   public ItemModifierManager(PredicateManager param0, LootTables param1) {
      super(GSON, "item_modifiers");
      this.predicateManager = param0;
      this.lootTables = param1;
   }

   @Nullable
   public LootItemFunction get(ResourceLocation param0) {
      return this.functions.get(param0);
   }

   public LootItemFunction get(ResourceLocation param0, LootItemFunction param1) {
      return this.functions.getOrDefault(param0, param1);
   }

   protected void apply(Map<ResourceLocation, JsonElement> param0, ResourceManager param1, ProfilerFiller param2) {
      Builder<ResourceLocation, LootItemFunction> var4 = ImmutableMap.builder();
      param0.forEach((l_param1, l_param2) -> {
         try {
            if (l_param2.isJsonArray()) {
               LootItemFunction[] l_var3 = GSON.fromJson(l_param2, LootItemFunction[].class);
               var4.put(l_param1, new ItemModifierManager.FunctionSequence(l_var3));
            } else {
               LootItemFunction l_var3 = GSON.fromJson(l_param2, LootItemFunction.class);
               var4.put(l_param1, l_var3);
            }
         } catch (Exception l_var3) {
            LOGGER.error("Couldn't parse item modifier {}", l_param1, l_var3);
         }
      });
      Map<ResourceLocation, LootItemFunction> var5 = var4.build();
      ValidationContext var6 = new ValidationContext(LootContextParamSets.ALL_PARAMS, this.predicateManager::get, this.lootTables::get);
      var5.forEach((l_param1, l_param2) -> l_param2.validate(var6));
      var6.getProblems().forEach((l_param0, l_param1) -> LOGGER.warn("Found item modifier validation problem in {}: {}", l_param0, l_param1));
      this.functions = var5;
   }

   public Set<ResourceLocation> getKeys() {
      return Collections.unmodifiableSet(this.functions.keySet());
   }

   static class FunctionSequence implements LootItemFunction {
      protected final LootItemFunction[] functions;
      private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;

      public FunctionSequence(LootItemFunction[] param0) {
         this.functions = param0;
         this.compositeFunction = LootItemFunctions.compose(param0);
      }

      public ItemStack apply(ItemStack param0, LootContext param1) {
         return this.compositeFunction.apply(param0, param1);
      }

      @Override
      public LootItemFunctionType getType() {
         throw new UnsupportedOperationException();
      }
   }
}

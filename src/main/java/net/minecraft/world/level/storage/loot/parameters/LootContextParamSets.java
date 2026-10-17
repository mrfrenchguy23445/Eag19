package net.minecraft.world.level.storage.loot.parameters;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public class LootContextParamSets {
   private static final BiMap<ResourceLocation, LootContextParamSet> REGISTRY = HashBiMap.create();
   public static final LootContextParamSet EMPTY = register("empty", param0 -> {});
   public static final LootContextParamSet CHEST = register(
      "chest", param0 -> param0.required(LootContextParams.ORIGIN).optional(LootContextParams.THIS_ENTITY)
   );
   public static final LootContextParamSet COMMAND = register(
      "command", param0 -> param0.required(LootContextParams.ORIGIN).optional(LootContextParams.THIS_ENTITY)
   );
   public static final LootContextParamSet SELECTOR = register(
      "selector", param0 -> param0.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY)
   );
   public static final LootContextParamSet FISHING = register(
      "fishing", param0 -> param0.required(LootContextParams.ORIGIN).required(LootContextParams.TOOL).optional(LootContextParams.THIS_ENTITY)
   );
   public static final LootContextParamSet ENTITY = register(
      "entity",
      param0 -> param0.required(LootContextParams.THIS_ENTITY)
         .required(LootContextParams.ORIGIN)
         .required(LootContextParams.DAMAGE_SOURCE)
         .optional(LootContextParams.KILLER_ENTITY)
         .optional(LootContextParams.DIRECT_KILLER_ENTITY)
         .optional(LootContextParams.LAST_DAMAGE_PLAYER)
   );
   public static final LootContextParamSet ARCHAEOLOGY = register(
      "archaeology", param0 -> param0.required(LootContextParams.ORIGIN).optional(LootContextParams.THIS_ENTITY)
   );
   public static final LootContextParamSet GIFT = register("gift", param0 -> param0.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY));
   public static final LootContextParamSet PIGLIN_BARTER = register("barter", param0 -> param0.required(LootContextParams.THIS_ENTITY));
   public static final LootContextParamSet ADVANCEMENT_REWARD = register(
      "advancement_reward", param0 -> param0.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN)
   );
   public static final LootContextParamSet ADVANCEMENT_ENTITY = register(
      "advancement_entity", param0 -> param0.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN)
   );
   public static final LootContextParamSet ALL_PARAMS = register(
      "generic",
      param0 -> param0.required(LootContextParams.THIS_ENTITY)
         .required(LootContextParams.LAST_DAMAGE_PLAYER)
         .required(LootContextParams.DAMAGE_SOURCE)
         .required(LootContextParams.KILLER_ENTITY)
         .required(LootContextParams.DIRECT_KILLER_ENTITY)
         .required(LootContextParams.ORIGIN)
         .required(LootContextParams.BLOCK_STATE)
         .required(LootContextParams.BLOCK_ENTITY)
         .required(LootContextParams.TOOL)
         .required(LootContextParams.EXPLOSION_RADIUS)
   );
   public static final LootContextParamSet BLOCK = register(
      "block",
      param0 -> param0.required(LootContextParams.BLOCK_STATE)
         .required(LootContextParams.ORIGIN)
         .required(LootContextParams.TOOL)
         .optional(LootContextParams.THIS_ENTITY)
         .optional(LootContextParams.BLOCK_ENTITY)
         .optional(LootContextParams.EXPLOSION_RADIUS)
   );

   private static LootContextParamSet register(String param0, Consumer<LootContextParamSet.Builder> param1) {
      LootContextParamSet.Builder var2 = new LootContextParamSet.Builder();
      param1.accept(var2);
      LootContextParamSet var3 = var2.build();
      ResourceLocation var4 = new ResourceLocation(param0);
      LootContextParamSet var5 = REGISTRY.put(var4, var3);
      if (var5 != null) {
         throw new IllegalStateException("Loot table parameter set " + var4 + " is already registered");
      } else {
         return var3;
      }
   }

   @Nullable
   public static LootContextParamSet get(ResourceLocation param0) {
      return REGISTRY.get(param0);
   }

   @Nullable
   public static ResourceLocation getKey(LootContextParamSet param0) {
      return REGISTRY.inverse().get(param0);
   }
}

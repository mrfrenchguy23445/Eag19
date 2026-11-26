package net.minecraft.data.loot;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootTableProvider implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackOutput.PathProvider pathProvider;
   private final Set<ResourceLocation> requiredTables;
   private final List<LootTableProvider.SubProviderEntry> subProviders;

   public LootTableProvider(PackOutput param0, Set<ResourceLocation> param1, List<LootTableProvider.SubProviderEntry> param2) {
      this.pathProvider = param0.createPathProvider(PackOutput.Target.DATA_PACK, "loot_tables");
      this.subProviders = param2;
      this.requiredTables = param1;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput param0) {
      Map<ResourceLocation, LootTable> var2 = Maps.newHashMap();
      this.subProviders.forEach(l_param1 -> l_param1.provider().get().generate((l2_param2, l2_param3) -> {
         if (var2.put(l2_param2, l2_param3.setParamSet(l_param1.paramSet).build()) != null) {
            throw new IllegalStateException("Duplicate loot table " + l2_param2);
         }
      }));
      ValidationContext var3 = new ValidationContext(LootContextParamSets.ALL_PARAMS, l_param0 -> null, var2::get);

      for (ResourceLocation var6 : Sets.difference(this.requiredTables, var2.keySet())) {
         var3.reportProblem("Missing built-in table: " + var6);
      }

      var2.forEach((l_param1, l_param2) -> LootTables.validate(var3, l_param1, l_param2));
      Multimap<String, String> var5 = var3.getProblems();
      if (!var5.isEmpty()) {
         var5.forEach((l_param0, l_param1) -> LOGGER.warn("Found validation problem in {}: {}", l_param0, l_param1));
         throw new IllegalStateException("Failed to validate loot tables, see logs");
      } else {
         return CompletableFuture.allOf(var2.entrySet().stream().map(l_param1 -> {
            ResourceLocation l_var3 = l_param1.getKey();
            LootTable l_var4 = l_param1.getValue();
            Path l_var5 = this.pathProvider.json(l_var3);
            return DataProvider.saveStable(param0, LootTables.serialize(l_var4), l_var5);
         }).toArray(CompletableFuture[]::new));
      }
   }

   @Override
   public final String getName() {
      return "Loot Tables";
   }

   public record SubProviderEntry(Supplier<LootTableSubProvider> provider, LootContextParamSet paramSet) {
   }
}

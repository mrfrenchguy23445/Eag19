package net.minecraft.data.advancements;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public class AdvancementProvider implements DataProvider {
   private final PackOutput.PathProvider pathProvider;
   private final List<AdvancementSubProvider> subProviders;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public AdvancementProvider(PackOutput param0, CompletableFuture<HolderLookup.Provider> param1, List<AdvancementSubProvider> param2) {
      this.pathProvider = param0.createPathProvider(PackOutput.Target.DATA_PACK, "advancements");
      this.subProviders = param2;
      this.registries = param1;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput param0) {
      return this.registries.thenCompose(l_param1 -> {
         Set<ResourceLocation> l_var3 = new HashSet<>();
         List<CompletableFuture<?>> l_var4 = new ArrayList<>();
         Consumer<Advancement> l_var5 = l2_param3 -> {
            if (!l_var3.add(l2_param3.getId())) {
               throw new IllegalStateException("Duplicate advancement " + l2_param3.getId());
            }

            Path l2_var5 = this.pathProvider.json(l2_param3.getId());
            l_var4.add(DataProvider.saveStable(param0, l2_param3.deconstruct().serializeToJson(), l2_var5));
         };

         for (AdvancementSubProvider l_var7 : this.subProviders) {
            l_var7.generate(l_param1, l_var5);
         }

         return CompletableFuture.allOf(l_var4.toArray(CompletableFuture[]::new));
      });
   }

   @Override
   public final String getName() {
      return "Advancements";
   }
}

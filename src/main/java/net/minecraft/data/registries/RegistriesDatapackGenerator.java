package net.minecraft.data.registries;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

public class RegistriesDatapackGenerator implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackOutput output;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public RegistriesDatapackGenerator(PackOutput param0, CompletableFuture<HolderLookup.Provider> param1) {
      this.registries = param1;
      this.output = param0;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput param0) {
      return this.registries
         .thenCompose(
            l_param1 -> {
               DynamicOps<JsonElement> l_var3 = RegistryOps.create(JsonOps.INSTANCE, l_param1);
               return CompletableFuture.allOf(
                  RegistryDataLoader.WORLDGEN_REGISTRIES
                     .stream()
                     .flatMap(l2_param3 -> this.dumpRegistryCap(param0, l_param1, l_var3, (RegistryDataLoader.RegistryData<?>)l2_param3).stream())
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   private <T> Optional<CompletableFuture<?>> dumpRegistryCap(
      CachedOutput param0, HolderLookup.Provider param1, DynamicOps<JsonElement> param2, RegistryDataLoader.RegistryData<T> param3
   ) {
      ResourceKey<? extends Registry<T>> var5 = param3.key();
      return param1.lookup(var5)
         .map(
            l_param4 -> {
               PackOutput.PathProvider l_var6 = this.output.createPathProvider(PackOutput.Target.DATA_PACK, var5.location().getPath());
               return CompletableFuture.allOf(
                  l_param4.listElements()
                     .map(l2_param4 -> dumpValue(l_var6.json(l2_param4.key().location()), param0, param2, param3.elementCodec(), l2_param4.value()))
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   private static <E> CompletableFuture<?> dumpValue(Path param0, CachedOutput param1, DynamicOps<JsonElement> param2, Encoder<E> param3, E param4) {
      Optional<JsonElement> var5 = param3.encodeStart(param2, param4)
         .resultOrPartial(l_param1 -> LOGGER.error("Couldn't serialize element {}: {}", param0, l_param1));
      return var5.isPresent() ? DataProvider.saveStable(param1, var5.get(), param0) : CompletableFuture.completedFuture(null);
   }

   @Override
   public final String getName() {
      return "Registries";
   }
}

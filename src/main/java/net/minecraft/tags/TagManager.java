package net.minecraft.tags;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

public class TagManager implements PreparableReloadListener {
   private static final Map<ResourceKey<? extends Registry<?>>, String> CUSTOM_REGISTRY_DIRECTORIES = Map.of(
      Registries.BLOCK,
      "tags/blocks",
      Registries.ENTITY_TYPE,
      "tags/entity_types",
      Registries.FLUID,
      "tags/fluids",
      Registries.GAME_EVENT,
      "tags/game_events",
      Registries.ITEM,
      "tags/items"
   );
   private final RegistryAccess registryAccess;
   private List<TagManager.LoadResult<?>> results = List.of();

   public TagManager(RegistryAccess param0) {
      this.registryAccess = param0;
   }

   public List<TagManager.LoadResult<?>> getResult() {
      return this.results;
   }

   public static String getTagDir(ResourceKey<? extends Registry<?>> param0) {
      String var1 = CUSTOM_REGISTRY_DIRECTORIES.get(param0);
      return var1 != null ? var1 : "tags/" + param0.location().getPath();
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.PreparationBarrier param0,
      ResourceManager param1,
      ProfilerFiller param2,
      ProfilerFiller param3,
      Executor param4,
      Executor param5
   ) {
      List<? extends CompletableFuture<? extends TagManager.LoadResult<?>>> var7 = this.registryAccess
         .registries()
         .map(l_param2 -> this.createLoader(param1, param4, (RegistryAccess.RegistryEntry<?>)l_param2))
         .toList();
      return CompletableFuture.allOf(var7.toArray(CompletableFuture[]::new))
         .thenCompose(param0::wait)
         .thenAcceptAsync(l_param1 -> this.results = var7.stream().map(CompletableFuture::join).collect(Collectors.toUnmodifiableList()), param5);
   }

   private <T> CompletableFuture<TagManager.LoadResult<T>> createLoader(ResourceManager param0, Executor param1, RegistryAccess.RegistryEntry<T> param2) {
      ResourceKey<? extends Registry<T>> var4 = param2.key();
      Registry<T> var5 = param2.value();
      TagLoader<Holder<T>> var6 = new TagLoader<>(l_param2 -> var5.getHolder(ResourceKey.create(var4, l_param2)), getTagDir(var4));
      return CompletableFuture.supplyAsync(() -> new TagManager.LoadResult<>(var4, var6.loadAndBuild(param0)), param1);
   }

   public record LoadResult<T>(ResourceKey<? extends Registry<T>> key, Map<ResourceLocation, Collection<Holder<T>>> tags) {
   }
}

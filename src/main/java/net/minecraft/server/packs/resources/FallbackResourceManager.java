package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;

public class FallbackResourceManager implements ResourceManager {
   static final Logger LOGGER = LogUtils.getLogger();
   protected final List<FallbackResourceManager.PackEntry> fallbacks = Lists.newArrayList();
   private final PackType type;
   private final String namespace;

   public FallbackResourceManager(PackType param0, String param1) {
      this.type = param0;
      this.namespace = param1;
   }

   public void push(PackResources param0) {
      this.pushInternal(param0.packId(), param0, null);
   }

   public void push(PackResources param0, Predicate<ResourceLocation> param1) {
      this.pushInternal(param0.packId(), param0, param1);
   }

   public void pushFilterOnly(String param0, Predicate<ResourceLocation> param1) {
      this.pushInternal(param0, null, param1);
   }

   private void pushInternal(String param0, @Nullable PackResources param1, @Nullable Predicate<ResourceLocation> param2) {
      this.fallbacks.add(new FallbackResourceManager.PackEntry(param0, param1, param2));
   }

   @Override
   public Set<String> getNamespaces() {
      return ImmutableSet.of(this.namespace);
   }

   @Override
   public Optional<Resource> getResource(ResourceLocation param0) {
      for (int var2 = this.fallbacks.size() - 1; var2 >= 0; var2--) {
         FallbackResourceManager.PackEntry var3 = this.fallbacks.get(var2);
         PackResources var4 = var3.resources;
         if (var4 != null) {
            IoSupplier<InputStream> var5 = var4.getResource(this.type, param0);
            if (var5 != null) {
               IoSupplier<ResourceMetadata> var6 = this.createStackMetadataFinder(param0, var2);
               return Optional.of(createResource(var4, param0, var5, var6));
            }
         }

         if (var3.isFiltered(param0)) {
            LOGGER.warn("Resource {} not found, but was filtered by pack {}", param0, var3.name);
            return Optional.empty();
         }
      }

      return Optional.empty();
   }

   private static Resource createResource(PackResources param0, ResourceLocation param1, IoSupplier<InputStream> param2, IoSupplier<ResourceMetadata> param3) {
      return new Resource(param0, wrapForDebug(param1, param0, param2), param3);
   }

   private static IoSupplier<InputStream> wrapForDebug(ResourceLocation param0, PackResources param1, IoSupplier<InputStream> param2) {
      return LOGGER.isDebugEnabled() ? () -> new FallbackResourceManager.LeakedResourceWarningInputStream(param2.get(), param0, param1.packId()) : param2;
   }

   @Override
   public List<Resource> getResourceStack(ResourceLocation param0) {
      ResourceLocation var2 = getMetadataLocation(param0);
      List<Resource> var3 = new ArrayList<>();
      boolean var4 = false;
      String var5 = null;

      for (int var6 = this.fallbacks.size() - 1; var6 >= 0; var6--) {
         FallbackResourceManager.PackEntry var7 = this.fallbacks.get(var6);
         PackResources var8 = var7.resources;
         if (var8 != null) {
            IoSupplier<InputStream> var9 = var8.getResource(this.type, param0);
            if (var9 != null) {
               IoSupplier<ResourceMetadata> var10;
               if (var4) {
                  var10 = ResourceMetadata.EMPTY_SUPPLIER;
               } else {
                  var10 = () -> {
                     IoSupplier<InputStream> l_var3 = var8.getResource(this.type, var2);
                     return l_var3 != null ? parseMetadata(l_var3) : ResourceMetadata.EMPTY;
                  };
               }

               var3.add(new Resource(var8, var9, var10));
            }
         }

         if (var7.isFiltered(param0)) {
            var5 = var7.name;
            break;
         }

         if (var7.isFiltered(var2)) {
            var4 = true;
         }
      }

      if (var3.isEmpty() && var5 != null) {
         LOGGER.warn("Resource {} not found, but was filtered by pack {}", param0, var5);
      }

      return Lists.reverse(var3);
   }

   private static boolean isMetadata(ResourceLocation param0) {
      return param0.getPath().endsWith(".mcmeta");
   }

   private static ResourceLocation getResourceLocationFromMetadata(ResourceLocation param0) {
      String var1 = param0.getPath().substring(0, param0.getPath().length() - ".mcmeta".length());
      return param0.withPath(var1);
   }

   static ResourceLocation getMetadataLocation(ResourceLocation param0) {
      return param0.withPath(param0.getPath() + ".mcmeta");
   }

   @Override
   public Map<ResourceLocation, Resource> listResources(String param0, Predicate<ResourceLocation> param1) {
      record ResourceWithSourceAndIndex(PackResources packResources, IoSupplier<InputStream> resource, int packIndex) {
      }

      Map<ResourceLocation, ResourceWithSourceAndIndex> var3 = new HashMap<>();
      Map<ResourceLocation, ResourceWithSourceAndIndex> var4 = new HashMap<>();
      int var5 = this.fallbacks.size();

      for (int var6 = 0; var6 < var5; var6++) {
         FallbackResourceManager.PackEntry var7 = this.fallbacks.get(var6);
         var7.filterAll(var3.keySet());
         var7.filterAll(var4.keySet());
         PackResources var8 = var7.resources;
         if (var8 != null) {
            int var9 = var6;
            var8.listResources(this.type, this.namespace, param0, (l_param5, l_param6) -> {
               if (isMetadata(l_param5)) {
                  if (param1.test(getResourceLocationFromMetadata(l_param5))) {
                     var4.put(l_param5, new ResourceWithSourceAndIndex(var8, l_param6, var9));
                  }
               } else if (param1.test(l_param5)) {
                  var3.put(l_param5, new ResourceWithSourceAndIndex(var8, l_param6, var9));
               }
            });
         }
      }

      Map<ResourceLocation, Resource> var6 = Maps.newTreeMap();
      var3.forEach((l_param2, l_param3) -> {
         ResourceLocation l_var5 = getMetadataLocation(l_param2);
         ResourceWithSourceAndIndex l_var6 = var4.get(l_var5);
         IoSupplier<ResourceMetadata> l_var4;
         if (l_var6 != null && l_var6.packIndex >= l_param3.packIndex) {
            l_var4 = convertToMetadata(l_var6.resource);
         } else {
            l_var4 = ResourceMetadata.EMPTY_SUPPLIER;
         }

         var6.put(l_param2, createResource(l_param3.packResources, l_param2, l_param3.resource, l_var4));
      });
      return var6;
   }

   private IoSupplier<ResourceMetadata> createStackMetadataFinder(ResourceLocation param0, int param1) {
      return () -> {
         ResourceLocation l_var3 = getMetadataLocation(param0);

         for (int l_var4 = this.fallbacks.size() - 1; l_var4 >= param1; l_var4--) {
            FallbackResourceManager.PackEntry l_var5 = this.fallbacks.get(l_var4);
            PackResources l_var6 = l_var5.resources;
            if (l_var6 != null) {
               IoSupplier<InputStream> l_var7 = l_var6.getResource(this.type, l_var3);
               if (l_var7 != null) {
                  return parseMetadata(l_var7);
               }
            }

            if (l_var5.isFiltered(l_var3)) {
               break;
            }
         }

         return ResourceMetadata.EMPTY;
      };
   }

   private static IoSupplier<ResourceMetadata> convertToMetadata(IoSupplier<InputStream> param0) {
      return () -> parseMetadata(param0);
   }

   private static ResourceMetadata parseMetadata(IoSupplier<InputStream> param0) throws IOException {
      try (InputStream var1 = param0.get()) {
         return ResourceMetadata.fromJsonStream(var1);
      }
   }

   private static void applyPackFiltersToExistingResources(
      FallbackResourceManager.PackEntry param0, Map<ResourceLocation, FallbackResourceManager.EntryStack> param1
   ) {
      for (FallbackResourceManager.EntryStack var3 : param1.values()) {
         if (param0.isFiltered(var3.fileLocation)) {
            var3.fileSources.clear();
         } else if (param0.isFiltered(var3.metadataLocation())) {
            var3.metaSources.clear();
         }
      }
   }

   private void listPackResources(
      FallbackResourceManager.PackEntry param0,
      String param1,
      Predicate<ResourceLocation> param2,
      Map<ResourceLocation, FallbackResourceManager.EntryStack> param3
   ) {
      PackResources var5 = param0.resources;
      if (var5 != null) {
         var5.listResources(
            this.type,
            this.namespace,
            param1,
            (l_param3, l_param4) -> {
               if (isMetadata(l_param3)) {
                  ResourceLocation l_var5 = getResourceLocationFromMetadata(l_param3);
                  if (!param2.test(l_var5)) {
                     return;
                  }

                  param3.computeIfAbsent(l_var5, FallbackResourceManager.EntryStack::new).metaSources.put(var5, l_param4);
               } else {
                  if (!param2.test(l_param3)) {
                     return;
                  }

                  param3.computeIfAbsent(l_param3, FallbackResourceManager.EntryStack::new)
                     .fileSources
                     .add(new FallbackResourceManager.ResourceWithSource(var5, l_param4));
               }
            }
         );
      }
   }

   @Override
   public Map<ResourceLocation, List<Resource>> listResourceStacks(String param0, Predicate<ResourceLocation> param1) {
      Map<ResourceLocation, FallbackResourceManager.EntryStack> var3 = Maps.newHashMap();

      for (FallbackResourceManager.PackEntry var5 : this.fallbacks) {
         applyPackFiltersToExistingResources(var5, var3);
         this.listPackResources(var5, param0, param1, var3);
      }

      TreeMap<ResourceLocation, List<Resource>> var4 = Maps.newTreeMap();

      for (FallbackResourceManager.EntryStack var6 : var3.values()) {
         if (!var6.fileSources.isEmpty()) {
            List<Resource> var7 = new ArrayList<>();

            for (FallbackResourceManager.ResourceWithSource var9 : var6.fileSources) {
               PackResources var10 = var9.source;
               IoSupplier<InputStream> var11 = var6.metaSources.get(var10);
               IoSupplier<ResourceMetadata> var12 = var11 != null ? convertToMetadata(var11) : ResourceMetadata.EMPTY_SUPPLIER;
               var7.add(createResource(var10, var6.fileLocation, var9.resource, var12));
            }

            var4.put(var6.fileLocation, var7);
         }
      }

      return var4;
   }

   @Override
   public Stream<PackResources> listPacks() {
      return this.fallbacks.stream().map(l_param0 -> l_param0.resources).filter(Objects::nonNull);
   }

   record EntryStack(
      ResourceLocation fileLocation,
      ResourceLocation metadataLocation,
      List<FallbackResourceManager.ResourceWithSource> fileSources,
      Map<PackResources, IoSupplier<InputStream>> metaSources
   ) {

      EntryStack(ResourceLocation param0) {
         this(param0, FallbackResourceManager.getMetadataLocation(param0), new ArrayList<>(), new Object2ObjectArrayMap<>());
      }
   }

   static class LeakedResourceWarningInputStream extends FilterInputStream {
      private final Supplier<String> message;
      private boolean closed;

      public LeakedResourceWarningInputStream(InputStream param0, ResourceLocation param1, String param2) {
         super(param0);
         Exception var4 = new Exception("Stacktrace");
         this.message = () -> {
            StringWriter l_var3 = new StringWriter();
            var4.printStackTrace(new PrintWriter(l_var3));
            return "Leaked resource: '" + param1 + "' loaded from pack: '" + param2 + "'\n" + l_var3;
         };
      }

      @Override
      public void close() throws IOException {
         super.close();
         this.closed = true;
      }

      @Override
      protected void finalize() throws Throwable {
         if (!this.closed) {
            FallbackResourceManager.LOGGER.warn("{}", this.message.get());
         }

         super.finalize();
      }
   }

   record PackEntry(String name, @Nullable PackResources resources, @Nullable Predicate<ResourceLocation> filter) {

      public void filterAll(Collection<ResourceLocation> param0) {
         if (this.filter != null) {
            param0.removeIf(this.filter);
         }
      }

      public boolean isFiltered(ResourceLocation param0) {
         return this.filter != null && this.filter.test(param0);
      }
   }

   record ResourceWithSource(PackResources source, IoSupplier<InputStream> resource) {
   }
}

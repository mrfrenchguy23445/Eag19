package net.minecraft.server.packs.resources;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;

public class MultiPackResourceManager implements CloseableResourceManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<String, FallbackResourceManager> namespacedManagers;
   private final List<PackResources> packs;

   public MultiPackResourceManager(PackType param0, List<PackResources> param1) {
      this.packs = List.copyOf(param1);
      Map<String, FallbackResourceManager> var3 = new HashMap<>();
      List<String> var4 = param1.stream().flatMap(l_param1 -> l_param1.getNamespaces(param0).stream()).distinct().toList();

      for (PackResources var6 : param1) {
         ResourceFilterSection var7 = this.getPackFilterSection(var6);
         Set<String> var8 = var6.getNamespaces(param0);
         Predicate<ResourceLocation> var9 = var7 != null ? l_param1 -> var7.isPathFiltered(l_param1.getPath()) : null;

         for (String var11 : var4) {
            boolean var12 = var8.contains(var11);
            boolean var13 = var7 != null && var7.isNamespaceFiltered(var11);
            if (var12 || var13) {
               FallbackResourceManager var14 = var3.get(var11);
               if (var14 == null) {
                  var14 = new FallbackResourceManager(param0, var11);
                  var3.put(var11, var14);
               }

               if (var12 && var13) {
                  var14.push(var6, var9);
               } else if (var12) {
                  var14.push(var6);
               } else {
                  var14.pushFilterOnly(var6.packId(), var9);
               }
            }
         }
      }

      this.namespacedManagers = var3;
   }

   @Nullable
   private ResourceFilterSection getPackFilterSection(PackResources param0) {
      try {
         return param0.getMetadataSection(ResourceFilterSection.TYPE);
      } catch (IOException var2) {
         LOGGER.error("Failed to get filter section from pack {}", param0.packId());
         return null;
      }
   }

   @Override
   public Set<String> getNamespaces() {
      return this.namespacedManagers.keySet();
   }

   @Override
   public Optional<Resource> getResource(ResourceLocation param0) {
      ResourceManager var2 = this.namespacedManagers.get(param0.getNamespace());
      return var2 != null ? var2.getResource(param0) : Optional.empty();
   }

   @Override
   public List<Resource> getResourceStack(ResourceLocation param0) {
      ResourceManager var2 = this.namespacedManagers.get(param0.getNamespace());
      return var2 != null ? var2.getResourceStack(param0) : List.of();
   }

   @Override
   public Map<ResourceLocation, Resource> listResources(String param0, Predicate<ResourceLocation> param1) {
      checkTrailingDirectoryPath(param0);
      Map<ResourceLocation, Resource> var3 = new TreeMap<>();

      for (FallbackResourceManager var5 : this.namespacedManagers.values()) {
         var3.putAll(var5.listResources(param0, param1));
      }

      return var3;
   }

   @Override
   public Map<ResourceLocation, List<Resource>> listResourceStacks(String param0, Predicate<ResourceLocation> param1) {
      checkTrailingDirectoryPath(param0);
      Map<ResourceLocation, List<Resource>> var3 = new TreeMap<>();

      for (FallbackResourceManager var5 : this.namespacedManagers.values()) {
         var3.putAll(var5.listResourceStacks(param0, param1));
      }

      return var3;
   }

   private static void checkTrailingDirectoryPath(String param0) {
      if (param0.endsWith("/")) {
         throw new IllegalArgumentException("Trailing slash in path " + param0);
      }
   }

   @Override
   public Stream<PackResources> listPacks() {
      return this.packs.stream();
   }

   @Override
   public void close() {
      this.packs.forEach(PackResources::close);
   }
}

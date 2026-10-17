package net.minecraft.tags;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public class TagLoader<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   final Function<ResourceLocation, Optional<? extends T>> idToValue;
   private final String directory;

   public TagLoader(Function<ResourceLocation, Optional<? extends T>> param0, String param1) {
      this.idToValue = param0;
      this.directory = param1;
   }

   public Map<ResourceLocation, List<TagLoader.EntryWithSource>> load(ResourceManager param0) {
      Map<ResourceLocation, List<TagLoader.EntryWithSource>> var2 = Maps.newHashMap();
      FileToIdConverter var3 = FileToIdConverter.json(this.directory);

      for (Entry<ResourceLocation, List<Resource>> var5 : var3.listMatchingResourceStacks(param0).entrySet()) {
         ResourceLocation var6 = var5.getKey();
         ResourceLocation var7 = var3.fileToId(var6);

         for (Resource var9 : var5.getValue()) {
            try (Reader var10 = var9.openAsReader()) {
               JsonElement var11 = JsonParser.parseReader(var10);
               List<TagLoader.EntryWithSource> var12 = var2.computeIfAbsent(var7, l_param0 -> new ArrayList<>());
               TagFile var13 = TagFile.CODEC.parse(new Dynamic<>(JsonOps.INSTANCE, var11)).getOrThrow(false, LOGGER::error);
               if (var13.replace()) {
                  var12.clear();
               }

               String var14 = var9.sourcePackId();
               var13.entries().forEach(l_param2 -> var12.add(new TagLoader.EntryWithSource(l_param2, var14)));
            } catch (Exception var10) {
               LOGGER.error("Couldn't read tag list {} from {} in data pack {}", var7, var6, var9.sourcePackId(), var10);
            }
         }
      }

      return var2;
   }

   private static void visitDependenciesAndElement(
      Map<ResourceLocation, List<TagLoader.EntryWithSource>> param0,
      Multimap<ResourceLocation, ResourceLocation> param1,
      Set<ResourceLocation> param2,
      ResourceLocation param3,
      BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>> param4
   ) {
      if (param2.add(param3)) {
         param1.get(param3).forEach(l_param4 -> visitDependenciesAndElement(param0, param1, param2, l_param4, param4));
         List<TagLoader.EntryWithSource> var5 = param0.get(param3);
         if (var5 != null) {
            param4.accept(param3, var5);
         }
      }
   }

   private static boolean isCyclic(Multimap<ResourceLocation, ResourceLocation> param0, ResourceLocation param1, ResourceLocation param2) {
      Collection<ResourceLocation> var3 = param0.get(param2);
      return var3.contains(param1) ? true : var3.stream().anyMatch(l_param2 -> isCyclic(param0, param1, l_param2));
   }

   private static void addDependencyIfNotCyclic(Multimap<ResourceLocation, ResourceLocation> param0, ResourceLocation param1, ResourceLocation param2) {
      if (!isCyclic(param0, param1, param2)) {
         param0.put(param1, param2);
      }
   }

   private Either<Collection<TagLoader.EntryWithSource>, Collection<T>> build(TagEntry.Lookup<T> param0, List<TagLoader.EntryWithSource> param1) {
      Builder<T> var3 = ImmutableSet.builder();
      List<TagLoader.EntryWithSource> var4 = new ArrayList<>();

      for (TagLoader.EntryWithSource var6 : param1) {
         if (!var6.entry().build(param0, var3::add)) {
            var4.add(var6);
         }
      }

      return var4.isEmpty() ? Either.right(var3.build()) : Either.left(var4);
   }

   public Map<ResourceLocation, Collection<T>> build(Map<ResourceLocation, List<TagLoader.EntryWithSource>> param0) {
      final Map<ResourceLocation, Collection<T>> var2 = Maps.newHashMap();
      TagEntry.Lookup<T> var3 = new TagEntry.Lookup<T>() {
         @Nullable
         @Override
         public T element(ResourceLocation l_param0) {
            return (T)TagLoader.this.idToValue.apply(l_param0).orElse(null);
         }

         @Nullable
         @Override
         public Collection<T> tag(ResourceLocation l_param0) {
            return var2.get(l_param0);
         }
      };
      Multimap<ResourceLocation, ResourceLocation> var4 = HashMultimap.create();
      param0.forEach(
         (l_param1, l_param2) -> l_param2.forEach(
            l2_param2 -> l2_param2.entry.visitRequiredDependencies(l3_param2 -> addDependencyIfNotCyclic(var4, l_param1, l3_param2))
         )
      );
      param0.forEach(
         (l_param1, l_param2) -> l_param2.forEach(
            l2_param2 -> l2_param2.entry.visitOptionalDependencies(l3_param2 -> addDependencyIfNotCyclic(var4, l_param1, l3_param2))
         )
      );
      Set<ResourceLocation> var5 = Sets.newHashSet();
      param0.keySet()
         .forEach(
            l_param5 -> visitDependenciesAndElement(
               param0,
               var4,
               var5,
               l_param5,
               (l2_param2, l2_param3) -> this.build(var3, l2_param3)
                  .ifLeft(
                     l3_param1 -> LOGGER.error(
                        "Couldn't load tag {} as it is missing following references: {}",
                        l2_param2,
                        l3_param1.stream().map(Objects::toString).collect(Collectors.joining(", "))
                     )
                  )
                  .ifRight(l3_param2 -> var2.put(l2_param2, (Collection<T>)l3_param2))
            )
         );
      return var2;
   }

   public Map<ResourceLocation, Collection<T>> loadAndBuild(ResourceManager param0) {
      return this.build(this.load(param0));
   }

   public record EntryWithSource(TagEntry entry, String source) {

      @Override
      public String toString() {
         return this.entry + " (from " + this.source + ")";
      }
   }
}

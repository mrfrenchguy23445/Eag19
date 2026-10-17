package net.minecraft.data.tags;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import org.slf4j.Logger;

public abstract class TagsProvider<T> implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final PackOutput.PathProvider pathProvider;
   private final CompletableFuture<HolderLookup.Provider> lookupProvider;
   private final CompletableFuture<Void> contentsDone = new CompletableFuture<>();
   private final CompletableFuture<TagsProvider.TagLookup<T>> parentProvider;
   protected final ResourceKey<? extends Registry<T>> registryKey;
   private final Map<ResourceLocation, TagBuilder> builders = Maps.newLinkedHashMap();

   protected TagsProvider(PackOutput param0, ResourceKey<? extends Registry<T>> param1, CompletableFuture<HolderLookup.Provider> param2) {
      this(param0, param1, param2, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()));
   }

   protected TagsProvider(
      PackOutput param0,
      ResourceKey<? extends Registry<T>> param1,
      CompletableFuture<HolderLookup.Provider> param2,
      CompletableFuture<TagsProvider.TagLookup<T>> param3
   ) {
      this.pathProvider = param0.createPathProvider(PackOutput.Target.DATA_PACK, TagManager.getTagDir(param1));
      this.registryKey = param1;
      this.parentProvider = param3;
      this.lookupProvider = param2;
   }

   @Override
   public final String getName() {
      return "Tags for " + this.registryKey.location();
   }

   protected abstract void addTags(HolderLookup.Provider var1);

   @Override
   public CompletableFuture<?> run(CachedOutput param0) {
      record CombinedData<T>(HolderLookup.Provider contents, TagsProvider.TagLookup<T> parent) {
      }

      return this.createContentsProvider()
         .thenApply(l_param0 -> {
            this.contentsDone.complete(null);
            return (HolderLookup.Provider)l_param0;
         })
         .thenCombineAsync(this.parentProvider, (l_param0, l_param1) -> new CombinedData<>(l_param0, (TagsProvider.TagLookup<T>)l_param1))
         .thenCompose(
            l_param1 -> {
               HolderLookup.RegistryLookup<T> l_var3 = l_param1.contents.lookupOrThrow(this.registryKey);
               Predicate<ResourceLocation> l_var4 = l2_param1 -> l_var3.get(ResourceKey.create(this.registryKey, l2_param1)).isPresent();
               Predicate<ResourceLocation> l_var5 = l2_param1 -> this.builders.containsKey(l2_param1)
                  || l_param1.parent.contains(TagKey.create(this.registryKey, l2_param1));
               return CompletableFuture.allOf(
                  this.builders
                     .entrySet()
                     .stream()
                     .map(
                        l2_param3 -> {
                           ResourceLocation l2_var5 = l2_param3.getKey();
                           TagBuilder l2_var6 = l2_param3.getValue();
                           List<TagEntry> l2_var7 = l2_var6.build();
                           List<TagEntry> l2_var8 = l2_var7.stream().filter(l3_param2 -> !l3_param2.verifyIfPresent(l_var4, l_var5)).toList();
                           if (!l2_var8.isEmpty()) {
                              throw new IllegalArgumentException(
                                 String.format(
                                    Locale.ROOT,
                                    "Couldn't define tag %s as it is missing following references: %s",
                                    l2_var5,
                                    l2_var8.stream().map(Objects::toString).collect(Collectors.joining(","))
                                 )
                              );
                           }

                           JsonElement l2_var9 = TagFile.CODEC.encodeStart(JsonOps.INSTANCE, new TagFile(l2_var7, false)).getOrThrow(false, LOGGER::error);
                           Path l2_var10 = this.pathProvider.json(l2_var5);
                           return DataProvider.saveStable(param0, l2_var9, l2_var10);
                        }
                     )
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   protected TagsProvider.TagAppender<T> tag(TagKey<T> param0) {
      TagBuilder var2 = this.getOrCreateRawBuilder(param0);
      return new TagsProvider.TagAppender<>(var2);
   }

   protected TagBuilder getOrCreateRawBuilder(TagKey<T> param0) {
      return this.builders.computeIfAbsent(param0.location(), l_param0 -> TagBuilder.create());
   }

   public CompletableFuture<TagsProvider.TagLookup<T>> contentsGetter() {
      return this.contentsDone.thenApply(l_param0 -> l2_param0 -> Optional.ofNullable(this.builders.get(l2_param0.location())));
   }

   protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
      return this.lookupProvider.thenApply(l_param0 -> {
         this.builders.clear();
         this.addTags(l_param0);
         return (HolderLookup.Provider)l_param0;
      });
   }

   protected static class TagAppender<T> {
      private final TagBuilder builder;

      protected TagAppender(TagBuilder param0) {
         this.builder = param0;
      }

      public final TagsProvider.TagAppender<T> add(ResourceKey<T> param0) {
         this.builder.addElement(param0.location());
         return this;
      }

      @SafeVarargs
      public final TagsProvider.TagAppender<T> add(ResourceKey<T>... param0) {
         for (ResourceKey<T> var5 : param0) {
            this.builder.addElement(var5.location());
         }

         return this;
      }

      public TagsProvider.TagAppender<T> addOptional(ResourceLocation param0) {
         this.builder.addOptionalElement(param0);
         return this;
      }

      public TagsProvider.TagAppender<T> addTag(TagKey<T> param0) {
         this.builder.addTag(param0.location());
         return this;
      }

      public TagsProvider.TagAppender<T> addOptionalTag(ResourceLocation param0) {
         this.builder.addOptionalTag(param0);
         return this;
      }
   }

   @FunctionalInterface
   public interface TagLookup<T> extends Function<TagKey<T>, Optional<TagBuilder>> {
      static <T> TagsProvider.TagLookup<T> empty() {
         return l_param0 -> Optional.empty();
      }

      default boolean contains(TagKey<T> param0) {
         return this.apply(param0).isPresent();
      }
   }
}

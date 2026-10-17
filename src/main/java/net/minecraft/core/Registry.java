package net.minecraft.core;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public interface Registry<T> extends Keyable, IdMap<T> {
   ResourceKey<? extends Registry<T>> key();

   default Codec<T> byNameCodec() {
      Codec<T> var1 = ResourceLocation.CODEC
         .flatXmap(
            l_param0 -> Optional.ofNullable(this.get(l_param0))
               .map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + this.key() + ": " + l_param0)),
            l_param0 -> this.getResourceKey((T)l_param0)
               .map(ResourceKey::location)
               .map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Unknown registry element in " + this.key() + ":" + l_param0))
         );
      Codec<T> var2 = ExtraCodecs.idResolverCodec(l_param0 -> this.getResourceKey(l_param0).isPresent() ? this.getId(l_param0) : -1, this::byId, -1);
      return ExtraCodecs.overrideLifecycle(ExtraCodecs.orCompressed(var1, var2), this::lifecycle, this::lifecycle);
   }

   default Codec<Holder<T>> holderByNameCodec() {
      Codec<Holder<T>> var1 = ResourceLocation.CODEC
         .flatXmap(
            l_param0 -> this.getHolder(ResourceKey.create(this.key(), l_param0))
               .map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + this.key() + ": " + l_param0)),
            l_param0 -> l_param0.unwrapKey()
               .map(ResourceKey::location)
               .map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Unknown registry element in " + this.key() + ":" + l_param0))
         );
      return ExtraCodecs.overrideLifecycle(var1, l_param0 -> this.lifecycle(l_param0.value()), l_param0 -> this.lifecycle(l_param0.value()));
   }

   @Override
   default <U> Stream<U> keys(DynamicOps<U> param0) {
      return this.keySet().stream().map(l_param1 -> param0.createString(l_param1.toString()));
   }

   @Nullable
   ResourceLocation getKey(T var1);

   Optional<ResourceKey<T>> getResourceKey(T var1);

   @Override
   int getId(@Nullable T var1);

   @Nullable
   T get(@Nullable ResourceKey<T> var1);

   @Nullable
   T get(@Nullable ResourceLocation var1);

   Lifecycle lifecycle(T var1);

   Lifecycle registryLifecycle();

   default Optional<T> getOptional(@Nullable ResourceLocation param0) {
      return Optional.ofNullable(this.get(param0));
   }

   default Optional<T> getOptional(@Nullable ResourceKey<T> param0) {
      return Optional.ofNullable(this.get(param0));
   }

   default T getOrThrow(ResourceKey<T> param0) {
      T var2 = this.get(param0);
      if (var2 == null) {
         throw new IllegalStateException("Missing key in " + this.key() + ": " + param0);
      } else {
         return var2;
      }
   }

   Set<ResourceLocation> keySet();

   Set<Entry<ResourceKey<T>, T>> entrySet();

   Set<ResourceKey<T>> registryKeySet();

   Optional<Holder.Reference<T>> getRandom(RandomSource var1);

   default Stream<T> stream() {
      return StreamSupport.stream(this.spliterator(), false);
   }

   boolean containsKey(ResourceLocation var1);

   boolean containsKey(ResourceKey<T> var1);

   static <T> T register(Registry<? super T> param0, String param1, T param2) {
      return register(param0, new ResourceLocation(param1), param2);
   }

   static <V, T extends V> T register(Registry<V> param0, ResourceLocation param1, T param2) {
      return register(param0, ResourceKey.create(param0.key(), param1), param2);
   }

   static <V, T extends V> T register(Registry<V> param0, ResourceKey<V> param1, T param2) {
      ((WritableRegistry)param0).register(param1, (V)param2, Lifecycle.stable());
      return param2;
   }

   static <T> Holder.Reference<T> registerForHolder(Registry<T> param0, ResourceKey<T> param1, T param2) {
      return ((WritableRegistry)param0).register(param1, param2, Lifecycle.stable());
   }

   static <T> Holder.Reference<T> registerForHolder(Registry<T> param0, ResourceLocation param1, T param2) {
      return registerForHolder(param0, ResourceKey.create(param0.key(), param1), param2);
   }

   static <V, T extends V> T registerMapping(Registry<V> param0, int param1, String param2, T param3) {
      ((WritableRegistry)param0).registerMapping(param1, ResourceKey.create(param0.key(), new ResourceLocation(param2)), (V)param3, Lifecycle.stable());
      return param3;
   }

   Registry<T> freeze();

   Holder.Reference<T> createIntrusiveHolder(T var1);

   Optional<Holder.Reference<T>> getHolder(int var1);

   Optional<Holder.Reference<T>> getHolder(ResourceKey<T> var1);

   Holder<T> wrapAsHolder(T var1);

   default Holder.Reference<T> getHolderOrThrow(ResourceKey<T> param0) {
      return this.getHolder(param0).orElseThrow(() -> new IllegalStateException("Missing key in " + this.key() + ": " + param0));
   }

   Stream<Holder.Reference<T>> holders();

   Optional<HolderSet.Named<T>> getTag(TagKey<T> var1);

   default Iterable<Holder<T>> getTagOrEmpty(TagKey<T> param0) {
      return DataFixUtils.orElse(this.getTag(param0), List.<Holder<T>>of());
   }

   HolderSet.Named<T> getOrCreateTag(TagKey<T> var1);

   Stream<Pair<TagKey<T>, HolderSet.Named<T>>> getTags();

   Stream<TagKey<T>> getTagNames();

   void resetTags();

   void bindTags(Map<TagKey<T>, List<Holder<T>>> var1);

   default IdMap<Holder<T>> asHolderIdMap() {
      return new IdMap<Holder<T>>() {
         public int getId(Holder<T> l_param0) {
            return Registry.this.getId(l_param0.value());
         }

         @Nullable
         public Holder<T> byId(int l_param0) {
            return (Holder<T>)Registry.this.getHolder(l_param0).orElse(null);
         }

         @Override
         public int size() {
            return Registry.this.size();
         }

         @Override
         public Iterator<Holder<T>> iterator() {
            return Registry.this.holders().map(l2_param0 -> (Holder<T>)l2_param0).iterator();
         }
      };
   }

   HolderOwner<T> holderOwner();

   HolderLookup.RegistryLookup<T> asLookup();

   default HolderLookup.RegistryLookup<T> asTagAddingLookup() {
      return new HolderLookup.RegistryLookup.Delegate<T>() {
         @Override
         protected HolderLookup.RegistryLookup<T> parent() {
            return Registry.this.asLookup();
         }

         @Override
         public Optional<HolderSet.Named<T>> get(TagKey<T> l_param0) {
            return Optional.of(this.getOrThrow(l_param0));
         }

         @Override
         public HolderSet.Named<T> getOrThrow(TagKey<T> l_param0) {
            return Registry.this.getOrCreateTag(l_param0);
         }
      };
   }
}

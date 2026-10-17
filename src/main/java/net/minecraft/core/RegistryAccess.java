package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

public interface RegistryAccess extends HolderLookup.Provider {
   Logger LOGGER = LogUtils.getLogger();
   RegistryAccess.Frozen EMPTY = new RegistryAccess.ImmutableRegistryAccess(Map.of()).freeze();

   <E> Optional<Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> var1);

   @Override
   default <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> param0) {
      return this.registry(param0).map(Registry::asLookup);
   }

   default <E> Registry<E> registryOrThrow(ResourceKey<? extends Registry<? extends E>> param0) {
      return this.registry(param0).orElseThrow(() -> new IllegalStateException("Missing registry: " + param0));
   }

   Stream<RegistryAccess.RegistryEntry<?>> registries();

   static RegistryAccess.Frozen fromRegistryOfRegistries(final Registry<? extends Registry<?>> param0) {
      return new RegistryAccess.Frozen() {
         @Override
         public <T> Optional<Registry<T>> registry(ResourceKey<? extends Registry<? extends T>> l_param0) {
            Registry<Registry<T>> l_var2 = (Registry<Registry<T>>)param0;
            return l_var2.getOptional((ResourceKey<Registry<T>>)l_param0);
         }

         @Override
         public Stream<RegistryAccess.RegistryEntry<?>> registries() {
            return param0.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
         }

         @Override
         public RegistryAccess.Frozen freeze() {
            return this;
         }
      };
   }

   default RegistryAccess.Frozen freeze() {
      class FrozenAccess extends RegistryAccess.ImmutableRegistryAccess implements RegistryAccess.Frozen {
         protected FrozenAccess(Stream<RegistryAccess.RegistryEntry<?>> l_param1) {
            super(l_param1);
         }
      }

      return new FrozenAccess(this.registries().map(RegistryAccess.RegistryEntry::freeze));
   }

   default Lifecycle allRegistriesLifecycle() {
      return this.registries().map(l_param0 -> l_param0.value.registryLifecycle()).reduce(Lifecycle.stable(), Lifecycle::add);
   }

   interface Frozen extends RegistryAccess {
   }

   class ImmutableRegistryAccess implements RegistryAccess {
      private final Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> registries;

      public ImmutableRegistryAccess(List<? extends Registry<?>> param0) {
         this.registries = param0.stream().collect(Collectors.toUnmodifiableMap(Registry::key, l_param0 -> l_param0));
      }

      public ImmutableRegistryAccess(Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> param0) {
         this.registries = Map.copyOf(param0);
      }

      public ImmutableRegistryAccess(Stream<RegistryAccess.RegistryEntry<?>> param0) {
         this.registries = param0.collect(ImmutableMap.toImmutableMap(RegistryAccess.RegistryEntry::key, RegistryAccess.RegistryEntry::value));
      }

      @Override
      public <E> Optional<Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> param0) {
         return Optional.ofNullable(this.registries.get(param0)).map(l_param0 -> (Registry<E>)l_param0);
      }

      @Override
      public Stream<RegistryAccess.RegistryEntry<?>> registries() {
         return this.registries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
      }
   }

   record RegistryEntry<T>(ResourceKey<? extends Registry<T>> key, Registry<T> value) {

      private static <T, R extends Registry<? extends T>> RegistryAccess.RegistryEntry<T> fromMapEntry(
         Entry<? extends ResourceKey<? extends Registry<?>>, R> param0
      ) {
         return fromUntyped((ResourceKey<? extends Registry<?>>)param0.getKey(), param0.getValue());
      }

      private static <T> RegistryAccess.RegistryEntry<T> fromUntyped(ResourceKey<? extends Registry<?>> param0, Registry<?> param1) {
         return new RegistryAccess.RegistryEntry<>((ResourceKey<? extends Registry<T>>)param0, (Registry<T>)param1);
      }

      private RegistryAccess.RegistryEntry<T> freeze() {
         return new RegistryAccess.RegistryEntry<>(this.key, this.value.freeze());
      }
   }
}

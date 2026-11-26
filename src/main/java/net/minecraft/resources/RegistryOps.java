package net.minecraft.resources;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.util.ExtraCodecs;

public class RegistryOps<T> extends DelegatingOps<T> {
   private final RegistryOps.RegistryInfoLookup lookupProvider;

   private static RegistryOps.RegistryInfoLookup memoizeLookup(final RegistryOps.RegistryInfoLookup param0) {
      return new RegistryOps.RegistryInfoLookup() {
         private final Map<ResourceKey<? extends Registry<?>>, Optional<? extends RegistryOps.RegistryInfo<?>>> lookups = new HashMap<>();

         @Override
         public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> l_param0) {
            return (Optional<RegistryOps.RegistryInfo<T>>)this.lookups.computeIfAbsent(l_param0, param0::lookup);
         }
      };
   }

   public static <T> RegistryOps<T> create(DynamicOps<T> param0, final HolderLookup.Provider param1) {
      return create(param0, memoizeLookup(new RegistryOps.RegistryInfoLookup() {
         @Override
         public <E> Optional<RegistryOps.RegistryInfo<E>> lookup(ResourceKey<? extends Registry<? extends E>> l_param0) {
            return param1.lookup(l_param0).map(l2_param0 -> new RegistryOps.RegistryInfo<>(l2_param0, l2_param0, l2_param0.registryLifecycle()));
         }
      }));
   }

   public static <T> RegistryOps<T> create(DynamicOps<T> param0, RegistryOps.RegistryInfoLookup param1) {
      return new RegistryOps<>(param0, param1);
   }

   private RegistryOps(DynamicOps<T> param0, RegistryOps.RegistryInfoLookup param1) {
      super(param0);
      this.lookupProvider = param1;
   }

   public <E> Optional<HolderOwner<E>> owner(ResourceKey<? extends Registry<? extends E>> param0) {
      return this.lookupProvider.lookup(param0).map(RegistryOps.RegistryInfo::owner);
   }

   public <E> Optional<HolderGetter<E>> getter(ResourceKey<? extends Registry<? extends E>> param0) {
      return this.lookupProvider.lookup(param0).map(RegistryOps.RegistryInfo::getter);
   }

   public static <E, O> RecordCodecBuilder<O, HolderGetter<E>> retrieveGetter(ResourceKey<? extends Registry<? extends E>> param0) {
      return ExtraCodecs.retrieveContext(
            l_param1 -> l_param1 instanceof RegistryOps<?> l_var2
               ? l_var2.lookupProvider
                  .lookup(param0)
                  .map(l2_param0 -> DataResult.success(l2_param0.getter(), l2_param0.elementsLifecycle()))
                  .orElseGet(() -> DataResult.error(() -> "Unknown registry: " + param0))
               : DataResult.error(() -> "Not a registry ops")
         )
         .forGetter(l_param0 -> null);
   }

   public static <E, O> RecordCodecBuilder<O, Holder.Reference<E>> retrieveElement(ResourceKey<E> param0) {
      ResourceKey<? extends Registry<E>> var1 = ResourceKey.createRegistryKey(param0.registry());
      return ExtraCodecs.retrieveContext(
            l_param2 -> l_param2 instanceof RegistryOps<?> l_var3
               ? l_var3.lookupProvider
                  .lookup(var1)
                  .flatMap(l2_param1 -> l2_param1.getter().get(param0))
                  .map(DataResult::success)
                  .orElseGet(() -> DataResult.error(() -> "Can't find value: " + param0))
               : DataResult.error(() -> "Not a registry ops")
         )
         .forGetter(l_param0 -> null);
   }

   public record RegistryInfo<T>(HolderOwner<T> owner, HolderGetter<T> getter, Lifecycle elementsLifecycle) {
   }

   public interface RegistryInfoLookup {
      <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1);
   }
}

package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;

public final class RegistryFileCodec<E> implements Codec<Holder<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;
   private final Codec<E> elementCodec;
   private final boolean allowInline;

   public static <E> RegistryFileCodec<E> create(ResourceKey<? extends Registry<E>> param0, Codec<E> param1) {
      return create(param0, param1, true);
   }

   public static <E> RegistryFileCodec<E> create(ResourceKey<? extends Registry<E>> param0, Codec<E> param1, boolean param2) {
      return new RegistryFileCodec<>(param0, param1, param2);
   }

   private RegistryFileCodec(ResourceKey<? extends Registry<E>> param0, Codec<E> param1, boolean param2) {
      this.registryKey = param0;
      this.elementCodec = param1;
      this.allowInline = param2;
   }

   public <T> DataResult<T> encode(Holder<E> param0, DynamicOps<T> param1, T param2) {
      if (param1 instanceof RegistryOps<?> var4) {
         Optional<HolderOwner<E>> var5 = var4.owner(this.registryKey);
         if (var5.isPresent()) {
            if (!param0.canSerializeIn(var5.get())) {
               return DataResult.error(() -> "Element " + param0 + " is not valid in current registry set");
            }

            return param0.unwrap()
               .map(
                  l_param2 -> ResourceLocation.CODEC.encode(l_param2.location(), param1, param2),
                  l_param2 -> this.elementCodec.encode((E)l_param2, param1, param2)
               );
         }
      }

      return this.elementCodec.encode(param0.value(), param1, param2);
   }

   @Override
   public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> param0, T param1) {
      if (param0 instanceof RegistryOps<?> var3) {
         Optional<HolderGetter<E>> var4 = var3.getter(this.registryKey);
         if (var4.isEmpty()) {
            return DataResult.error(() -> "Registry does not exist: " + this.registryKey);
         }

         HolderGetter<E> var5 = var4.get();
         DataResult<Pair<ResourceLocation, T>> var6 = ResourceLocation.CODEC.decode(param0, param1);
         if (var6.result().isEmpty()) {
            return !this.allowInline
               ? DataResult.error(() -> "Inline definitions not allowed here")
               : this.elementCodec.decode(param0, param1).map(l_param0 -> l_param0.mapFirst(Holder::direct));
         }

         Pair<ResourceLocation, T> var7 = var6.result().get();
         ResourceKey<E> var8 = ResourceKey.create(this.registryKey, var7.getFirst());
         return var5.get(var8)
            .map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Failed to get element " + var8))
            .<Pair<Holder<E>, T>>map(l_param1 -> Pair.of(l_param1, var7.getSecond()))
            .setLifecycle(Lifecycle.stable());
      } else {
         return this.elementCodec.decode(param0, param1).map(l_param0 -> l_param0.mapFirst(Holder::direct));
      }
   }

   @Override
   public String toString() {
      return "RegistryFileCodec[" + this.registryKey + " " + this.elementCodec + "]";
   }
}

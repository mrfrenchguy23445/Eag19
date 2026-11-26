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

public final class RegistryFixedCodec<E> implements Codec<Holder<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;

   public static <E> RegistryFixedCodec<E> create(ResourceKey<? extends Registry<E>> param0) {
      return new RegistryFixedCodec<>(param0);
   }

   private RegistryFixedCodec(ResourceKey<? extends Registry<E>> param0) {
      this.registryKey = param0;
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
                  l_param0 -> DataResult.error(() -> "Elements from registry " + this.registryKey + " can't be serialized to a value")
               );
         }
      }

      return DataResult.error(() -> "Can't access registry " + this.registryKey);
   }

   @Override
   public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> param0, T param1) {
      if (param0 instanceof RegistryOps<?> var3) {
         Optional<HolderGetter<E>> var4 = var3.getter(this.registryKey);
         if (var4.isPresent()) {
            return ResourceLocation.CODEC
               .decode(param0, param1)
               .flatMap(
                  l_param1 -> {
                     ResourceLocation l_var3 = l_param1.getFirst();
                     return var4.get()
                        .get(ResourceKey.create(this.registryKey, l_var3))
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Failed to get element " + l_var3))
                        .<Pair<Holder<E>, T>>map(l2_param1 -> Pair.of(l2_param1, (T)l_param1.getSecond()))
                        .setLifecycle(Lifecycle.stable());
                  }
               );
         }
      }

      return DataResult.error(() -> "Can't access registry " + this.registryKey);
   }

   @Override
   public String toString() {
      return "RegistryFixedCodec[" + this.registryKey + "]";
   }
}

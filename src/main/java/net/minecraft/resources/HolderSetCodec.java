package net.minecraft.resources;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;

public class HolderSetCodec<E> implements Codec<HolderSet<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;
   private final Codec<Holder<E>> elementCodec;
   private final Codec<List<Holder<E>>> homogenousListCodec;
   private final Codec<Either<TagKey<E>, List<Holder<E>>>> registryAwareCodec;

   private static <E> Codec<List<Holder<E>>> homogenousList(Codec<Holder<E>> param0, boolean param1) {
      Codec<List<Holder<E>>> var2 = ExtraCodecs.validate(param0.listOf(), ExtraCodecs.ensureHomogenous(Holder::kind));
      return param1
         ? var2
         : Codec.either(var2, param0)
            .xmap(
               l_param0 -> l_param0.map(l2_param0 -> l2_param0, List::of),
               l_param0 -> l_param0.size() == 1 ? Either.right(l_param0.get(0)) : Either.left((List<Holder<E>>)l_param0)
            );
   }

   public static <E> Codec<HolderSet<E>> create(ResourceKey<? extends Registry<E>> param0, Codec<Holder<E>> param1, boolean param2) {
      return new HolderSetCodec<>(param0, param1, param2);
   }

   private HolderSetCodec(ResourceKey<? extends Registry<E>> param0, Codec<Holder<E>> param1, boolean param2) {
      this.registryKey = param0;
      this.elementCodec = param1;
      this.homogenousListCodec = homogenousList(param1, param2);
      this.registryAwareCodec = Codec.either(TagKey.hashedCodec(param0), this.homogenousListCodec);
   }

   @Override
   public <T> DataResult<Pair<HolderSet<E>, T>> decode(DynamicOps<T> param0, T param1) {
      if (param0 instanceof RegistryOps<T> var3) {
         Optional<HolderGetter<E>> var4 = var3.getter(this.registryKey);
         if (var4.isPresent()) {
            HolderGetter<E> var5 = var4.get();
            return this.registryAwareCodec
               .decode(param0, param1)
               .map(l_param1 -> l_param1.mapFirst(l2_param1 -> l2_param1.map(var5::getOrThrow, HolderSet::direct)));
         }
      }

      return this.decodeWithoutRegistry(param0, param1);
   }

   public <T> DataResult<T> encode(HolderSet<E> param0, DynamicOps<T> param1, T param2) {
      if (param1 instanceof RegistryOps<T> var4) {
         Optional<HolderOwner<E>> var5 = var4.owner(this.registryKey);
         if (var5.isPresent()) {
            if (!param0.canSerializeIn(var5.get())) {
               return DataResult.error(() -> "HolderSet " + param0 + " is not valid in current registry set");
            }

            return this.registryAwareCodec.encode(param0.unwrap().mapRight(List::copyOf), param1, param2);
         }
      }

      return this.encodeWithoutRegistry(param0, param1, param2);
   }

   private <T> DataResult<Pair<HolderSet<E>, T>> decodeWithoutRegistry(DynamicOps<T> param0, T param1) {
      return this.elementCodec.listOf().decode(param0, param1).flatMap(l_param0 -> {
         List<Holder.Direct<E>> l_var1 = new ArrayList<>();

         for (Holder<E> l_var3 : l_param0.getFirst()) {
            if (!(l_var3 instanceof Holder.Direct<E> l_var4)) {
               return DataResult.error(() -> "Can't decode element " + l_var3 + " without registry");
            }

            l_var1.add(l_var4);
         }

         return DataResult.success(new Pair<>(HolderSet.direct(l_var1), l_param0.getSecond()));
      });
   }

   private <T> DataResult<T> encodeWithoutRegistry(HolderSet<E> param0, DynamicOps<T> param1, T param2) {
      return this.homogenousListCodec.encode(param0.stream().toList(), param1, param2);
   }
}

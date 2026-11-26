package net.minecraft.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;

public class RegistryCodecs {
   private static <T> MapCodec<RegistryCodecs.RegistryEntry<T>> withNameAndId(ResourceKey<? extends Registry<T>> param0, MapCodec<T> param1) {
      return RecordCodecBuilder.mapCodec(
         l_param2 -> l_param2.group(
               ResourceKey.codec(param0).fieldOf("name").forGetter(RegistryCodecs.RegistryEntry::key),
               Codec.INT.fieldOf("id").forGetter(RegistryCodecs.RegistryEntry::id),
               param1.forGetter(RegistryCodecs.RegistryEntry::value)
            )
            .apply(l_param2, RegistryCodecs.RegistryEntry::new)
      );
   }

   public static <T> Codec<Registry<T>> networkCodec(ResourceKey<? extends Registry<T>> param0, Lifecycle param1, Codec<T> param2) {
      return withNameAndId(param0, param2.fieldOf("element")).codec().listOf().xmap(l_param2 -> {
         WritableRegistry<T> l_var3 = new MappedRegistry<>(param0, param1);

         for (RegistryCodecs.RegistryEntry<T> l_var5 : l_param2) {
            l_var3.registerMapping(l_var5.id(), l_var5.key(), l_var5.value(), param1);
         }

         return l_var3;
      }, l_param0 -> {
         Builder<RegistryCodecs.RegistryEntry<T>> l_var1 = ImmutableList.builder();

         for (T l_var3 : l_param0) {
            l_var1.add(new RegistryCodecs.RegistryEntry<>(l_param0.getResourceKey(l_var3).get(), l_param0.getId(l_var3), l_var3));
         }

         return l_var1.build();
      });
   }

   public static <E> Codec<Registry<E>> fullCodec(ResourceKey<? extends Registry<E>> param0, Lifecycle param1, Codec<E> param2) {
      Codec<Map<ResourceKey<E>, E>> var3 = Codec.unboundedMap(ResourceKey.codec(param0), param2);
      return var3.xmap(l_param2 -> {
         WritableRegistry<E> l_var3 = new MappedRegistry<>(param0, param1);
         l_param2.forEach((l2_param2, l2_param3) -> l_var3.register((ResourceKey<E>)l2_param2, (E)l2_param3, param1));
         return l_var3.freeze();
      }, l_param0 -> ImmutableMap.copyOf(l_param0.entrySet()));
   }

   public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> param0, Codec<E> param1) {
      return homogeneousList(param0, param1, false);
   }

   public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> param0, Codec<E> param1, boolean param2) {
      return HolderSetCodec.create(param0, RegistryFileCodec.create(param0, param1), param2);
   }

   public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> param0) {
      return homogeneousList(param0, false);
   }

   public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> param0, boolean param1) {
      return HolderSetCodec.create(param0, RegistryFixedCodec.create(param0), param1);
   }

   record RegistryEntry<T>(ResourceKey<T> key, int id, T value) {
   }
}

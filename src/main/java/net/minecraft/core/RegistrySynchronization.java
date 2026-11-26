package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

public class RegistrySynchronization {
   private static final Map<ResourceKey<? extends Registry<?>>, RegistrySynchronization.NetworkedRegistryData<?>> NETWORKABLE_REGISTRIES = Util.make(() -> {
      Builder<ResourceKey<? extends Registry<?>>, RegistrySynchronization.NetworkedRegistryData<?>> var0 = ImmutableMap.builder();
      put(var0, Registries.BIOME, Biome.NETWORK_CODEC);
      put(var0, Registries.CHAT_TYPE, ChatType.CODEC);
      put(var0, Registries.TRIM_PATTERN, TrimPattern.DIRECT_CODEC);
      put(var0, Registries.TRIM_MATERIAL, TrimMaterial.DIRECT_CODEC);
      put(var0, Registries.DIMENSION_TYPE, DimensionType.DIRECT_CODEC);
      put(var0, Registries.DAMAGE_TYPE, DamageType.CODEC);
      return var0.build();
   });
   public static final Codec<RegistryAccess> NETWORK_CODEC = makeNetworkCodec();

   private static <E> void put(
      Builder<ResourceKey<? extends Registry<?>>, RegistrySynchronization.NetworkedRegistryData<?>> param0,
      ResourceKey<? extends Registry<E>> param1,
      Codec<E> param2
   ) {
      param0.put(param1, new RegistrySynchronization.NetworkedRegistryData<>(param1, param2));
   }

   private static Stream<RegistryAccess.RegistryEntry<?>> ownedNetworkableRegistries(RegistryAccess param0) {
      return param0.registries().filter(l_param0 -> NETWORKABLE_REGISTRIES.containsKey(l_param0.key()));
   }

   private static <E> DataResult<? extends Codec<E>> getNetworkCodec(ResourceKey<? extends Registry<E>> param0) {
      // NETWORKABLE_REGISTRIES is Map<ResourceKey<? extends Registry<?>>, NetworkedRegistryData<?>>,
      // so the lookup yields a capture unrelated to E; the cast is erased at runtime.
      return (DataResult<? extends Codec<E>>)(DataResult<?>)
         Optional.ofNullable(NETWORKABLE_REGISTRIES.get(param0))
         .map(l_param0 -> l_param0.networkCodec())
         .map(DataResult::success)
         .orElseGet(() -> DataResult.error(() -> "Unknown or not serializable registry: " + param0));
   }

   private static <E> Codec<RegistryAccess> makeNetworkCodec() {
      Codec<ResourceKey<? extends Registry<E>>> var0 = ResourceLocation.CODEC.xmap(ResourceKey::createRegistryKey, ResourceKey::location);
      Codec<Registry<E>> var1 = var0.partialDispatch(
         "type",
         l_param0 -> DataResult.success(l_param0.key()),
         l_param0 -> getNetworkCodec((ResourceKey<? extends Registry<E>>)l_param0)
            .map(l2_param1 -> RegistryCodecs.networkCodec((ResourceKey<? extends Registry<E>>)l_param0, Lifecycle.experimental(), (Codec<E>)l2_param1))
      );
      UnboundedMapCodec<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> var2 = Codec.unboundedMap(var0, var1);
      return captureMap(var2);
   }

   private static <K extends ResourceKey<? extends Registry<?>>, V extends Registry<?>> Codec<RegistryAccess> captureMap(UnboundedMapCodec<K, V> param0) {
      return param0.xmap(
         // ImmutableRegistryAccess has List/Map/Stream constructors, so ::new was ambiguous;
         // the lambda pins the Map overload that xmap needs.
         l_param0 -> new RegistryAccess.ImmutableRegistryAccess(l_param0),
         l_param0 -> (Map<K, V>)(Map<?, ?>)ownedNetworkableRegistries(l_param0).collect(ImmutableMap.toImmutableMap(l2_param0 -> l2_param0.key(), l2_param0 -> l2_param0.value()))
      );
   }

   public static Stream<RegistryAccess.RegistryEntry<?>> networkedRegistries(LayeredRegistryAccess<RegistryLayer> param0) {
      return ownedNetworkableRegistries(param0.getAccessFrom(RegistryLayer.WORLDGEN));
   }

   public static Stream<RegistryAccess.RegistryEntry<?>> networkSafeRegistries(LayeredRegistryAccess<RegistryLayer> param0) {
      Stream<RegistryAccess.RegistryEntry<?>> var1 = param0.getLayer(RegistryLayer.STATIC).registries();
      Stream<RegistryAccess.RegistryEntry<?>> var2 = networkedRegistries(param0);
      return Stream.concat(var2, var1);
   }

   record NetworkedRegistryData<E>(ResourceKey<? extends Registry<E>> key, Codec<E> networkCodec) {
   }
}

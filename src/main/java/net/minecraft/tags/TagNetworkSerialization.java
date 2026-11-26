package net.minecraft.tags;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;

public class TagNetworkSerialization {
   public static Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> serializeTagsToNetwork(
      LayeredRegistryAccess<RegistryLayer> param0
   ) {
      return RegistrySynchronization.networkSafeRegistries(param0)
         .map(l_param0 -> Pair.of(l_param0.key(), serializeToNetwork(l_param0.value())))
         .filter(l_param0 -> !l_param0.getSecond().isEmpty())
         .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
   }

   private static <T> TagNetworkSerialization.NetworkPayload serializeToNetwork(Registry<T> param0) {
      Map<ResourceLocation, IntList> var1 = new HashMap<>();
      param0.getTags().forEach(l_param2 -> {
         HolderSet<T> l_var3 = l_param2.getSecond();
         IntList l_var4 = new IntArrayList(l_var3.size());

         for (Holder<T> l_var6 : l_var3) {
            if (l_var6.kind() != Holder.Kind.REFERENCE) {
               throw new IllegalStateException("Can't serialize unregistered value " + l_var6);
            }

            l_var4.add(param0.getId(l_var6.value()));
         }

         var1.put(l_param2.getFirst().location(), l_var4);
      });
      return new TagNetworkSerialization.NetworkPayload(var1);
   }

   public static <T> void deserializeTagsFromNetwork(
      ResourceKey<? extends Registry<T>> param0, Registry<T> param1, TagNetworkSerialization.NetworkPayload param2, TagNetworkSerialization.TagOutput<T> param3
   ) {
      param2.tags.forEach((l_param3, l_param4) -> {
         TagKey<T> l_var5 = TagKey.create(param0, l_param3);
         // Port note (wasm19): IntList.intStream() is not implemented in this classlib; the values are
         // walked by index, mapped to holders the same way, and wrapped unmodifiable to match the
         // Collectors.toUnmodifiableList() the stream form returned.
         List<Holder<T>> l_var7 = new ArrayList<>(l_param4.size());
         for (int i = 0; i < l_param4.size(); ++i) {
            param1.getHolder(l_param4.getInt(i)).ifPresent(l_var7::add);
         }
         param3.accept(l_var5, Collections.unmodifiableList(l_var7));
      });
   }

   public static final class NetworkPayload {
      final Map<ResourceLocation, IntList> tags;

      NetworkPayload(Map<ResourceLocation, IntList> param0) {
         this.tags = param0;
      }

      public void write(FriendlyByteBuf param0) {
         param0.writeMap(this.tags, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeIntIdList);
      }

      public static TagNetworkSerialization.NetworkPayload read(FriendlyByteBuf param0) {
         return new TagNetworkSerialization.NetworkPayload(param0.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readIntIdList));
      }

      public boolean isEmpty() {
         return this.tags.isEmpty();
      }
   }

   @FunctionalInterface
   public interface TagOutput<T> {
      void accept(TagKey<T> var1, List<Holder<T>> var2);
   }
}

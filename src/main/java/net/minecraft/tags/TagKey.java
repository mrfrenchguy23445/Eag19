package net.minecraft.tags;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public record TagKey<T>(ResourceKey<? extends Registry<T>> registry, ResourceLocation location) {
   private static final Interner<TagKey<?>> VALUES = Interners.newWeakInterner();

   @Deprecated
   public TagKey {
   }

   public static <T> Codec<TagKey<T>> codec(ResourceKey<? extends Registry<T>> param0) {
      return ResourceLocation.CODEC.xmap(l_param1 -> create(param0, l_param1), TagKey::location);
   }

   public static <T> Codec<TagKey<T>> hashedCodec(ResourceKey<? extends Registry<T>> param0) {
      return Codec.STRING
         .comapFlatMap(
            l_param1 -> l_param1.startsWith("#")
               ? ResourceLocation.read(l_param1.substring(1)).map(l2_param1 -> create(param0, l2_param1))
               : DataResult.error(() -> "Not a tag id"),
            l_param0 -> "#" + l_param0.location
         );
   }

   public static <T> TagKey<T> create(ResourceKey<? extends Registry<T>> param0, ResourceLocation param1) {
      return (TagKey<T>)VALUES.intern(new TagKey<>(param0, param1));
   }

   public boolean isFor(ResourceKey<? extends Registry<?>> param0) {
      return this.registry == param0;
   }

   public <E> Optional<TagKey<E>> cast(ResourceKey<? extends Registry<E>> param0) {
      return this.isFor(param0) ? Optional.of((TagKey<E>)this) : Optional.empty();
   }

   @Override
   public String toString() {
      return "TagKey[" + this.registry.location() + " / " + this.location + "]";
   }
}

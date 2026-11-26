package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public interface StringRepresentable {
   int PRE_BUILT_MAP_THRESHOLD = 16;

   String getSerializedName();

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnum(Supplier<E[]> param0) {
      return fromEnumWithMapping(param0, l_param0 -> l_param0);
   }

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnumWithMapping(Supplier<E[]> param0, Function<String, String> param1) {
      E[] var2 = (E[])param0.get();
      if (var2.length > 16) {
         Map<String, E> var3 = Arrays.stream(var2).collect(Collectors.toMap(l_param1 -> param1.apply(l_param1.getSerializedName()), l_param0 -> (E)l_param0));
         return new StringRepresentable.EnumCodec<>(var2, l_param1 -> l_param1 == null ? null : var3.get(l_param1));
      } else {
         return new StringRepresentable.EnumCodec<>(var2, l_param2 -> {
            for (E l_var6 : var2) {
               if (param1.apply(l_var6.getSerializedName()).equals(l_param2)) {
                  return l_var6;
               }
            }

            return null;
         });
      }
   }

   static Keyable keys(final StringRepresentable[] param0) {
      return new Keyable() {
         @Override
         public <T> Stream<T> keys(DynamicOps<T> l_param0) {
            return Arrays.stream(param0).map(StringRepresentable::getSerializedName).map(l_param0::createString);
         }
      };
   }

   @Deprecated
   class EnumCodec<E extends Enum<E> & StringRepresentable> implements Codec<E> {
      private final Codec<E> codec;
      private final Function<String, E> resolver;

      public EnumCodec(E[] param0, Function<String, E> param1) {
         this.codec = ExtraCodecs.orCompressed(
            ExtraCodecs.stringResolverCodec(l_param0 -> l_param0.getSerializedName(), param1),
            ExtraCodecs.idResolverCodec(l_param0 -> l_param0.ordinal(), l_param1 -> l_param1 >= 0 && l_param1 < param0.length ? param0[l_param1] : null, -1)
         );
         this.resolver = param1;
      }

      @Override
      public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> param0, T param1) {
         return this.codec.decode(param0, param1);
      }

      public <T> DataResult<T> encode(E param0, DynamicOps<T> param1, T param2) {
         return this.codec.encode(param0, param1, param2);
      }

      @Nullable
      public E byName(@Nullable String param0) {
         return this.resolver.apply(param0);
      }

      public E byName(@Nullable String param0, E param1) {
         return Objects.requireNonNullElse(this.byName(param0), param1);
      }
   }
}

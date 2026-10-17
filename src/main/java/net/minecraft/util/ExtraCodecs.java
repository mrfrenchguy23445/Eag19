package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.Codec.ResultFunction;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.HolderSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ExtraCodecs {
   public static final Codec<JsonElement> JSON = Codec.PASSTHROUGH
      .xmap(param0 -> param0.convert(JsonOps.INSTANCE).getValue(), param0 -> new Dynamic<>(JsonOps.INSTANCE, param0));
   public static final Codec<Component> COMPONENT = JSON.flatXmap(param0 -> {
      try {
         return DataResult.success(Component.Serializer.fromJson(param0));
      } catch (JsonParseException var1) {
         return DataResult.error(var1::getMessage);
      }
   }, param0 -> {
      try {
         return DataResult.success(Component.Serializer.toJsonTree(param0));
      } catch (IllegalArgumentException var1) {
         return DataResult.error(var1::getMessage);
      }
   });
   public static final Codec<Vector3f> VECTOR3F = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         param0 -> Util.fixedSize((List<Float>)param0, 3).map(l_param0 -> new Vector3f(l_param0.get(0), l_param0.get(1), l_param0.get(2))),
         param0 -> List.of(param0.x(), param0.y(), param0.z())
      );
   public static final Codec<Quaternionf> QUATERNIONF_COMPONENTS = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         param0 -> Util.fixedSize((List<Float>)param0, 4).map(l_param0 -> new Quaternionf(l_param0.get(0), l_param0.get(1), l_param0.get(2), l_param0.get(3))),
         param0 -> List.of(param0.x, param0.y, param0.z, param0.w)
      );
   public static final Codec<AxisAngle4f> AXISANGLE4F = RecordCodecBuilder.create(
      param0 -> param0.group(
            Codec.FLOAT.fieldOf("angle").forGetter(l_param0 -> l_param0.angle),
            VECTOR3F.fieldOf("axis").forGetter(l_param0 -> new Vector3f(l_param0.x, l_param0.y, l_param0.z))
         )
         .apply(param0, AxisAngle4f::new)
   );
   public static final Codec<Quaternionf> QUATERNIONF = Codec.either(QUATERNIONF_COMPONENTS, AXISANGLE4F.xmap(Quaternionf::new, AxisAngle4f::new))
      .xmap(param0 -> param0.map(l_param0 -> (Quaternionf)l_param0, l_param0 -> (Quaternionf)l_param0), Either::left);
   public static Codec<Matrix4f> MATRIX4F = Codec.FLOAT.listOf().comapFlatMap(param0 -> Util.fixedSize((List<Float>)param0, 16).map(l_param0 -> {
      Matrix4f l_var1 = new Matrix4f();

      for (int l_var2 = 0; l_var2 < l_param0.size(); l_var2++) {
         l_var1.setRowColumn(l_var2 >> 2, l_var2 & 3, l_param0.get(l_var2));
      }

      return l_var1.determineProperties();
   }), param0 -> {
      FloatList var1 = new FloatArrayList(16);

      for (int var2 = 0; var2 < 16; var2++) {
         var1.add(param0.getRowColumn(var2 >> 2, var2 & 3));
      }

      return var1;
   });
   public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, param0 -> "Value must be non-negative: " + param0);
   public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, param0 -> "Value must be positive: " + param0);
   public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(0.0F, Float.MAX_VALUE, param0 -> "Value must be positive: " + param0);
   public static final Codec<Pattern> PATTERN = Codec.STRING.comapFlatMap(param0 -> {
      try {
         return DataResult.success(Pattern.compile(param0));
      } catch (PatternSyntaxException var1) {
         return DataResult.error(() -> "Invalid regex pattern '" + param0 + "': " + var1.getMessage());
      }
   }, Pattern::pattern);
   public static final Codec<Instant> INSTANT_ISO8601 = instantCodec(DateTimeFormatter.ISO_INSTANT);
   public static final Codec<byte[]> BASE64_STRING = Codec.STRING.comapFlatMap(param0 -> {
      try {
         return DataResult.success(Base64.getDecoder().decode(param0));
      } catch (IllegalArgumentException var1) {
         return DataResult.error(() -> "Malformed base64 string");
      }
   }, param0 -> Base64.getEncoder().encodeToString(param0));
   public static final Codec<ExtraCodecs.TagOrElementLocation> TAG_OR_ELEMENT_ID = Codec.STRING
      .comapFlatMap(
         param0 -> param0.startsWith("#")
            ? ResourceLocation.read(param0.substring(1)).map(l_param0 -> new ExtraCodecs.TagOrElementLocation(l_param0, true))
            : ResourceLocation.read(param0).map(l_param0 -> new ExtraCodecs.TagOrElementLocation(l_param0, false)),
         ExtraCodecs.TagOrElementLocation::decoratedId
      );
   public static final Function<Optional<Long>, OptionalLong> toOptionalLong = param0 -> param0.map(OptionalLong::of).orElseGet(OptionalLong::empty);
   public static final Function<OptionalLong, Optional<Long>> fromOptionalLong = param0 -> param0.isPresent()
      ? Optional.of(param0.getAsLong())
      : Optional.empty();
   public static final Codec<BitSet> BIT_SET = Codec.LONG_STREAM
      .xmap(param0 -> BitSet.valueOf(param0.toArray()), param0 -> Arrays.stream(param0.toLongArray()));
   private static final Codec<Property> PROPERTY = RecordCodecBuilder.create(
      param0 -> param0.group(
            Codec.STRING.fieldOf("name").forGetter(Property::getName),
            Codec.STRING.fieldOf("value").forGetter(Property::getValue),
            Codec.STRING.optionalFieldOf("signature").forGetter(l_param0 -> Optional.ofNullable(l_param0.getSignature()))
         )
         .apply(param0, (l_param0, l_param1, l_param2) -> new Property(l_param0, l_param1, l_param2.orElse(null)))
   );
   @VisibleForTesting
   public static final Codec<PropertyMap> PROPERTY_MAP = Codec.either(Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()), PROPERTY.listOf())
      .xmap(param0 -> {
         PropertyMap var1 = new PropertyMap();
         param0.ifLeft(l_param1 -> l_param1.forEach((l2_param1, l2_param2) -> {
            for (String l2_var4 : l2_param2) {
               var1.put(l2_param1, new Property(l2_param1, l2_var4));
            }
         })).ifRight(l_param1 -> {
            for (Property l_var3 : l_param1) {
               var1.put(l_var3.getName(), l_var3);
            }
         });
         return var1;
      }, param0 -> Either.right(param0.values().stream().toList()));
   public static final Codec<GameProfile> GAME_PROFILE = RecordCodecBuilder.create(
      param0 -> param0.group(
            Codec.mapPair(
                  UUIDUtil.AUTHLIB_CODEC.xmap(Optional::of, l_param0 -> (UUID)l_param0.orElse(null)).optionalFieldOf("id", Optional.empty()),
                  Codec.STRING.xmap(Optional::of, l_param0 -> (String)l_param0.orElse(null)).optionalFieldOf("name", Optional.empty())
               )
               .flatXmap(ExtraCodecs::mapIdNameToGameProfile, ExtraCodecs::mapGameProfileToIdName)
               .forGetter(Function.identity()),
            PROPERTY_MAP.optionalFieldOf("properties", new PropertyMap()).forGetter(GameProfile::getProperties)
         )
         .apply(param0, (l_param0, l_param1) -> {
            l_param1.forEach((l2_param1, l2_param2) -> l_param0.getProperties().put(l2_param1, l2_param2));
            return l_param0;
         })
   );
   public static final Codec<String> NON_EMPTY_STRING = validate(
      Codec.STRING, param0 -> param0.isEmpty() ? DataResult.error(() -> "Expected non-empty string") : DataResult.success(param0)
   );

   public static <F, S> Codec<Either<F, S>> xor(Codec<F> param0, Codec<S> param1) {
      return new ExtraCodecs.XorCodec<>(param0, param1);
   }

   public static <P, I> Codec<I> intervalCodec(
      Codec<P> param0, String param1, String param2, BiFunction<P, P, DataResult<I>> param3, Function<I, P> param4, Function<I, P> param5
   ) {
      Codec<I> var6 = Codec.list(param0).comapFlatMap(l_param1 -> Util.fixedSize((List<P>)l_param1, 2).flatMap(l2_param1 -> {
         P l2_var2 = l2_param1.get(0);
         P l2_var3 = l2_param1.get(1);
         return param3.apply(l2_var2, l2_var3);
      }), l_param2 -> ImmutableList.of(param4.apply((I)l_param2), param5.apply((I)l_param2)));
      Codec<I> var7 = RecordCodecBuilder.<Pair<P, P>>create(
            l_param3 -> l_param3.group(param0.fieldOf(param1).forGetter(Pair::getFirst), param0.fieldOf(param2).forGetter(Pair::getSecond))
               .apply(l_param3, Pair::of)
         )
         .comapFlatMap(
            l_param1 -> param3.apply((P)l_param1.getFirst(), (P)l_param1.getSecond()),
            l_param2 -> Pair.of(param4.apply((I)l_param2), param5.apply((I)l_param2))
         );
      Codec<I> var8 = new ExtraCodecs.EitherCodec<>(var6, var7)
         .xmap(l_param0 -> l_param0.map(l2_param0 -> (I)l2_param0, l2_param0 -> (I)l2_param0), Either::left);
      return Codec.either(param0, var8)
         .comapFlatMap(l_param1 -> l_param1.map(l2_param1 -> param3.apply((P)l2_param1, (P)l2_param1), DataResult::success), l_param2 -> {
            P l_var3 = param4.apply((I)l_param2);
            P l_var4 = param5.apply((I)l_param2);
            return Objects.equals(l_var3, l_var4) ? Either.left(l_var3) : Either.right((I)l_param2);
         });
   }

   public static <A> ResultFunction<A> orElsePartial(final A param0) {
      return new ResultFunction<A>() {
         @Override
         public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> l_param0, T l_param1, DataResult<Pair<A, T>> l_param2) {
            MutableObject<String> l_var4 = new MutableObject<>();
            Optional<Pair<A, T>> l_var5 = l_param2.resultOrPartial(l_var4::setValue);
            return l_var5.isPresent() ? l_param2 : DataResult.error(() -> "(" + l_var4.getValue() + " -> using default)", Pair.of(param0, l_param1));
         }

         @Override
         public <T> DataResult<T> coApply(DynamicOps<T> l_param0, A l_param1, DataResult<T> l_param2) {
            return l_param2;
         }

         @Override
         public String toString() {
            return "OrElsePartial[" + param0 + "]";
         }
      };
   }

   public static <E> Codec<E> idResolverCodec(ToIntFunction<E> param0, IntFunction<E> param1, int param2) {
      return Codec.INT
         .flatXmap(
            l_param1 -> Optional.ofNullable(param1.apply(l_param1))
               .map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Unknown element id: " + l_param1)),
            l_param2 -> {
               int l_var3 = param0.applyAsInt((E)l_param2);
               return l_var3 == param2 ? DataResult.error(() -> "Element with unknown id: " + l_param2) : DataResult.success(l_var3);
            }
         );
   }

   public static <E> Codec<E> stringResolverCodec(Function<E, String> param0, Function<String, E> param1) {
      return Codec.STRING
         .flatXmap(
            l_param1 -> Optional.ofNullable(param1.apply(l_param1))
               .map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Unknown element name:" + l_param1)),
            l_param1 -> Optional.ofNullable(param0.apply((E)l_param1))
               .map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Element with unknown name: " + l_param1))
         );
   }

   public static <E> Codec<E> orCompressed(final Codec<E> param0, final Codec<E> param1) {
      return new Codec<E>() {
         @Override
         public <T> DataResult<T> encode(E l_param0, DynamicOps<T> l_param1, T l_param2) {
            return l_param1.compressMaps() ? param1.encode(l_param0, l_param1, l_param2) : param0.encode(l_param0, l_param1, l_param2);
         }

         @Override
         public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> l_param0, T l_param1) {
            return l_param0.compressMaps() ? param1.decode(l_param0, l_param1) : param0.decode(l_param0, l_param1);
         }

         @Override
         public String toString() {
            return param0 + " orCompressed " + param1;
         }
      };
   }

   public static <E> Codec<E> overrideLifecycle(Codec<E> param0, final Function<E, Lifecycle> param1, final Function<E, Lifecycle> param2) {
      return param0.mapResult(new ResultFunction<E>() {
         @Override
         public <T> DataResult<Pair<E, T>> apply(DynamicOps<T> l_param0, T l_param1, DataResult<Pair<E, T>> l_param2) {
            return l_param2.result().map(l2_param2 -> l_param2.setLifecycle(param1.apply(l2_param2.getFirst()))).orElse(l_param2);
         }

         @Override
         public <T> DataResult<T> coApply(DynamicOps<T> l_param0, E l_param1, DataResult<T> l_param2) {
            return l_param2.setLifecycle(param2.apply(l_param1));
         }

         @Override
         public String toString() {
            return "WithLifecycle[" + param1 + " " + param2 + "]";
         }
      });
   }

   public static <T> Codec<T> validate(Codec<T> param0, Function<T, DataResult<T>> param1) {
      return param0.flatXmap(param1, param1);
   }

   private static Codec<Integer> intRangeWithMessage(int param0, int param1, Function<Integer, String> param2) {
      return validate(
         Codec.INT,
         l_param3 -> l_param3.compareTo(param0) >= 0 && l_param3.compareTo(param1) <= 0
            ? DataResult.success(l_param3)
            : DataResult.error(() -> param2.apply(l_param3))
      );
   }

   public static Codec<Integer> intRange(int param0, int param1) {
      return intRangeWithMessage(param0, param1, l_param2 -> "Value must be within range [" + param0 + ";" + param1 + "]: " + l_param2);
   }

   private static Codec<Float> floatRangeMinExclusiveWithMessage(float param0, float param1, Function<Float, String> param2) {
      return validate(
         Codec.FLOAT,
         l_param3 -> l_param3.compareTo(param0) > 0 && l_param3.compareTo(param1) <= 0
            ? DataResult.success(l_param3)
            : DataResult.error(() -> param2.apply(l_param3))
      );
   }

   public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> param0) {
      return validate(param0, l_param0 -> l_param0.isEmpty() ? DataResult.error(() -> "List must have contents") : DataResult.success(l_param0));
   }

   public static <T> Codec<HolderSet<T>> nonEmptyHolderSet(Codec<HolderSet<T>> param0) {
      return validate(
         param0,
         l_param0 -> l_param0.unwrap().right().filter(List::isEmpty).isPresent()
            ? DataResult.error(() -> "List must have contents")
            : DataResult.success(l_param0)
      );
   }

   public static <A> Codec<A> lazyInitializedCodec(Supplier<Codec<A>> param0) {
      return new ExtraCodecs.LazyInitializedCodec<>(param0);
   }

   public static <E> MapCodec<E> retrieveContext(final Function<DynamicOps<?>, DataResult<E>> param0) {
      class ContextRetrievalCodec extends MapCodec<E> {
         @Override
         public <T> RecordBuilder<T> encode(E l_param0, DynamicOps<T> l_param1, RecordBuilder<T> l_param2) {
            return l_param2;
         }

         @Override
         public <T> DataResult<E> decode(DynamicOps<T> l_param0, MapLike<T> l_param1) {
            return param0.apply(l_param0);
         }

         @Override
         public String toString() {
            return "ContextRetrievalCodec[" + param0 + "]";
         }

         @Override
         public <T> Stream<T> keys(DynamicOps<T> l_param0) {
            return Stream.empty();
         }
      }

      return new ContextRetrievalCodec();
   }

   public static <E, L extends Collection<E>, T> Function<L, DataResult<L>> ensureHomogenous(Function<E, T> param0) {
      return l_param1 -> {
         Iterator<E> l_var2 = l_param1.iterator();
         if (l_var2.hasNext()) {
            T l_var3 = param0.apply(l_var2.next());

            while (l_var2.hasNext()) {
               E l_var4 = l_var2.next();
               T l_var5 = param0.apply(l_var4);
               if (l_var5 != l_var3) {
                  return DataResult.error(() -> "Mixed type list: element " + l_var4 + " had type " + l_var5 + ", but list is of type " + l_var3);
               }
            }
         }

         return DataResult.success(l_param1, Lifecycle.stable());
      };
   }

   public static <A> Codec<A> catchDecoderException(final Codec<A> param0) {
      return Codec.of(param0, new Decoder<A>() {
         @Override
         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> l_param0, T l_param1) {
            try {
               return param0.decode(l_param0, l_param1);
            } catch (Exception l_var3) {
               return DataResult.error(() -> "Caught exception decoding " + l_param1 + ": " + l_var3.getMessage());
            }
         }
      });
   }

   public static Codec<Instant> instantCodec(DateTimeFormatter param0) {
      return Codec.STRING.comapFlatMap(l_param1 -> {
         try {
            return DataResult.success(Instant.from(param0.parse(l_param1)));
         } catch (Exception l_var2) {
            return DataResult.error(l_var2::getMessage);
         }
      }, param0::format);
   }

   public static MapCodec<OptionalLong> asOptionalLong(MapCodec<Optional<Long>> param0) {
      return param0.xmap(toOptionalLong, fromOptionalLong);
   }

   private static DataResult<GameProfile> mapIdNameToGameProfile(Pair<Optional<UUID>, Optional<String>> param0) {
      try {
         return DataResult.success(new GameProfile(param0.getFirst().orElse(null), param0.getSecond().orElse(null)));
      } catch (Throwable var1) {
         return DataResult.error(var1::getMessage);
      }
   }

   private static DataResult<Pair<Optional<UUID>, Optional<String>>> mapGameProfileToIdName(GameProfile param0) {
      return DataResult.success(Pair.of(Optional.ofNullable(param0.getId()), Optional.ofNullable(param0.getName())));
   }

   public static Codec<String> sizeLimitedString(int param0, int param1) {
      return validate(
         Codec.STRING,
         l_param2 -> {
            int l_var3 = l_param2.length();
            if (l_var3 < param0) {
               return DataResult.error(() -> "String \"" + l_param2 + "\" is too short: " + l_var3 + ", expected range [" + param0 + "-" + param1 + "]");
            } else {
               return l_var3 > param1
                  ? DataResult.error(() -> "String \"" + l_param2 + "\" is too long: " + l_var3 + ", expected range [" + param0 + "-" + param1 + "]")
                  : DataResult.success(l_param2);
            }
         }
      );
   }

   static final class EitherCodec<F, S> implements Codec<Either<F, S>> {
      private final Codec<F> first;
      private final Codec<S> second;

      public EitherCodec(Codec<F> param0, Codec<S> param1) {
         this.first = param0;
         this.second = param1;
      }

      @Override
      public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> param0, T param1) {
         DataResult<Pair<Either<F, S>, T>> var3 = this.first.decode(param0, param1).map(l_param0 -> l_param0.mapFirst(Either::left));
         if (!var3.error().isPresent()) {
            return var3;
         }

         DataResult<Pair<Either<F, S>, T>> var4 = this.second.decode(param0, param1).map(l_param0 -> l_param0.mapFirst(Either::right));
         return !var4.error().isPresent() ? var4 : var3.apply2((l_param0, l_param1) -> l_param1, var4);
      }

      public <T> DataResult<T> encode(Either<F, S> param0, DynamicOps<T> param1, T param2) {
         return param0.map(l_param2 -> this.first.encode((F)l_param2, param1, param2), l_param2 -> this.second.encode((S)l_param2, param1, param2));
      }

      @Override
      public boolean equals(Object param0) {
         if (this == param0) {
            return true;
         } else if (param0 != null && this.getClass() == param0.getClass()) {
            ExtraCodecs.EitherCodec<?, ?> var2 = (ExtraCodecs.EitherCodec<?, ?>)param0;
            return Objects.equals(this.first, var2.first) && Objects.equals(this.second, var2.second);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.first, this.second);
      }

      @Override
      public String toString() {
         return "EitherCodec[" + this.first + ", " + this.second + "]";
      }
   }

   record LazyInitializedCodec<A>(Supplier<Codec<A>> delegate) implements Codec<A> {
      LazyInitializedCodec(Supplier<Codec<A>> delegate) {
         Supplier<Codec<A>> var2 = Suppliers.memoize(delegate::get);
         this.delegate = var2;
      }

      @Override
      public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> param0, T param1) {
         return this.delegate.get().decode(param0, param1);
      }

      @Override
      public <T> DataResult<T> encode(A param0, DynamicOps<T> param1, T param2) {
         return this.delegate.get().encode(param0, param1, param2);
      }
   }

   public record TagOrElementLocation(ResourceLocation id, boolean tag) {
      @Override
      public String toString() {
         return this.decoratedId();
      }

      private String decoratedId() {
         return this.tag ? "#" + this.id : this.id.toString();
      }
   }

   static final class XorCodec<F, S> implements Codec<Either<F, S>> {
      private final Codec<F> first;
      private final Codec<S> second;

      public XorCodec(Codec<F> param0, Codec<S> param1) {
         this.first = param0;
         this.second = param1;
      }

      @Override
      public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> param0, T param1) {
         DataResult<Pair<Either<F, S>, T>> var3 = this.first.decode(param0, param1).map(l_param0 -> l_param0.mapFirst(Either::left));
         DataResult<Pair<Either<F, S>, T>> var4 = this.second.decode(param0, param1).map(l_param0 -> l_param0.mapFirst(Either::right));
         Optional<Pair<Either<F, S>, T>> var5 = var3.result();
         Optional<Pair<Either<F, S>, T>> var6 = var4.result();
         if (var5.isPresent() && var6.isPresent()) {
            return DataResult.error(
               () -> "Both alternatives read successfully, can not pick the correct one; first: " + var5.get() + " second: " + var6.get(), var5.get()
            );
         } else {
            return var5.isPresent() ? var3 : var4;
         }
      }

      public <T> DataResult<T> encode(Either<F, S> param0, DynamicOps<T> param1, T param2) {
         return param0.map(l_param2 -> this.first.encode((F)l_param2, param1, param2), l_param2 -> this.second.encode((S)l_param2, param1, param2));
      }

      @Override
      public boolean equals(Object param0) {
         if (this == param0) {
            return true;
         } else if (param0 != null && this.getClass() == param0.getClass()) {
            ExtraCodecs.XorCodec<?, ?> var2 = (ExtraCodecs.XorCodec<?, ?>)param0;
            return Objects.equals(this.first, var2.first) && Objects.equals(this.second, var2.second);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.first, this.second);
      }

      @Override
      public String toString() {
         return "XorCodec[" + this.first + ", " + this.second + "]";
      }
   }
}

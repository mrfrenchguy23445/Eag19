package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.mutable.MutableObject;

public interface CubicSpline<C, I extends ToFloatFunction<C>> extends ToFloatFunction<C> {
   @VisibleForDebug
   String parityString();

   CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> var1);

   static <C, I extends ToFloatFunction<C>> Codec<CubicSpline<C, I>> codec(Codec<I> param0) {
      MutableObject<Codec<CubicSpline<C, I>>> var1 = new MutableObject<>();

      record Point<C, I extends ToFloatFunction<C>>(float location, CubicSpline<C, I> value, float derivative) {
      }

      Codec<Point<C, I>> var2 = RecordCodecBuilder.create(
         l_param1 -> l_param1.group(
               Codec.FLOAT.fieldOf("location").forGetter(Point::location),
               ExtraCodecs.lazyInitializedCodec(var1::getValue).fieldOf("value").forGetter(Point::value),
               Codec.FLOAT.fieldOf("derivative").forGetter(Point::derivative)
            )
            .apply(l_param1, (l2_param0, l2_param1, l2_param2) -> new Point<>(l2_param0, l2_param1, l2_param2))
      );
      Codec<CubicSpline.Multipoint<C, I>> var3 = RecordCodecBuilder.create(
         l_param2 -> l_param2.group(
               param0.fieldOf("coordinate").forGetter(CubicSpline.Multipoint::coordinate),
               ExtraCodecs.nonEmptyList(var2.listOf())
                  .fieldOf("points")
                  .forGetter(
                     l2_param0 -> IntStream.range(0, l2_param0.locations.length)
                        .mapToObj(
                           l3_param1 -> new Point<>(l2_param0.locations()[l3_param1], l2_param0.values().get(l3_param1), l2_param0.derivatives()[l3_param1])
                        )
                        .toList()
                  )
            )
            .apply(l_param2, (l2_param0, l2_param1) -> {
               float[] l2_var2 = new float[l2_param1.size()];
               ImmutableList.Builder<CubicSpline<C, I>> l2_var3 = ImmutableList.builder();
               float[] l2_var4 = new float[l2_param1.size()];

               for (int l2_var5 = 0; l2_var5 < l2_param1.size(); l2_var5++) {
                  Point<C, I> l2_var6 = l2_param1.get(l2_var5);
                  l2_var2[l2_var5] = l2_var6.location();
                  l2_var3.add(l2_var6.value());
                  l2_var4[l2_var5] = l2_var6.derivative();
               }

               return CubicSpline.Multipoint.create(l2_param0, l2_var2, l2_var3.build(), l2_var4);
            })
      );
      var1.setValue(
         Codec.either(Codec.FLOAT, var3)
            .xmap(
               l_param0 -> l_param0.map(CubicSpline.Constant::new, l2_param0 -> l2_param0),
               l_param0 -> l_param0 instanceof CubicSpline.Constant<C, I> l_var1
                  ? Either.left(l_var1.value())
                  : Either.right((CubicSpline.Multipoint<C, I>)l_param0)
            )
      );
      return var1.getValue();
   }

   static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> constant(float param0) {
      return new CubicSpline.Constant<>(param0);
   }

   static <C, I extends ToFloatFunction<C>> CubicSpline.Builder<C, I> builder(I param0) {
      return new CubicSpline.Builder<>(param0);
   }

   static <C, I extends ToFloatFunction<C>> CubicSpline.Builder<C, I> builder(I param0, ToFloatFunction<Float> param1) {
      return new CubicSpline.Builder<>(param0, param1);
   }

   final class Builder<C, I extends ToFloatFunction<C>> {
      private final I coordinate;
      private final ToFloatFunction<Float> valueTransformer;
      private final FloatList locations = new FloatArrayList();
      private final List<CubicSpline<C, I>> values = Lists.newArrayList();
      private final FloatList derivatives = new FloatArrayList();

      protected Builder(I param0) {
         this(param0, ToFloatFunction.IDENTITY);
      }

      protected Builder(I param0, ToFloatFunction<Float> param1) {
         this.coordinate = param0;
         this.valueTransformer = param1;
      }

      public CubicSpline.Builder<C, I> addPoint(float param0, float param1) {
         return this.addPoint(param0, new CubicSpline.Constant<>(this.valueTransformer.apply(param1)), 0.0F);
      }

      public CubicSpline.Builder<C, I> addPoint(float param0, float param1, float param2) {
         return this.addPoint(param0, new CubicSpline.Constant<>(this.valueTransformer.apply(param1)), param2);
      }

      public CubicSpline.Builder<C, I> addPoint(float param0, CubicSpline<C, I> param1) {
         return this.addPoint(param0, param1, 0.0F);
      }

      private CubicSpline.Builder<C, I> addPoint(float param0, CubicSpline<C, I> param1, float param2) {
         if (!this.locations.isEmpty() && param0 <= this.locations.getFloat(this.locations.size() - 1)) {
            throw new IllegalArgumentException("Please register points in ascending order");
         }

         this.locations.add(param0);
         this.values.add(param1);
         this.derivatives.add(param2);
         return this;
      }

      public CubicSpline<C, I> build() {
         if (this.locations.isEmpty()) {
            throw new IllegalStateException("No elements added");
         } else {
            return CubicSpline.Multipoint.create(
               this.coordinate, this.locations.toFloatArray(), ImmutableList.copyOf(this.values), this.derivatives.toFloatArray()
            );
         }
      }
   }

   @VisibleForDebug
   record Constant<C, I extends ToFloatFunction<C>>(float value) implements CubicSpline<C, I> {
      @Override
      public float apply(C param0) {
         return this.value;
      }

      @Override
      public String parityString() {
         return String.format(Locale.ROOT, "k=%.3f", this.value);
      }

      @Override
      public float minValue() {
         return this.value;
      }

      @Override
      public float maxValue() {
         return this.value;
      }

      @Override
      public CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> param0) {
         return this;
      }
   }

   interface CoordinateVisitor<I> {
      I visit(I var1);
   }

   @VisibleForDebug
   record Multipoint<C, I extends ToFloatFunction<C>>(
      I coordinate, float[] locations, List<CubicSpline<C, I>> values, float[] derivatives, float minValue, float maxValue
   ) implements CubicSpline<C, I> {

      public Multipoint {
         validateSizes(locations, values, derivatives);
      }

      static <C, I extends ToFloatFunction<C>> CubicSpline.Multipoint<C, I> create(I param0, float[] param1, List<CubicSpline<C, I>> param2, float[] param3) {
         validateSizes(param1, param2, param3);
         int var4 = param1.length - 1;
         float var5 = Float.POSITIVE_INFINITY;
         float var6 = Float.NEGATIVE_INFINITY;
         float var7 = param0.minValue();
         float var8 = param0.maxValue();
         if (var7 < param1[0]) {
            float var9 = linearExtend(var7, param1, param2.get(0).minValue(), param3, 0);
            float var10 = linearExtend(var7, param1, param2.get(0).maxValue(), param3, 0);
            var5 = Math.min(var5, Math.min(var9, var10));
            var6 = Math.max(var6, Math.max(var9, var10));
         }

         if (var8 > param1[var4]) {
            float var9 = linearExtend(var8, param1, param2.get(var4).minValue(), param3, var4);
            float var10 = linearExtend(var8, param1, param2.get(var4).maxValue(), param3, var4);
            var5 = Math.min(var5, Math.min(var9, var10));
            var6 = Math.max(var6, Math.max(var9, var10));
         }

         for (CubicSpline<C, I> var10 : param2) {
            var5 = Math.min(var5, var10.minValue());
            var6 = Math.max(var6, var10.maxValue());
         }

         for (int var9 = 0; var9 < var4; var9++) {
            float var10 = param1[var9];
            float var11 = param1[var9 + 1];
            float var12 = var11 - var10;
            CubicSpline<C, I> var13 = param2.get(var9);
            CubicSpline<C, I> var14 = param2.get(var9 + 1);
            float var15 = var13.minValue();
            float var16 = var13.maxValue();
            float var17 = var14.minValue();
            float var18 = var14.maxValue();
            float var19 = param3[var9];
            float var20 = param3[var9 + 1];
            if (var19 != 0.0F || var20 != 0.0F) {
               float var21 = var19 * var12;
               float var22 = var20 * var12;
               float var23 = Math.min(var15, var17);
               float var24 = Math.max(var16, var18);
               float var25 = var21 - var18 + var15;
               float var26 = var21 - var17 + var16;
               float var27 = -var22 + var17 - var16;
               float var28 = -var22 + var18 - var15;
               float var29 = Math.min(var25, var27);
               float var30 = Math.max(var26, var28);
               var5 = Math.min(var5, var23 + 0.25F * var29);
               var6 = Math.max(var6, var24 + 0.25F * var30);
            }
         }

         return new CubicSpline.Multipoint<>(param0, param1, param2, param3, var5, var6);
      }

      private static float linearExtend(float param0, float[] param1, float param2, float[] param3, int param4) {
         float var5 = param3[param4];
         return var5 == 0.0F ? param2 : param2 + var5 * (param0 - param1[param4]);
      }

      private static <C, I extends ToFloatFunction<C>> void validateSizes(float[] param0, List<CubicSpline<C, I>> param1, float[] param2) {
         if (param0.length != param1.size() || param0.length != param2.length) {
            throw new IllegalArgumentException("All lengths must be equal, got: " + param0.length + " " + param1.size() + " " + param2.length);
         }

         if (param0.length == 0) {
            throw new IllegalArgumentException("Cannot create a multipoint spline with no points");
         }
      }

      @Override
      public float apply(C param0) {
         float var2 = this.coordinate.apply(param0);
         int var3 = findIntervalStart(this.locations, var2);
         int var4 = this.locations.length - 1;
         if (var3 < 0) {
            return linearExtend(var2, this.locations, this.values.get(0).apply(param0), this.derivatives, 0);
         }

         if (var3 == var4) {
            return linearExtend(var2, this.locations, this.values.get(var4).apply(param0), this.derivatives, var4);
         }

         float var5 = this.locations[var3];
         float var6 = this.locations[var3 + 1];
         float var7 = (var2 - var5) / (var6 - var5);
         ToFloatFunction<C> var8 = (ToFloatFunction<C>)this.values.get(var3);
         ToFloatFunction<C> var9 = (ToFloatFunction<C>)this.values.get(var3 + 1);
         float var10 = this.derivatives[var3];
         float var11 = this.derivatives[var3 + 1];
         float var12 = var8.apply(param0);
         float var13 = var9.apply(param0);
         float var14 = var10 * (var6 - var5) - (var13 - var12);
         float var15 = -var11 * (var6 - var5) + (var13 - var12);
         return Mth.lerp(var7, var12, var13) + var7 * (1.0F - var7) * Mth.lerp(var7, var14, var15);
      }

      private static int findIntervalStart(float[] param0, float param1) {
         return Mth.binarySearch(0, param0.length, l_param2 -> param1 < param0[l_param2]) - 1;
      }

      @VisibleForTesting
      @Override
      public String parityString() {
         return "Spline{coordinate="
            + this.coordinate
            + ", locations="
            + this.toString(this.locations)
            + ", derivatives="
            + this.toString(this.derivatives)
            + ", values="
            + this.values.stream().map(CubicSpline::parityString).collect(Collectors.joining(", ", "[", "]"))
            + "}";
      }

      private String toString(float[] param0) {
         return "["
            + IntStream.range(0, param0.length)
               .mapToDouble(l_param1 -> param0[l_param1])
               .mapToObj(l_param0 -> String.format(Locale.ROOT, "%.3f", l_param0))
               .collect(Collectors.joining(", "))
            + "]";
      }

      @Override
      public CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> param0) {
         return create(
            param0.visit(this.coordinate), this.locations, this.values().stream().map(l_param1 -> l_param1.mapAll(param0)).toList(), this.derivatives
         );
      }
   }
}

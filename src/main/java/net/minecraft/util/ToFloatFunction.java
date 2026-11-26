package net.minecraft.util;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.function.Function;

public interface ToFloatFunction<C> {
   ToFloatFunction<Float> IDENTITY = createUnlimited(param0 -> param0);

   float apply(C var1);

   float minValue();

   float maxValue();

   static ToFloatFunction<Float> createUnlimited(final Float2FloatFunction param0) {
      return new ToFloatFunction<Float>() {
         public float apply(Float l_param0) {
            return param0.apply(l_param0);
         }

         @Override
         public float minValue() {
            return Float.NEGATIVE_INFINITY;
         }

         @Override
         public float maxValue() {
            return Float.POSITIVE_INFINITY;
         }
      };
   }

   default <C2> ToFloatFunction<C2> comap(final Function<C2, C> param0) {
      final ToFloatFunction<C> var2 = this;
      return new ToFloatFunction<C2>() {
         @Override
         public float apply(C2 l_param0) {
            return var2.apply(param0.apply(l_param0));
         }

         @Override
         public float minValue() {
            return var2.minValue();
         }

         @Override
         public float maxValue() {
            return var2.maxValue();
         }
      };
   }
}

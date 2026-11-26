package net.minecraft.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public class ByIdMap {
   private static <T> IntFunction<T> createMap(ToIntFunction<T> param0, T[] param1) {
      if (param1.length == 0) {
         throw new IllegalArgumentException("Empty value list");
      }

      Int2ObjectMap<T> var2 = new Int2ObjectOpenHashMap<>();

      for (T var6 : param1) {
         int var7 = param0.applyAsInt(var6);
         T var8 = var2.put(var7, var6);
         if (var8 != null) {
            throw new IllegalArgumentException("Duplicate entry on id " + var7 + ": current=" + var6 + ", previous=" + var8);
         }
      }

      return var2;
   }

   public static <T> IntFunction<T> sparse(ToIntFunction<T> param0, T[] param1, T param2) {
      IntFunction<T> var3 = createMap(param0, param1);
      return l_param2 -> Objects.requireNonNullElse(var3.apply(l_param2), param2);
   }

   private static <T> T[] createSortedArray(ToIntFunction<T> param0, T[] param1) {
      int var2 = param1.length;
      if (var2 == 0) {
         throw new IllegalArgumentException("Empty value list");
      }

      T[] var3 = (T[])param1.clone();
      Arrays.fill(var3, null);

      for (T var7 : param1) {
         int var8 = param0.applyAsInt(var7);
         if (var8 < 0 || var8 >= var2) {
            throw new IllegalArgumentException("Values are not continous, found index " + var8 + " for value " + var7);
         }

         T var9 = var3[var8];
         if (var9 != null) {
            throw new IllegalArgumentException("Duplicate entry on id " + var8 + ": current=" + var7 + ", previous=" + var9);
         }

         var3[var8] = var7;
      }

      for (int var4 = 0; var4 < var2; var4++) {
         if (var3[var4] == null) {
            throw new IllegalArgumentException("Missing value at index: " + var4);
         }
      }

      return var3;
   }

   public static <T> IntFunction<T> continuous(ToIntFunction<T> param0, T[] param1, ByIdMap.OutOfBoundsStrategy param2) {
      T[] var3 = createSortedArray(param0, param1);
      int var4 = var3.length;

      return switch (param2) {
         case ZERO -> {
            T var5 = var3[0];
            yield l_param3 -> l_param3 >= 0 && l_param3 < var4 ? var3[l_param3] : var5;
         }
         case WRAP -> l_param2 -> var3[Mth.positiveModulo(l_param2, var4)];
         case CLAMP -> l_param2 -> var3[Mth.clamp(l_param2, 0, var4 - 1)];
      };
   }

   public enum OutOfBoundsStrategy {
      ZERO,
      WRAP,
      CLAMP;
   }
}

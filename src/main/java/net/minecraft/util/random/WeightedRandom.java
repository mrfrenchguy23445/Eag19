package net.minecraft.util.random;

import java.util.List;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.util.RandomSource;

public class WeightedRandom {
   private WeightedRandom() {
   }

   public static int getTotalWeight(List<? extends WeightedEntry> param0) {
      long var1 = 0L;

      for (WeightedEntry var4 : param0) {
         var1 += var4.getWeight().asInt();
      }

      if (var1 > 2147483647L) {
         throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
      } else {
         return (int)var1;
      }
   }

   public static <T extends WeightedEntry> Optional<T> getRandomItem(RandomSource param0, List<T> param1, int param2) {
      if (param2 < 0) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("Negative total weight in getRandomItem"));
      }

      if (param2 == 0) {
         return Optional.empty();
      }

      int var3 = param0.nextInt(param2);
      return getWeightedItem(param1, var3);
   }

   public static <T extends WeightedEntry> Optional<T> getWeightedItem(List<T> param0, int param1) {
      for (T var3 : param0) {
         param1 -= var3.getWeight().asInt();
         if (param1 < 0) {
            return Optional.of(var3);
         }
      }

      return Optional.empty();
   }

   public static <T extends WeightedEntry> Optional<T> getRandomItem(RandomSource param0, List<T> param1) {
      return getRandomItem(param0, param1, getTotalWeight(param1));
   }
}

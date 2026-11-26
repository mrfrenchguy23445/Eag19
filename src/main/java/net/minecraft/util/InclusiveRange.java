package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public record InclusiveRange<T extends Comparable<T>>(T minInclusive, T maxInclusive) {
   public static final Codec<InclusiveRange<Integer>> INT = codec(Codec.INT);

   public InclusiveRange {
      if (minInclusive.compareTo(maxInclusive) > 0) {
         throw new IllegalArgumentException("min_inclusive must be less than or equal to max_inclusive");
      }
   }

   public static <T extends Comparable<T>> Codec<InclusiveRange<T>> codec(Codec<T> param0) {
      return ExtraCodecs.intervalCodec(
         param0, "min_inclusive", "max_inclusive", InclusiveRange::create, InclusiveRange::minInclusive, InclusiveRange::maxInclusive
      );
   }

   public static <T extends Comparable<T>> Codec<InclusiveRange<T>> codec(Codec<T> param0, T param1, T param2) {
      return ExtraCodecs.validate(
         codec(param0),
         l_param2 -> {
            if (l_param2.minInclusive().compareTo(param1) < 0) {
               return DataResult.error(
                  () -> "Range limit too low, expected at least " + param1 + " [" + l_param2.minInclusive() + "-" + l_param2.maxInclusive() + "]"
               );
            } else {
               return l_param2.maxInclusive().compareTo(param2) > 0
                  ? DataResult.error(
                     () -> "Range limit too high, expected at most " + param2 + " [" + l_param2.minInclusive() + "-" + l_param2.maxInclusive() + "]"
                  )
                  : DataResult.success(l_param2);
            }
         }
      );
   }

   public static <T extends Comparable<T>> DataResult<InclusiveRange<T>> create(T param0, T param1) {
      return param0.compareTo(param1) <= 0
         ? DataResult.success(new InclusiveRange<>(param0, param1))
         : DataResult.error(() -> "min_inclusive must be less than or equal to max_inclusive");
   }

   public boolean isValueInRange(T param0) {
      return param0.compareTo(this.minInclusive) >= 0 && param0.compareTo(this.maxInclusive) <= 0;
   }

   public boolean contains(InclusiveRange<T> param0) {
      return param0.minInclusive().compareTo(this.minInclusive) >= 0 && param0.maxInclusive.compareTo(this.maxInclusive) <= 0;
   }

   @Override
   public String toString() {
      return "[" + this.minInclusive + ", " + this.maxInclusive + "]";
   }
}

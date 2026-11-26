package net.minecraft.world.flag;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nullable;

public final class FeatureFlagSet {
   private static final FeatureFlagSet EMPTY = new FeatureFlagSet(null, 0L);
   public static final int MAX_CONTAINER_SIZE = 64;
   @Nullable
   private final FeatureFlagUniverse universe;
   private final long mask;

   private FeatureFlagSet(@Nullable FeatureFlagUniverse param0, long param1) {
      this.universe = param0;
      this.mask = param1;
   }

   static FeatureFlagSet create(FeatureFlagUniverse param0, Collection<FeatureFlag> param1) {
      if (param1.isEmpty()) {
         return EMPTY;
      }

      long var2 = computeMask(param0, 0L, param1);
      return new FeatureFlagSet(param0, var2);
   }

   public static FeatureFlagSet of() {
      return EMPTY;
   }

   public static FeatureFlagSet of(FeatureFlag param0) {
      return new FeatureFlagSet(param0.universe, param0.mask);
   }

   public static FeatureFlagSet of(FeatureFlag param0, FeatureFlag... param1) {
      long var2 = param1.length == 0 ? param0.mask : computeMask(param0.universe, param0.mask, Arrays.asList(param1));
      return new FeatureFlagSet(param0.universe, var2);
   }

   private static long computeMask(FeatureFlagUniverse param0, long param1, Iterable<FeatureFlag> param3) {
      for (FeatureFlag var5 : param3) {
         if (param0 != var5.universe) {
            throw new IllegalStateException("Mismatched feature universe, expected '" + param0 + "', but got '" + var5.universe + "'");
         }

         param1 |= var5.mask;
      }

      return param1;
   }

   public boolean contains(FeatureFlag param0) {
      return this.universe != param0.universe ? false : (this.mask & param0.mask) != 0L;
   }

   public boolean isSubsetOf(FeatureFlagSet param0) {
      if (this.universe == null) {
         return true;
      } else {
         return this.universe != param0.universe ? false : (this.mask & ~param0.mask) == 0L;
      }
   }

   public FeatureFlagSet join(FeatureFlagSet param0) {
      if (this.universe == null) {
         return param0;
      } else if (param0.universe == null) {
         return this;
      } else if (this.universe != param0.universe) {
         throw new IllegalArgumentException("Mismatched set elements: '" + this.universe + "' != '" + param0.universe + "'");
      } else {
         return new FeatureFlagSet(this.universe, this.mask | param0.mask);
      }
   }

   @Override
   public boolean equals(Object param0) {
      return this == param0 ? true : param0 instanceof FeatureFlagSet var2 && this.universe == var2.universe && this.mask == var2.mask;
   }

   @Override
   public int hashCode() {
      return (int)HashCommon.mix(this.mask);
   }
}

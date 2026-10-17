package net.minecraft;

import java.util.Objects;

@FunctionalInterface
public interface CharPredicate {
   boolean test(char var1);

   default CharPredicate and(CharPredicate param0) {
      Objects.requireNonNull(param0);
      return l_param1 -> this.test(l_param1) && param0.test(l_param1);
   }

   default CharPredicate negate() {
      return l_param0 -> !this.test(l_param0);
   }

   default CharPredicate or(CharPredicate param0) {
      Objects.requireNonNull(param0);
      return l_param1 -> this.test(l_param1) || param0.test(l_param1);
   }
}

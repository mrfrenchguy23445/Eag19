package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.List;
import net.minecraft.network.chat.Style;

@FunctionalInterface
public interface FormattedCharSequence {
   FormattedCharSequence EMPTY = param0 -> true;

   boolean accept(FormattedCharSink var1);

   static FormattedCharSequence codepoint(int param0, Style param1) {
      return l_param2 -> l_param2.accept(0, param1, param0);
   }

   static FormattedCharSequence forward(String param0, Style param1) {
      return param0.isEmpty() ? EMPTY : l_param2 -> StringDecomposer.iterate(param0, param1, l_param2);
   }

   static FormattedCharSequence forward(String param0, Style param1, Int2IntFunction param2) {
      return param0.isEmpty() ? EMPTY : l_param3 -> StringDecomposer.iterate(param0, param1, decorateOutput(l_param3, param2));
   }

   static FormattedCharSequence backward(String param0, Style param1) {
      return param0.isEmpty() ? EMPTY : l_param2 -> StringDecomposer.iterateBackwards(param0, param1, l_param2);
   }

   static FormattedCharSequence backward(String param0, Style param1, Int2IntFunction param2) {
      return param0.isEmpty() ? EMPTY : l_param3 -> StringDecomposer.iterateBackwards(param0, param1, decorateOutput(l_param3, param2));
   }

   static FormattedCharSink decorateOutput(FormattedCharSink param0, Int2IntFunction param1) {
      return (l_param2, l_param3, l_param4) -> param0.accept(l_param2, l_param3, param1.apply(l_param4));
   }

   static FormattedCharSequence composite() {
      return EMPTY;
   }

   static FormattedCharSequence composite(FormattedCharSequence param0) {
      return param0;
   }

   static FormattedCharSequence composite(FormattedCharSequence param0, FormattedCharSequence param1) {
      return fromPair(param0, param1);
   }

   static FormattedCharSequence composite(FormattedCharSequence... param0) {
      return fromList(ImmutableList.copyOf(param0));
   }

   static FormattedCharSequence composite(List<FormattedCharSequence> param0) {
      int var1 = param0.size();
      switch (var1) {
         case 0:
            return EMPTY;
         case 1:
            return param0.get(0);
         case 2:
            return fromPair(param0.get(0), param0.get(1));
         default:
            return fromList(ImmutableList.copyOf(param0));
      }
   }

   static FormattedCharSequence fromPair(FormattedCharSequence param0, FormattedCharSequence param1) {
      return l_param2 -> param0.accept(l_param2) && param1.accept(l_param2);
   }

   static FormattedCharSequence fromList(List<FormattedCharSequence> param0) {
      return l_param1 -> {
         for (FormattedCharSequence l_var3 : param0) {
            if (!l_var3.accept(l_param1)) {
               return false;
            }
         }

         return true;
      };
   }
}

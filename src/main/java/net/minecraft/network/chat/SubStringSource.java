package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

public class SubStringSource {
   private final String plainText;
   private final List<Style> charStyles;
   private final Int2IntFunction reverseCharModifier;

   private SubStringSource(String param0, List<Style> param1, Int2IntFunction param2) {
      this.plainText = param0;
      this.charStyles = ImmutableList.copyOf(param1);
      this.reverseCharModifier = param2;
   }

   public String getPlainText() {
      return this.plainText;
   }

   public List<FormattedCharSequence> substring(int param0, int param1, boolean param2) {
      if (param1 == 0) {
         return ImmutableList.of();
      }

      List<FormattedCharSequence> var4 = Lists.newArrayList();
      Style var5 = this.charStyles.get(param0);
      int var6 = param0;

      for (int var7 = 1; var7 < param1; var7++) {
         int var8 = param0 + var7;
         Style var9 = this.charStyles.get(var8);
         if (!var9.equals(var5)) {
            String var10 = this.plainText.substring(var6, var8);
            var4.add(param2 ? FormattedCharSequence.backward(var10, var5, this.reverseCharModifier) : FormattedCharSequence.forward(var10, var5));
            var5 = var9;
            var6 = var8;
         }
      }

      if (var6 < param0 + param1) {
         String var7 = this.plainText.substring(var6, param0 + param1);
         var4.add(param2 ? FormattedCharSequence.backward(var7, var5, this.reverseCharModifier) : FormattedCharSequence.forward(var7, var5));
      }

      return param2 ? Lists.reverse(var4) : var4;
   }

   public static SubStringSource create(FormattedText param0) {
      return create(param0, l_param0 -> l_param0, l_param0 -> l_param0);
   }

   public static SubStringSource create(FormattedText param0, Int2IntFunction param1, UnaryOperator<String> param2) {
      StringBuilder var3 = new StringBuilder();
      List<Style> var4 = Lists.newArrayList();
      param0.visit((l_param2, l_param3) -> {
         StringDecomposer.iterateFormatted(l_param3, l_param2, (l2_param2, l2_param3, l2_param4) -> {
            var3.appendCodePoint(l2_param4);
            int l2_var5 = Character.charCount(l2_param4);

            for (int l2_var6 = 0; l2_var6 < l2_var5; l2_var6++) {
               var4.add(l2_param3);
            }

            return true;
         });
         return Optional.empty();
      }, Style.EMPTY);
      return new SubStringSource(param2.apply(var3.toString()), var4, param1);
   }
}

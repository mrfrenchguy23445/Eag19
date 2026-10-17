package net.minecraft.stats;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.Util;

public interface StatFormatter {
   DecimalFormat DECIMAL_FORMAT = Util.make(
      new DecimalFormat("########0.00"), param0 -> param0.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT))
   );
   StatFormatter DEFAULT = NumberFormat.getIntegerInstance(Locale.US)::format;
   StatFormatter DIVIDE_BY_TEN = param0 -> DECIMAL_FORMAT.format(param0 * 0.1);
   StatFormatter DISTANCE = param0 -> {
      double var1 = param0 / 100.0;
      double var3 = var1 / 1000.0;
      if (var3 > 0.5) {
         return DECIMAL_FORMAT.format(var3) + " km";
      } else {
         return var1 > 0.5 ? DECIMAL_FORMAT.format(var1) + " m" : param0 + " cm";
      }
   };
   StatFormatter TIME = param0 -> {
      double var1 = param0 / 20.0;
      double var3 = var1 / 60.0;
      double var5 = var3 / 60.0;
      double var7 = var5 / 24.0;
      double var9 = var7 / 365.0;
      if (var9 > 0.5) {
         return DECIMAL_FORMAT.format(var9) + " y";
      } else if (var7 > 0.5) {
         return DECIMAL_FORMAT.format(var7) + " d";
      } else if (var5 > 0.5) {
         return DECIMAL_FORMAT.format(var5) + " h";
      } else {
         return var3 > 0.5 ? DECIMAL_FORMAT.format(var3) + " m" : var1 + " s";
      }
   };

   String format(int var1);
}

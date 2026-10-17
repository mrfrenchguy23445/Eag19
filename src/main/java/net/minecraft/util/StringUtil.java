package net.minecraft.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public class StringUtil {
   private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
   // PORT NOTE (Eag19 / TeaVM WASM-GC): \v (vertical whitespace) is not implemented by
   // TeaVM's regex engine (see GlslPreprocessor for the same issue with \h); expand it to
   // the JDK's documented vertical-whitespace class. \r\n is matched first so a CRLF counts
   // as a single line break.
   private static final Pattern LINE_PATTERN = Pattern.compile("\\r\\n|[\\n\\x0B\\f\\r\\x85\\u2028\\u2029]");
   private static final Pattern LINE_END_PATTERN = Pattern.compile("(?:\\r\\n|[\\n\\x0B\\f\\r\\x85\\u2028\\u2029])$");

   public static String formatTickDuration(int param0) {
      int var1 = param0 / 20;
      int var2 = var1 / 60;
      var1 %= 60;
      int var3 = var2 / 60;
      var2 %= 60;
      return var3 > 0 ? String.format(Locale.ROOT, "%02d:%02d:%02d", var3, var2, var1) : String.format(Locale.ROOT, "%02d:%02d", var2, var1);
   }

   public static String stripColor(String param0) {
      return STRIP_COLOR_PATTERN.matcher(param0).replaceAll("");
   }

   public static boolean isNullOrEmpty(@Nullable String param0) {
      return StringUtils.isEmpty(param0);
   }

   public static String truncateStringIfNecessary(String param0, int param1, boolean param2) {
      if (param0.length() <= param1) {
         return param0;
      } else {
         return param2 && param1 > 3 ? param0.substring(0, param1 - 3) + "..." : param0.substring(0, param1);
      }
   }

   public static int lineCount(String param0) {
      if (param0.isEmpty()) {
         return 0;
      }

      Matcher var1 = LINE_PATTERN.matcher(param0);
      int var2 = 1;

      while (var1.find()) {
         var2++;
      }

      return var2;
   }

   public static boolean endsWithNewLine(String param0) {
      return LINE_END_PATTERN.matcher(param0).find();
   }

   public static String trimChatMessage(String param0) {
      return truncateStringIfNecessary(param0, 256, false);
   }
}

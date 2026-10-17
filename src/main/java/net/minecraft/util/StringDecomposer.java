package net.minecraft.util;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public class StringDecomposer {
   private static final char REPLACEMENT_CHAR = '�';
   private static final Optional<Object> STOP_ITERATION = Optional.of(Unit.INSTANCE);

   private static boolean feedChar(Style param0, FormattedCharSink param1, int param2, char param3) {
      return Character.isSurrogate(param3) ? param1.accept(param2, param0, 65533) : param1.accept(param2, param0, param3);
   }

   public static boolean iterate(String param0, Style param1, FormattedCharSink param2) {
      int var3 = param0.length();

      for (int var4 = 0; var4 < var3; var4++) {
         char var5 = param0.charAt(var4);
         if (Character.isHighSurrogate(var5)) {
            if (var4 + 1 >= var3) {
               if (!param2.accept(var4, param1, 65533)) {
                  return false;
               }
               break;
            }

            char var6 = param0.charAt(var4 + 1);
            if (Character.isLowSurrogate(var6)) {
               if (!param2.accept(var4, param1, Character.toCodePoint(var5, var6))) {
                  return false;
               }

               var4++;
            } else if (!param2.accept(var4, param1, 65533)) {
               return false;
            }
         } else if (!feedChar(param1, param2, var4, var5)) {
            return false;
         }
      }

      return true;
   }

   public static boolean iterateBackwards(String param0, Style param1, FormattedCharSink param2) {
      int var3 = param0.length();

      for (int var4 = var3 - 1; var4 >= 0; var4--) {
         char var5 = param0.charAt(var4);
         if (Character.isLowSurrogate(var5)) {
            if (var4 - 1 < 0) {
               if (!param2.accept(0, param1, 65533)) {
                  return false;
               }
               break;
            }

            char var6 = param0.charAt(var4 - 1);
            if (Character.isHighSurrogate(var6)) {
               if (!param2.accept(--var4, param1, Character.toCodePoint(var6, var5))) {
                  return false;
               }
            } else if (!param2.accept(var4, param1, 65533)) {
               return false;
            }
         } else if (!feedChar(param1, param2, var4, var5)) {
            return false;
         }
      }

      return true;
   }

   public static boolean iterateFormatted(String param0, Style param1, FormattedCharSink param2) {
      return iterateFormatted(param0, 0, param1, param2);
   }

   public static boolean iterateFormatted(String param0, int param1, Style param2, FormattedCharSink param3) {
      return iterateFormatted(param0, param1, param2, param2, param3);
   }

   public static boolean iterateFormatted(String param0, int param1, Style param2, Style param3, FormattedCharSink param4) {
      int var5 = param0.length();
      Style var6 = param2;

      for (int var7 = param1; var7 < var5; var7++) {
         char var8 = param0.charAt(var7);
         if (var8 == 167) {
            if (var7 + 1 >= var5) {
               break;
            }

            char var9 = param0.charAt(var7 + 1);
            ChatFormatting var10 = ChatFormatting.getByCode(var9);
            if (var10 != null) {
               var6 = var10 == ChatFormatting.RESET ? param3 : var6.applyLegacyFormat(var10);
            }

            var7++;
         } else if (Character.isHighSurrogate(var8)) {
            if (var7 + 1 >= var5) {
               if (!param4.accept(var7, var6, 65533)) {
                  return false;
               }
               break;
            }

            char var9 = param0.charAt(var7 + 1);
            if (Character.isLowSurrogate(var9)) {
               if (!param4.accept(var7, var6, Character.toCodePoint(var8, var9))) {
                  return false;
               }

               var7++;
            } else if (!param4.accept(var7, var6, 65533)) {
               return false;
            }
         } else if (!feedChar(var6, param4, var7, var8)) {
            return false;
         }
      }

      return true;
   }

   public static boolean iterateFormatted(FormattedText param0, Style param1, FormattedCharSink param2) {
      return !param0.visit((l_param1, l_param2) -> iterateFormatted(l_param2, 0, l_param1, param2) ? Optional.empty() : STOP_ITERATION, param1).isPresent();
   }

   public static String filterBrokenSurrogates(String param0) {
      StringBuilder var1 = new StringBuilder();
      iterate(param0, Style.EMPTY, (l_param1, l_param2, l_param3) -> {
         var1.appendCodePoint(l_param3);
         return true;
      });
      return var1.toString();
   }

   public static String getPlainText(FormattedText param0) {
      StringBuilder var1 = new StringBuilder();
      iterateFormatted(param0, Style.EMPTY, (l_param1, l_param2, l_param3) -> {
         var1.appendCodePoint(l_param3);
         return true;
      });
      return var1.toString();
   }
}

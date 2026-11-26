package com.mojang.realmsclient;

import java.util.Locale;

public enum Unit {
   B,
   KB,
   MB,
   GB;

   private static final int BASE_UNIT = 1024;

   public static Unit getLargest(long param0) {
      if (param0 < 1024L) {
         return B;
      }

      try {
         int var2 = (int)(Math.log(param0) / Math.log(1024.0));
         String var3 = String.valueOf("KMGTPE".charAt(var2 - 1));
         return valueOf(var3 + "B");
      } catch (Exception var2) {
         return GB;
      }
   }

   public static double convertTo(long param0, Unit param2) {
      return param2 == B ? param0 : param0 / Math.pow(1024.0, param2.ordinal());
   }

   public static String humanReadable(long param0) {
      int var2 = 1024;
      if (param0 < 1024L) {
         return param0 + " B";
      }

      int var3 = (int)(Math.log(param0) / Math.log(1024.0));
      String var4 = "KMGTPE".charAt(var3 - 1) + "";
      return String.format(Locale.ROOT, "%.1f %sB", param0 / Math.pow(1024.0, var3), var4);
   }

   public static String humanReadable(long param0, Unit param2) {
      return String.format(Locale.ROOT, "%." + (param2 == GB ? "1" : "0") + "f %s", convertTo(param0, param2), param2.name());
   }
}

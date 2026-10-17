package net.minecraft;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;

public class CrashReportCategory {
   private final String title;
   private final List<CrashReportCategory.Entry> entries = Lists.newArrayList();
   private StackTraceElement[] stackTrace = new StackTraceElement[0];

   public CrashReportCategory(String param0) {
      this.title = param0;
   }

   public static String formatLocation(LevelHeightAccessor param0, double param1, double param3, double param5) {
      return String.format(Locale.ROOT, "%.2f,%.2f,%.2f - %s", param1, param3, param5, formatLocation(param0, BlockPos.containing(param1, param3, param5)));
   }

   public static String formatLocation(LevelHeightAccessor param0, BlockPos param1) {
      return formatLocation(param0, param1.getX(), param1.getY(), param1.getZ());
   }

   public static String formatLocation(LevelHeightAccessor param0, int param1, int param2, int param3) {
      StringBuilder var4 = new StringBuilder();

      try {
         var4.append(String.format(Locale.ROOT, "World: (%d,%d,%d)", param1, param2, param3));
      } catch (Throwable var5) {
         var4.append("(Error finding world loc)");
      }

      var4.append(", ");

      try {
         int var5 = SectionPos.blockToSectionCoord(param1);
         int var6 = SectionPos.blockToSectionCoord(param2);
         int var7 = SectionPos.blockToSectionCoord(param3);
         int var8 = param1 & 15;
         int var9 = param2 & 15;
         int var10 = param3 & 15;
         int var11 = SectionPos.sectionToBlockCoord(var5);
         int var12 = param0.getMinBuildHeight();
         int var13 = SectionPos.sectionToBlockCoord(var7);
         int var14 = SectionPos.sectionToBlockCoord(var5 + 1) - 1;
         int var15 = param0.getMaxBuildHeight() - 1;
         int var16 = SectionPos.sectionToBlockCoord(var7 + 1) - 1;
         var4.append(
            String.format(
               Locale.ROOT,
               "Section: (at %d,%d,%d in %d,%d,%d; chunk contains blocks %d,%d,%d to %d,%d,%d)",
               var8,
               var9,
               var10,
               var5,
               var6,
               var7,
               var11,
               var12,
               var13,
               var14,
               var15,
               var16
            )
         );
      } catch (Throwable var5) {
         var4.append("(Error finding chunk loc)");
      }

      var4.append(", ");

      try {
         int var5 = param1 >> 9;
         int var6 = param3 >> 9;
         int var7 = var5 << 5;
         int var8 = var6 << 5;
         int var9 = (var5 + 1 << 5) - 1;
         int var10 = (var6 + 1 << 5) - 1;
         int var11 = var5 << 9;
         int var12 = param0.getMinBuildHeight();
         int var13 = var6 << 9;
         int var14 = (var5 + 1 << 9) - 1;
         int var15 = param0.getMaxBuildHeight() - 1;
         int var16 = (var6 + 1 << 9) - 1;
         var4.append(
            String.format(
               Locale.ROOT,
               "Region: (%d,%d; contains chunks %d,%d to %d,%d, blocks %d,%d,%d to %d,%d,%d)",
               var5,
               var6,
               var7,
               var8,
               var9,
               var10,
               var11,
               var12,
               var13,
               var14,
               var15,
               var16
            )
         );
      } catch (Throwable var5) {
         var4.append("(Error finding world loc)");
      }

      return var4.toString();
   }

   public CrashReportCategory setDetail(String param0, CrashReportDetail<String> param1) {
      try {
         this.setDetail(param0, param1.call());
      } catch (Throwable var3) {
         this.setDetailError(param0, var3);
      }

      return this;
   }

   public CrashReportCategory setDetail(String param0, Object param1) {
      this.entries.add(new CrashReportCategory.Entry(param0, param1));
      return this;
   }

   public void setDetailError(String param0, Throwable param1) {
      this.setDetail(param0, param1);
   }

   public int fillInStackTrace(int param0) {
      StackTraceElement[] var2 = Thread.currentThread().getStackTrace();
      if (var2.length <= 0) {
         return 0;
      }

      this.stackTrace = new StackTraceElement[var2.length - 3 - param0];
      System.arraycopy(var2, 3 + param0, this.stackTrace, 0, this.stackTrace.length);
      return this.stackTrace.length;
   }

   public boolean validateStackTrace(StackTraceElement param0, StackTraceElement param1) {
      if (this.stackTrace.length != 0 && param0 != null) {
         StackTraceElement var3 = this.stackTrace[0];
         if (var3.isNativeMethod() == param0.isNativeMethod()
            && var3.getClassName().equals(param0.getClassName())
            && var3.getFileName().equals(param0.getFileName())
            && var3.getMethodName().equals(param0.getMethodName())) {
            if (param1 != null != this.stackTrace.length > 1) {
               return false;
            }

            if (param1 != null && !this.stackTrace[1].equals(param1)) {
               return false;
            }

            this.stackTrace[0] = param0;
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void trimStacktrace(int param0) {
      StackTraceElement[] var2 = new StackTraceElement[this.stackTrace.length - param0];
      System.arraycopy(this.stackTrace, 0, var2, 0, var2.length);
      this.stackTrace = var2;
   }

   public void getDetails(StringBuilder param0) {
      param0.append("-- ").append(this.title).append(" --\n");
      param0.append("Details:");

      for (CrashReportCategory.Entry var3 : this.entries) {
         param0.append("\n\t");
         param0.append(var3.getKey());
         param0.append(": ");
         param0.append(var3.getValue());
      }

      if (this.stackTrace != null && this.stackTrace.length > 0) {
         param0.append("\nStacktrace:");

         for (StackTraceElement var5 : this.stackTrace) {
            param0.append("\n\tat ");
            param0.append(var5);
         }
      }
   }

   public StackTraceElement[] getStacktrace() {
      return this.stackTrace;
   }

   public static void populateBlockDetails(CrashReportCategory param0, LevelHeightAccessor param1, BlockPos param2, @Nullable BlockState param3) {
      if (param3 != null) {
         param0.setDetail("Block", param3::toString);
      }

      param0.setDetail("Block location", () -> formatLocation(param1, param2));
   }

   static class Entry {
      private final String key;
      private final String value;

      public Entry(String param0, @Nullable Object param1) {
         this.key = param0;
         if (param1 == null) {
            this.value = "~~NULL~~";
         } else if (param1 instanceof Throwable var3) {
            this.value = "~~ERROR~~ " + var3.getClass().getSimpleName() + ": " + var3.getMessage();
         } else {
            this.value = param1.toString();
         }
      }

      public String getKey() {
         return this.key;
      }

      public String getValue() {
         return this.value;
      }
   }
}

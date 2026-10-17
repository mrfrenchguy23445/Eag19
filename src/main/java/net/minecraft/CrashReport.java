package net.minecraft;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.MemoryReserve;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

public class CrashReport {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
   private final String title;
   private final Throwable exception;
   private final List<CrashReportCategory> details = Lists.newArrayList();
   private File saveFile;
   private boolean trackingStackTrace = true;
   private StackTraceElement[] uncategorizedStackTrace = new StackTraceElement[0];
   private final SystemReport systemReport = new SystemReport();

   public CrashReport(String param0, Throwable param1) {
      this.title = param0;
      this.exception = param1;
   }

   public String getTitle() {
      return this.title;
   }

   public Throwable getException() {
      return this.exception;
   }

   public String getDetails() {
      StringBuilder var1 = new StringBuilder();
      this.getDetails(var1);
      return var1.toString();
   }

   public void getDetails(StringBuilder param0) {
      if ((this.uncategorizedStackTrace == null || this.uncategorizedStackTrace.length <= 0) && !this.details.isEmpty()) {
         this.uncategorizedStackTrace = ArrayUtils.subarray(this.details.get(0).getStacktrace(), 0, 1);
      }

      if (this.uncategorizedStackTrace != null && this.uncategorizedStackTrace.length > 0) {
         param0.append("-- Head --\n");
         param0.append("Thread: ").append(Thread.currentThread().getName()).append("\n");
         param0.append("Stacktrace:\n");

         for (StackTraceElement var5 : this.uncategorizedStackTrace) {
            param0.append("\t").append("at ").append(var5);
            param0.append("\n");
         }

         param0.append("\n");
      }

      for (CrashReportCategory var3 : this.details) {
         var3.getDetails(param0);
         param0.append("\n\n");
      }

      this.systemReport.appendToCrashReportString(param0);
   }

   public String getExceptionMessage() {
      StringWriter var1 = null;
      PrintWriter var2 = null;
      Throwable var3 = this.exception;
      if (var3.getMessage() == null) {
         if (var3 instanceof NullPointerException) {
            var3 = new NullPointerException(this.title);
         } else if (var3 instanceof StackOverflowError) {
            var3 = new StackOverflowError(this.title);
         } else if (var3 instanceof OutOfMemoryError) {
            var3 = new OutOfMemoryError(this.title);
         }

         var3.setStackTrace(this.exception.getStackTrace());
      }

      try {
         var1 = new StringWriter();
         var2 = new PrintWriter(var1);
         var3.printStackTrace(var2);
         return var1.toString();
      } finally {
         IOUtils.closeQuietly(var1);
         IOUtils.closeQuietly(var2);
      }
   }

   public String getFriendlyReport() {
      StringBuilder var1 = new StringBuilder();
      var1.append("---- Minecraft Crash Report ----\n");
      var1.append("// ");
      var1.append(getErrorComment());
      var1.append("\n\n");
      var1.append("Time: ");
      var1.append(DATE_TIME_FORMATTER.format(ZonedDateTime.now()));
      var1.append("\n");
      var1.append("Description: ");
      var1.append(this.title);
      var1.append("\n\n");
      var1.append(this.getExceptionMessage());
      var1.append("\n\nA detailed walkthrough of the error, its code path and all known details is as follows:\n");

      for (int var2 = 0; var2 < 87; var2++) {
         var1.append("-");
      }

      var1.append("\n\n");
      this.getDetails(var1);
      return var1.toString();
   }

   public File getSaveFile() {
      return this.saveFile;
   }

   public boolean saveToFile(File param0) {
      if (this.saveFile != null) {
         return false;
      }

      if (param0.getParentFile() != null) {
         param0.getParentFile().mkdirs();
      }

      Writer var2 = null;

      try {
         var2 = new OutputStreamWriter(new FileOutputStream(param0), StandardCharsets.UTF_8);
         var2.write(this.getFriendlyReport());
         this.saveFile = param0;
         return true;
      } catch (Throwable var3) {
         LOGGER.error("Could not save crash report to {}", param0, var3);
         return false;
      } finally {
         IOUtils.closeQuietly(var2);
      }
   }

   public SystemReport getSystemReport() {
      return this.systemReport;
   }

   public CrashReportCategory addCategory(String param0) {
      return this.addCategory(param0, 1);
   }

   public CrashReportCategory addCategory(String param0, int param1) {
      CrashReportCategory var3 = new CrashReportCategory(param0);
      if (this.trackingStackTrace) {
         int var4 = var3.fillInStackTrace(param1);
         StackTraceElement[] var5 = this.exception.getStackTrace();
         StackTraceElement var6 = null;
         StackTraceElement var7 = null;
         int var8 = var5.length - var4;
         if (var8 < 0) {
            System.out.println("Negative index in crash report handler (" + var5.length + "/" + var4 + ")");
         }

         if (var5 != null && 0 <= var8 && var8 < var5.length) {
            var6 = var5[var8];
            if (var5.length + 1 - var4 < var5.length) {
               var7 = var5[var5.length + 1 - var4];
            }
         }

         this.trackingStackTrace = var3.validateStackTrace(var6, var7);
         if (var5 != null && var5.length >= var4 && 0 <= var8 && var8 < var5.length) {
            this.uncategorizedStackTrace = new StackTraceElement[var8];
            System.arraycopy(var5, 0, this.uncategorizedStackTrace, 0, this.uncategorizedStackTrace.length);
         } else {
            this.trackingStackTrace = false;
         }
      }

      this.details.add(var3);
      return var3;
   }

   private static String getErrorComment() {
      String[] var0 = new String[]{
         "Who set us up the TNT?",
         "Everything's going to plan. No, really, that was supposed to happen.",
         "Uh... Did I do that?",
         "Oops.",
         "Why did you do that?",
         "I feel sad now :(",
         "My bad.",
         "I'm sorry, Dave.",
         "I let you down. Sorry :(",
         "On the bright side, I bought you a teddy bear!",
         "Daisy, daisy...",
         "Oh - I know what I did wrong!",
         "Hey, that tickles! Hehehe!",
         "I blame Dinnerbone.",
         "You should try our sister game, Minceraft!",
         "Don't be sad. I'll do better next time, I promise!",
         "Don't be sad, have a hug! <3",
         "I just don't know what went wrong :(",
         "Shall we play a game?",
         "Quite honestly, I wouldn't worry myself about that.",
         "I bet Cylons wouldn't have this problem.",
         "Sorry :(",
         "Surprise! Haha. Well, this is awkward.",
         "Would you like a cupcake?",
         "Hi. I'm Minecraft, and I'm a crashaholic.",
         "Ooh. Shiny.",
         "This doesn't make any sense!",
         "Why is it breaking :(",
         "Don't do that.",
         "Ouch. That hurt :(",
         "You're mean.",
         "This is a token for 1 free hug. Redeem at your nearest Mojangsta: [~~HUG~~]",
         "There are four lights!",
         "But it works on my machine."
      };

      try {
         return var0[(int)(Util.getNanos() % var0.length)];
      } catch (Throwable var1) {
         return "Witty comment unavailable :(";
      }
   }

   /**
    * Whether a throwable is a {@code CompletionException}, identified by name.
    *
    * <p>Upstream peels {@code java.util.concurrent.CompletionException} wrappers off in
    * {@link #forThrowable} so the crash report names the real cause. That class is absent from the
    * WASM-GC TeaVM classlib, and nothing on this target can produce one: {@code CompletableFuture} is
    * unavailable too, and its uses are replaced with direct execution (see {@code Util}'s executor
    * substitutions). The name test therefore preserves the original intent without a class reference,
    * and is simply inert on this target -- it would start working unchanged if a completion wrapper
    * were ever introduced.
    */
   private static boolean isCompletionException(Throwable param0) {
      return param0.getClass().getName().equals("java.util.concurrent.CompletionException");
   }

   public static CrashReport forThrowable(Throwable param0, String param1) {
      while (isCompletionException(param0) && param0.getCause() != null) {
         param0 = param0.getCause();
      }

      CrashReport var2;
      if (param0 instanceof ReportedException) {
         var2 = ((ReportedException)param0).getReport();
      } else {
         var2 = new CrashReport(param1, param0);
      }

      return var2;
   }

   public static void preload() {
      MemoryReserve.allocate();
      new CrashReport("Don't panic!", new Throwable()).getFriendlyReport();
   }
}

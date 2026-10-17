package net.minecraft.util.profiling;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;

public class FilledProfileResults implements ProfileResults {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ProfilerPathEntry EMPTY = new ProfilerPathEntry() {
      @Override
      public long getDuration() {
         return 0L;
      }

      @Override
      public long getMaxDuration() {
         return 0L;
      }

      @Override
      public long getCount() {
         return 0L;
      }

      @Override
      public Object2LongMap<String> getCounters() {
         return Object2LongMaps.emptyMap();
      }
   };
   private static final Splitter SPLITTER = Splitter.on('\u001e');
   private static final Comparator<Entry<String, FilledProfileResults.CounterCollector>> COUNTER_ENTRY_COMPARATOR = Entry.<String, FilledProfileResults.CounterCollector>comparingByValue(
         Comparator.comparingLong(param0 -> param0.totalValue)
      )
      .reversed();
   private final Map<String, ? extends ProfilerPathEntry> entries;
   private final long startTimeNano;
   private final int startTimeTicks;
   private final long endTimeNano;
   private final int endTimeTicks;
   private final int tickDuration;

   public FilledProfileResults(Map<String, ? extends ProfilerPathEntry> param0, long param1, int param3, long param4, int param6) {
      this.entries = param0;
      this.startTimeNano = param1;
      this.startTimeTicks = param3;
      this.endTimeNano = param4;
      this.endTimeTicks = param6;
      this.tickDuration = param6 - param3;
   }

   private ProfilerPathEntry getEntry(String param0) {
      ProfilerPathEntry var2 = this.entries.get(param0);
      return var2 != null ? var2 : EMPTY;
   }

   @Override
   public List<ResultField> getTimes(String param0) {
      String var2 = param0;
      ProfilerPathEntry var3 = this.getEntry("root");
      long var4 = var3.getDuration();
      ProfilerPathEntry var6 = this.getEntry(param0);
      long var7 = var6.getDuration();
      long var9 = var6.getCount();
      List<ResultField> var11 = Lists.newArrayList();
      if (!param0.isEmpty()) {
         param0 = param0 + "\u001e";
      }

      long var12 = 0L;

      for (String var15 : this.entries.keySet()) {
         if (isDirectChild(param0, var15)) {
            var12 += this.getEntry(var15).getDuration();
         }
      }

      float var14 = (float)var12;
      if (var12 < var7) {
         var12 = var7;
      }

      if (var4 < var12) {
         var4 = var12;
      }

      for (String var16 : this.entries.keySet()) {
         if (isDirectChild(param0, var16)) {
            ProfilerPathEntry var17 = this.getEntry(var16);
            long var18 = var17.getDuration();
            double var20 = var18 * 100.0 / var12;
            double var22 = var18 * 100.0 / var4;
            String var24 = var16.substring(param0.length());
            var11.add(new ResultField(var24, var20, var22, var17.getCount()));
         }
      }

      if ((float)var12 > var14) {
         var11.add(new ResultField("unspecified", ((float)var12 - var14) * 100.0 / var12, ((float)var12 - var14) * 100.0 / var4, var9));
      }

      Collections.sort(var11);
      var11.add(0, new ResultField(var2, 100.0, var12 * 100.0 / var4, var9));
      return var11;
   }

   private static boolean isDirectChild(String param0, String param1) {
      return param1.length() > param0.length() && param1.startsWith(param0) && param1.indexOf(30, param0.length() + 1) < 0;
   }

   private Map<String, FilledProfileResults.CounterCollector> getCounterValues() {
      Map<String, FilledProfileResults.CounterCollector> var1 = Maps.newTreeMap();
      this.entries
         .forEach(
            (l_param1, l_param2) -> {
               Object2LongMap<String> l_var3 = l_param2.getCounters();
               if (!l_var3.isEmpty()) {
                  List<String> l_var4 = SPLITTER.splitToList(l_param1);
                  l_var3.forEach(
                     (l2_param2, l2_param3) -> var1.computeIfAbsent(l2_param2, l3_param0 -> new FilledProfileResults.CounterCollector())
                        .addValue(l_var4.iterator(), l2_param3)
                  );
               }
            }
         );
      return var1;
   }

   @Override
   public long getStartTimeNano() {
      return this.startTimeNano;
   }

   @Override
   public int getStartTimeTicks() {
      return this.startTimeTicks;
   }

   @Override
   public long getEndTimeNano() {
      return this.endTimeNano;
   }

   @Override
   public int getEndTimeTicks() {
      return this.endTimeTicks;
   }

   @Override
   public boolean saveResults(Path param0) {
      Writer var2 = null;

      try {
         Files.createDirectories(param0.getParent());
         var2 = Files.newBufferedWriter(param0, StandardCharsets.UTF_8);
         var2.write(this.getProfilerResults(this.getNanoDuration(), this.getTickDuration()));
         return true;
      } catch (Throwable var3) {
         LOGGER.error("Could not save profiler results to {}", param0, var3);
         return false;
      } finally {
         IOUtils.closeQuietly(var2);
      }
   }

   protected String getProfilerResults(long param0, int param2) {
      StringBuilder var4 = new StringBuilder();
      var4.append("---- Minecraft Profiler Results ----\n");
      var4.append("// ");
      var4.append(getComment());
      var4.append("\n\n");
      var4.append("Version: ").append(SharedConstants.getCurrentVersion().getId()).append('\n');
      var4.append("Time span: ").append(param0 / 1000000L).append(" ms\n");
      var4.append("Tick span: ").append(param2).append(" ticks\n");
      var4.append("// This is approximately ")
         .append(String.format(Locale.ROOT, "%.2f", param2 / ((float)param0 / 1.0E9F)))
         .append(" ticks per second. It should be ")
         .append(20)
         .append(" ticks per second\n\n");
      var4.append("--- BEGIN PROFILE DUMP ---\n\n");
      this.appendProfilerResults(0, "root", var4);
      var4.append("--- END PROFILE DUMP ---\n\n");
      Map<String, FilledProfileResults.CounterCollector> var5 = this.getCounterValues();
      if (!var5.isEmpty()) {
         var4.append("--- BEGIN COUNTER DUMP ---\n\n");
         this.appendCounters(var5, var4, param2);
         var4.append("--- END COUNTER DUMP ---\n\n");
      }

      return var4.toString();
   }

   @Override
   public String getProfilerResults() {
      StringBuilder var1 = new StringBuilder();
      this.appendProfilerResults(0, "root", var1);
      return var1.toString();
   }

   private static StringBuilder indentLine(StringBuilder param0, int param1) {
      param0.append(String.format(Locale.ROOT, "[%02d] ", param1));

      for (int var2 = 0; var2 < param1; var2++) {
         param0.append("|   ");
      }

      return param0;
   }

   private void appendProfilerResults(int param0, String param1, StringBuilder param2) {
      List<ResultField> var4 = this.getTimes(param1);
      Object2LongMap<String> var5 = ObjectUtils.firstNonNull(this.entries.get(param1), EMPTY).getCounters();
      var5.forEach(
         (l_param2, l_param3) -> indentLine(param2, param0)
            .append('#')
            .append(l_param2)
            .append(' ')
            .append(l_param3)
            .append('/')
            .append(l_param3 / this.tickDuration)
            .append('\n')
      );
      if (var4.size() >= 3) {
         for (int var6 = 1; var6 < var4.size(); var6++) {
            ResultField var7 = var4.get(var6);
            indentLine(param2, param0)
               .append(var7.name)
               .append('(')
               .append(var7.count)
               .append('/')
               .append(String.format(Locale.ROOT, "%.0f", (float)var7.count / this.tickDuration))
               .append(')')
               .append(" - ")
               .append(String.format(Locale.ROOT, "%.2f", var7.percentage))
               .append("%/")
               .append(String.format(Locale.ROOT, "%.2f", var7.globalPercentage))
               .append("%\n");
            if (!"unspecified".equals(var7.name)) {
               try {
                  this.appendProfilerResults(param0 + 1, param1 + "\u001e" + var7.name, param2);
               } catch (Exception var8) {
                  param2.append("[[ EXCEPTION ").append(var8).append(" ]]");
               }
            }
         }
      }
   }

   private void appendCounterResults(int param0, String param1, FilledProfileResults.CounterCollector param2, int param3, StringBuilder param4) {
      indentLine(param4, param0)
         .append(param1)
         .append(" total:")
         .append(param2.selfValue)
         .append('/')
         .append(param2.totalValue)
         .append(" average: ")
         .append(param2.selfValue / param3)
         .append('/')
         .append(param2.totalValue / param3)
         .append('\n');
      param2.children
         .entrySet()
         .stream()
         .sorted(COUNTER_ENTRY_COMPARATOR)
         .forEach(l_param3 -> this.appendCounterResults(param0 + 1, l_param3.getKey(), l_param3.getValue(), param3, param4));
   }

   private void appendCounters(Map<String, FilledProfileResults.CounterCollector> param0, StringBuilder param1, int param2) {
      param0.forEach((l_param2, l_param3) -> {
         param1.append("-- Counter: ").append(l_param2).append(" --\n");
         this.appendCounterResults(0, "root", l_param3.children.get("root"), param2, param1);
         param1.append("\n\n");
      });
   }

   private static String getComment() {
      String[] var0 = new String[]{
         "I'd Rather Be Surfing",
         "Shiny numbers!",
         "Am I not running fast enough? :(",
         "I'm working as hard as I can!",
         "Will I ever be good enough for you? :(",
         "Speedy. Zoooooom!",
         "Hello world",
         "40% better than a crash report.",
         "Now with extra numbers",
         "Now with less numbers",
         "Now with the same numbers",
         "You should add flames to things, it makes them go faster!",
         "Do you feel the need for... optimization?",
         "*cracks redstone whip*",
         "Maybe if you treated it better then it'll have more motivation to work faster! Poor server."
      };

      try {
         return var0[(int)(Util.getNanos() % var0.length)];
      } catch (Throwable var1) {
         return "Witty comment unavailable :(";
      }
   }

   @Override
   public int getTickDuration() {
      return this.tickDuration;
   }

   static class CounterCollector {
      long selfValue;
      long totalValue;
      final Map<String, FilledProfileResults.CounterCollector> children = Maps.newHashMap();

      public void addValue(Iterator<String> param0, long param1) {
         this.totalValue += param1;
         if (!param0.hasNext()) {
            this.selfValue += param1;
         } else {
            this.children.computeIfAbsent(param0.next(), l_param0 -> new FilledProfileResults.CounterCollector()).addValue(param0, param1);
         }
      }
   }
}

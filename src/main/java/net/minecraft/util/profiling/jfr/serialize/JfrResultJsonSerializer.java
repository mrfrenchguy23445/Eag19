package net.minecraft.util.profiling.jfr.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.LongSerializationPolicy;
import com.mojang.datafixers.util.Pair;
import java.time.Duration;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import net.minecraft.Util;
import net.minecraft.util.profiling.jfr.Percentiles;
import net.minecraft.util.profiling.jfr.parse.JfrStatsResult;
import net.minecraft.util.profiling.jfr.stats.ChunkGenStat;
import net.minecraft.util.profiling.jfr.stats.CpuLoadStat;
import net.minecraft.util.profiling.jfr.stats.FileIOStat;
import net.minecraft.util.profiling.jfr.stats.GcHeapStat;
import net.minecraft.util.profiling.jfr.stats.NetworkPacketSummary;
import net.minecraft.util.profiling.jfr.stats.ThreadAllocationStat;
import net.minecraft.util.profiling.jfr.stats.TickTimeStat;
import net.minecraft.util.profiling.jfr.stats.TimedStatSummary;
import net.minecraft.world.level.chunk.ChunkStatus;

public class JfrResultJsonSerializer {
   private static final String BYTES_PER_SECOND = "bytesPerSecond";
   private static final String COUNT = "count";
   private static final String DURATION_NANOS_TOTAL = "durationNanosTotal";
   private static final String TOTAL_BYTES = "totalBytes";
   private static final String COUNT_PER_SECOND = "countPerSecond";
   final Gson gson = new GsonBuilder().setPrettyPrinting().setLongSerializationPolicy(LongSerializationPolicy.DEFAULT).create();

   public String format(JfrStatsResult param0) {
      JsonObject var2 = new JsonObject();
      var2.addProperty("startedEpoch", param0.recordingStarted().toEpochMilli());
      var2.addProperty("endedEpoch", param0.recordingEnded().toEpochMilli());
      var2.addProperty("durationMs", param0.recordingDuration().toMillis());
      Duration var3 = param0.worldCreationDuration();
      if (var3 != null) {
         var2.addProperty("worldGenDurationMs", var3.toMillis());
      }

      var2.add("heap", this.heap(param0.heapSummary()));
      var2.add("cpuPercent", this.cpu(param0.cpuLoadStats()));
      var2.add("network", this.network(param0));
      var2.add("fileIO", this.fileIO(param0));
      var2.add("serverTick", this.serverTicks(param0.tickTimes()));
      var2.add("threadAllocation", this.threadAllocations(param0.threadAllocationSummary()));
      var2.add("chunkGen", this.chunkGen(param0.chunkGenSummary()));
      return this.gson.toJson(var2);
   }

   private JsonElement heap(GcHeapStat.Summary param0) {
      JsonObject var2 = new JsonObject();
      var2.addProperty("allocationRateBytesPerSecond", param0.allocationRateBytesPerSecond());
      var2.addProperty("gcCount", param0.totalGCs());
      var2.addProperty("gcOverHeadPercent", param0.gcOverHead());
      var2.addProperty("gcTotalDurationMs", param0.gcTotalDuration().toMillis());
      return var2;
   }

   private JsonElement chunkGen(List<Pair<ChunkStatus, TimedStatSummary<ChunkGenStat>>> param0) {
      JsonObject var2 = new JsonObject();
      var2.addProperty("durationNanosTotal", param0.stream().mapToDouble(l_param0 -> l_param0.getSecond().totalDuration().toNanos()).sum());
      JsonArray var3 = Util.make(new JsonArray(), l_param1 -> var2.add("status", l_param1));

      for (Pair<ChunkStatus, TimedStatSummary<ChunkGenStat>> var5 : param0) {
         TimedStatSummary<ChunkGenStat> var6 = var5.getSecond();
         JsonObject var7 = Util.make(new JsonObject(), var3::add);
         var7.addProperty("state", var5.getFirst().getName());
         var7.addProperty("count", var6.count());
         var7.addProperty("durationNanosTotal", var6.totalDuration().toNanos());
         var7.addProperty("durationNanosAvg", var6.totalDuration().toNanos() / var6.count());
         JsonObject var8 = Util.make(new JsonObject(), l_param1 -> var7.add("durationNanosPercentiles", l_param1));
         var6.percentilesNanos().forEach((l_param1, l_param2) -> var8.addProperty("p" + l_param1, l_param2));
         Function<ChunkGenStat, JsonElement> var9 = l_param0 -> {
            JsonObject l_var1 = new JsonObject();
            l_var1.addProperty("durationNanos", l_param0.duration().toNanos());
            l_var1.addProperty("level", l_param0.level());
            l_var1.addProperty("chunkPosX", l_param0.chunkPos().x);
            l_var1.addProperty("chunkPosZ", l_param0.chunkPos().z);
            l_var1.addProperty("worldPosX", l_param0.worldPos().x());
            l_var1.addProperty("worldPosZ", l_param0.worldPos().z());
            return l_var1;
         };
         var7.add("fastest", var9.apply(var6.fastest()));
         var7.add("slowest", var9.apply(var6.slowest()));
         var7.add("secondSlowest", var6.secondSlowest() != null ? var9.apply(var6.secondSlowest()) : JsonNull.INSTANCE);
      }

      return var2;
   }

   private JsonElement threadAllocations(ThreadAllocationStat.Summary param0) {
      JsonArray var2 = new JsonArray();
      param0.allocationsPerSecondByThread().forEach((l_param1, l_param2) -> var2.add(Util.make(new JsonObject(), l2_param2 -> {
         l2_param2.addProperty("thread", l_param1);
         l2_param2.addProperty("bytesPerSecond", l_param2);
      })));
      return var2;
   }

   private JsonElement serverTicks(List<TickTimeStat> param0) {
      if (param0.isEmpty()) {
         return JsonNull.INSTANCE;
      }

      JsonObject var2 = new JsonObject();
      double[] var3 = param0.stream().mapToDouble(l_param0 -> l_param0.currentAverage().toNanos() / 1000000.0).toArray();
      DoubleSummaryStatistics var4 = DoubleStream.of(var3).summaryStatistics();
      var2.addProperty("minMs", var4.getMin());
      var2.addProperty("averageMs", var4.getAverage());
      var2.addProperty("maxMs", var4.getMax());
      Map<Integer, Double> var5 = Percentiles.evaluate(var3);
      var5.forEach((l_param1, l_param2) -> var2.addProperty("p" + l_param1, l_param2));
      return var2;
   }

   private JsonElement fileIO(JfrStatsResult param0) {
      JsonObject var2 = new JsonObject();
      var2.add("write", this.fileIoSummary(param0.fileWrites()));
      var2.add("read", this.fileIoSummary(param0.fileReads()));
      return var2;
   }

   private JsonElement fileIoSummary(FileIOStat.Summary param0) {
      JsonObject var2 = new JsonObject();
      var2.addProperty("totalBytes", param0.totalBytes());
      var2.addProperty("count", param0.counts());
      var2.addProperty("bytesPerSecond", param0.bytesPerSecond());
      var2.addProperty("countPerSecond", param0.countsPerSecond());
      JsonArray var3 = new JsonArray();
      var2.add("topContributors", var3);
      param0.topTenContributorsByTotalBytes().forEach(l_param1 -> {
         JsonObject l_var2 = new JsonObject();
         var3.add(l_var2);
         l_var2.addProperty("path", l_param1.getFirst());
         l_var2.addProperty("totalBytes", l_param1.getSecond());
      });
      return var2;
   }

   private JsonElement network(JfrStatsResult param0) {
      JsonObject var2 = new JsonObject();
      var2.add("sent", this.packets(param0.sentPacketsSummary()));
      var2.add("received", this.packets(param0.receivedPacketsSummary()));
      return var2;
   }

   private JsonElement packets(NetworkPacketSummary param0) {
      JsonObject var2 = new JsonObject();
      var2.addProperty("totalBytes", param0.getTotalSize());
      var2.addProperty("count", param0.getTotalCount());
      var2.addProperty("bytesPerSecond", param0.getSizePerSecond());
      var2.addProperty("countPerSecond", param0.getCountsPerSecond());
      JsonArray var3 = new JsonArray();
      var2.add("topContributors", var3);
      param0.largestSizeContributors().forEach(l_param1 -> {
         JsonObject l_var2 = new JsonObject();
         var3.add(l_var2);
         NetworkPacketSummary.PacketIdentification l_var3 = l_param1.getFirst();
         NetworkPacketSummary.PacketCountAndSize l_var4 = l_param1.getSecond();
         l_var2.addProperty("protocolId", l_var3.protocolId());
         l_var2.addProperty("packetId", l_var3.packetId());
         l_var2.addProperty("packetName", l_var3.packetName());
         l_var2.addProperty("totalBytes", l_var4.totalSize());
         l_var2.addProperty("count", l_var4.totalCount());
      });
      return var2;
   }

   private JsonElement cpu(List<CpuLoadStat> param0) {
      JsonObject var2 = new JsonObject();
      BiFunction<List<CpuLoadStat>, ToDoubleFunction<CpuLoadStat>, JsonObject> var3 = (l_param0, l_param1) -> {
         JsonObject l_var2 = new JsonObject();
         DoubleSummaryStatistics l_var3 = l_param0.stream().mapToDouble(l_param1).summaryStatistics();
         l_var2.addProperty("min", l_var3.getMin());
         l_var2.addProperty("average", l_var3.getAverage());
         l_var2.addProperty("max", l_var3.getMax());
         return l_var2;
      };
      var2.add("jvm", var3.apply(param0, CpuLoadStat::jvm));
      var2.add("userJvm", var3.apply(param0, CpuLoadStat::userJvm));
      var2.add("system", var3.apply(param0, CpuLoadStat::system));
      return var2;
   }
}

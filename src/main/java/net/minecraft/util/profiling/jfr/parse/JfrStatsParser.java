package net.minecraft.util.profiling.jfr.parse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.minecraft.util.profiling.jfr.stats.ChunkGenStat;
import net.minecraft.util.profiling.jfr.stats.CpuLoadStat;
import net.minecraft.util.profiling.jfr.stats.FileIOStat;
import net.minecraft.util.profiling.jfr.stats.GcHeapStat;
import net.minecraft.util.profiling.jfr.stats.NetworkPacketSummary;
import net.minecraft.util.profiling.jfr.stats.ThreadAllocationStat;
import net.minecraft.util.profiling.jfr.stats.TickTimeStat;

public class JfrStatsParser {
   private Instant recordingStarted = Instant.EPOCH;
   private Instant recordingEnded = Instant.EPOCH;
   private final List<ChunkGenStat> chunkGenStats = Lists.newArrayList();
   private final List<CpuLoadStat> cpuLoadStat = Lists.newArrayList();
   private final Map<NetworkPacketSummary.PacketIdentification, JfrStatsParser.MutableCountAndSize> receivedPackets = Maps.newHashMap();
   private final Map<NetworkPacketSummary.PacketIdentification, JfrStatsParser.MutableCountAndSize> sentPackets = Maps.newHashMap();
   private final List<FileIOStat> fileWrites = Lists.newArrayList();
   private final List<FileIOStat> fileReads = Lists.newArrayList();
   private int garbageCollections;
   private Duration gcTotalDuration = Duration.ZERO;
   private final List<GcHeapStat> gcHeapStats = Lists.newArrayList();
   private final List<ThreadAllocationStat> threadAllocationStats = Lists.newArrayList();
   private final List<TickTimeStat> tickTimes = Lists.newArrayList();
   @Nullable
   private Duration worldCreationDuration = null;

   private JfrStatsParser(Stream<RecordedEvent> param0) {
      this.capture(param0);
   }

   public static JfrStatsResult parse(Path param0) {
      try (final RecordingFile var1 = new RecordingFile(param0)) {
         Iterator<RecordedEvent> var2 = new Iterator<RecordedEvent>() {
            @Override
            public boolean hasNext() {
               return var1.hasMoreEvents();
            }

            public RecordedEvent next() {
               if (!this.hasNext()) {
                  throw new NoSuchElementException();
               }

               try {
                  return var1.readEvent();
               } catch (IOException l_var1) {
                  throw new UncheckedIOException(l_var1);
               }
            }
         };
         Stream<RecordedEvent> var3 = StreamSupport.stream(Spliterators.spliteratorUnknownSize(var2, 1297), false);
         return new JfrStatsParser(var3).results();
      } catch (IOException var1) {
         throw new UncheckedIOException(var1);
      }
   }

   private JfrStatsResult results() {
      Duration var1 = Duration.between(this.recordingStarted, this.recordingEnded);
      return new JfrStatsResult(
         this.recordingStarted,
         this.recordingEnded,
         var1,
         this.worldCreationDuration,
         this.tickTimes,
         this.cpuLoadStat,
         GcHeapStat.summary(var1, this.gcHeapStats, this.gcTotalDuration, this.garbageCollections),
         ThreadAllocationStat.summary(this.threadAllocationStats),
         collectPacketStats(var1, this.receivedPackets),
         collectPacketStats(var1, this.sentPackets),
         FileIOStat.summary(var1, this.fileWrites),
         FileIOStat.summary(var1, this.fileReads),
         this.chunkGenStats
      );
   }

   private void capture(Stream<RecordedEvent> param0) {
      param0.forEach(l_param0 -> {
         if (l_param0.getEndTime().isAfter(this.recordingEnded) || this.recordingEnded.equals(Instant.EPOCH)) {
            this.recordingEnded = l_param0.getEndTime();
         }

         if (l_param0.getStartTime().isBefore(this.recordingStarted) || this.recordingStarted.equals(Instant.EPOCH)) {
            this.recordingStarted = l_param0.getStartTime();
         }

         switch (l_param0.getEventType().getName()) {
            case "minecraft.ChunkGeneration":
               this.chunkGenStats.add(ChunkGenStat.from(l_param0));
               break;
            case "minecraft.LoadWorld":
               this.worldCreationDuration = l_param0.getDuration();
               break;
            case "minecraft.ServerTickTime":
               this.tickTimes.add(TickTimeStat.from(l_param0));
               break;
            case "minecraft.PacketReceived":
               this.incrementPacket(l_param0, l_param0.getInt("bytes"), this.receivedPackets);
               break;
            case "minecraft.PacketSent":
               this.incrementPacket(l_param0, l_param0.getInt("bytes"), this.sentPackets);
               break;
            case "jdk.ThreadAllocationStatistics":
               this.threadAllocationStats.add(ThreadAllocationStat.from(l_param0));
               break;
            case "jdk.GCHeapSummary":
               this.gcHeapStats.add(GcHeapStat.from(l_param0));
               break;
            case "jdk.CPULoad":
               this.cpuLoadStat.add(CpuLoadStat.from(l_param0));
               break;
            case "jdk.FileWrite":
               this.appendFileIO(l_param0, this.fileWrites, "bytesWritten");
               break;
            case "jdk.FileRead":
               this.appendFileIO(l_param0, this.fileReads, "bytesRead");
               break;
            case "jdk.GarbageCollection":
               this.garbageCollections++;
               this.gcTotalDuration = this.gcTotalDuration.plus(l_param0.getDuration());
         }
      });
   }

   private void incrementPacket(RecordedEvent param0, int param1, Map<NetworkPacketSummary.PacketIdentification, JfrStatsParser.MutableCountAndSize> param2) {
      param2.computeIfAbsent(NetworkPacketSummary.PacketIdentification.from(param0), l_param0 -> new JfrStatsParser.MutableCountAndSize()).increment(param1);
   }

   private void appendFileIO(RecordedEvent param0, List<FileIOStat> param1, String param2) {
      param1.add(new FileIOStat(param0.getDuration(), param0.getString("path"), param0.getLong(param2)));
   }

   private static NetworkPacketSummary collectPacketStats(
      Duration param0, Map<NetworkPacketSummary.PacketIdentification, JfrStatsParser.MutableCountAndSize> param1
   ) {
      List<Pair<NetworkPacketSummary.PacketIdentification, NetworkPacketSummary.PacketCountAndSize>> var2 = param1.entrySet()
         .stream()
         .map(l_param0 -> Pair.of(l_param0.getKey(), l_param0.getValue().toCountAndSize()))
         .toList();
      return new NetworkPacketSummary(param0, var2);
   }

   public static final class MutableCountAndSize {
      private long count;
      private long totalSize;

      public void increment(int param0) {
         this.totalSize += param0;
         this.count++;
      }

      public NetworkPacketSummary.PacketCountAndSize toCountAndSize() {
         return new NetworkPacketSummary.PacketCountAndSize(this.count, this.totalSize);
      }
   }
}

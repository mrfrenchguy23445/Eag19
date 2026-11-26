package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record CpuLoadStat(double jvm, double userJvm, double system) {
   public static CpuLoadStat from(RecordedEvent param0) {
      return new CpuLoadStat(param0.getFloat("jvmSystem"), param0.getFloat("jvmUser"), param0.getFloat("machineTotal"));
   }
}

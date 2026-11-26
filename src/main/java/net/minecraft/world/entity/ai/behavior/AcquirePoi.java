package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableLong;

public class AcquirePoi {
   public static final int SCAN_RANGE = 48;

   public static BehaviorControl<PathfinderMob> create(
      Predicate<Holder<PoiType>> param0, MemoryModuleType<GlobalPos> param1, boolean param2, Optional<Byte> param3
   ) {
      return create(param0, param1, param1, param2, param3);
   }

   public static BehaviorControl<PathfinderMob> create(
      Predicate<Holder<PoiType>> param0, MemoryModuleType<GlobalPos> param1, MemoryModuleType<GlobalPos> param2, boolean param3, Optional<Byte> param4
   ) {
      int var5 = 5;
      int var6 = 20;
      MutableLong var7 = new MutableLong(0L);
      Long2ObjectMap<AcquirePoi.JitteredLinearRetry> var8 = new Long2ObjectOpenHashMap<>();
      OneShot<PathfinderMob> var9 = BehaviorBuilder.create(
         l_param6 -> l_param6.group(l_param6.absent(param2))
            .apply(
               l_param6,
               l2_param5 -> (l3_param6, l3_param7, l3_param8) -> {
                  if (param3 && l3_param7.isBaby()) {
                     return false;
                  }

                  if (var7.getValue() == 0L) {
                     var7.setValue(l3_param6.getGameTime() + l3_param6.random.nextInt(20));
                     return false;
                  }

                  if (l3_param6.getGameTime() < var7.getValue()) {
                     return false;
                  }

                  var7.setValue(l3_param8 + 20L + l3_param6.getRandom().nextInt(20));
                  PoiManager l3_var10 = l3_param6.getPoiManager();
                  var8.long2ObjectEntrySet().removeIf(l4_param2 -> !l4_param2.getValue().isStillValid(l3_param8));
                  Predicate<BlockPos> l3_var11 = l4_param3 -> {
                     AcquirePoi.JitteredLinearRetry l4_var4 = var8.get(l4_param3.asLong());
                     if (l4_var4 == null) {
                        return true;
                     }

                     if (!l4_var4.shouldRetry(l3_param8)) {
                        return false;
                     }

                     l4_var4.markAttempt(l3_param8);
                     return true;
                  };
                  Set<Pair<Holder<PoiType>, BlockPos>> l3_var12 = l3_var10.findAllClosestFirstWithType(
                        param0, l3_var11, l3_param7.blockPosition(), 48, PoiManager.Occupancy.HAS_SPACE
                     )
                     .limit(5L)
                     .collect(Collectors.toSet());
                  Path l3_var13 = findPathToPois(l3_param7, l3_var12);
                  if (l3_var13 != null && l3_var13.canReach()) {
                     BlockPos l3_var14 = l3_var13.getTarget();
                     l3_var10.getType(l3_var14).ifPresent(l4_param8 -> {
                        l3_var10.take(param0, (l5_param1, l5_param2) -> l5_param2.equals(l3_var14), l3_var14, 1);
                        l2_param5.set(GlobalPos.of(l3_param6.dimension(), l3_var14));
                        param4.ifPresent(l5_param2 -> l3_param6.broadcastEntityEvent(l3_param7, l5_param2));
                        var8.clear();
                        DebugPackets.sendPoiTicketCountPacket(l3_param6, l3_var14);
                     });
                  } else {
                     for (Pair<Holder<PoiType>, BlockPos> l3_var15 : l3_var12) {
                        var8.computeIfAbsent(l3_var15.getSecond().asLong(), l4_param3 -> new AcquirePoi.JitteredLinearRetry(l3_param6.random, l3_param8));
                     }
                  }

                  return true;
               }
            )
      );
      return param2 == param1 ? var9 : BehaviorBuilder.create(l_param2 -> l_param2.group(l_param2.absent(param1)).apply(l_param2, l2_param1 -> var9));
   }

   @Nullable
   public static Path findPathToPois(Mob param0, Set<Pair<Holder<PoiType>, BlockPos>> param1) {
      if (param1.isEmpty()) {
         return null;
      }

      Set<BlockPos> var2 = new HashSet<>();
      int var3 = 1;

      for (Pair<Holder<PoiType>, BlockPos> var5 : param1) {
         var3 = Math.max(var3, var5.getFirst().value().validRange());
         var2.add(var5.getSecond());
      }

      return param0.getNavigation().createPath(var2, var3);
   }

   static class JitteredLinearRetry {
      private static final int MIN_INTERVAL_INCREASE = 40;
      private static final int MAX_INTERVAL_INCREASE = 80;
      private static final int MAX_RETRY_PATHFINDING_INTERVAL = 400;
      private final RandomSource random;
      private long previousAttemptTimestamp;
      private long nextScheduledAttemptTimestamp;
      private int currentDelay;

      JitteredLinearRetry(RandomSource param0, long param1) {
         this.random = param0;
         this.markAttempt(param1);
      }

      public void markAttempt(long param0) {
         this.previousAttemptTimestamp = param0;
         int var3 = this.currentDelay + this.random.nextInt(40) + 40;
         this.currentDelay = Math.min(var3, 400);
         this.nextScheduledAttemptTimestamp = param0 + this.currentDelay;
      }

      public boolean isStillValid(long param0) {
         return param0 - this.previousAttemptTimestamp < 400L;
      }

      public boolean shouldRetry(long param0) {
         return param0 >= this.nextScheduledAttemptTimestamp;
      }

      @Override
      public String toString() {
         return "RetryMarker{, previousAttemptAt="
            + this.previousAttemptTimestamp
            + ", nextScheduledAttemptAt="
            + this.nextScheduledAttemptTimestamp
            + ", currentDelay="
            + this.currentDelay
            + "}";
      }
   }
}

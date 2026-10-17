package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;

public class SetClosestHomeAsWalkTarget {
   private static final int CACHE_TIMEOUT = 40;
   private static final int BATCH_SIZE = 5;
   private static final int RATE = 20;
   private static final int OK_DISTANCE_SQR = 4;

   public static BehaviorControl<PathfinderMob> create(float param0) {
      Long2LongMap var1 = new Long2LongOpenHashMap();
      MutableLong var2 = new MutableLong(0L);
      return BehaviorBuilder.create(
         l_param3 -> l_param3.group(l_param3.absent(MemoryModuleType.WALK_TARGET), l_param3.absent(MemoryModuleType.HOME))
            .apply(
               l_param3,
               (l2_param3, l2_param4) -> (l3_param4, l3_param5, l3_param6) -> {
                  if (l3_param4.getGameTime() - var2.getValue() < 20L) {
                     return false;
                  }

                  PoiManager l3_var8 = l3_param4.getPoiManager();
                  Optional<BlockPos> l3_var9 = l3_var8.findClosest(
                     l4_param0 -> l4_param0.is(PoiTypes.HOME), l3_param5.blockPosition(), 48, PoiManager.Occupancy.ANY
                  );
                  if (!l3_var9.isEmpty() && !(l3_var9.get().distSqr(l3_param5.blockPosition()) <= 4.0)) {
                     MutableInt l3_var10 = new MutableInt(0);
                     var2.setValue(l3_param4.getGameTime() + l3_param4.getRandom().nextInt(20));
                     Predicate<BlockPos> l3_var11 = l4_param3 -> {
                        long l4_var4 = l4_param3.asLong();
                        if (var1.containsKey(l4_var4)) {
                           return false;
                        }

                        if (l3_var10.incrementAndGet() >= 5) {
                           return false;
                        }

                        var1.put(l4_var4, var2.getValue() + 40L);
                        return true;
                     };
                     Set<Pair<Holder<PoiType>, BlockPos>> l3_var12 = l3_var8.findAllWithType(
                           l4_param0 -> l4_param0.is(PoiTypes.HOME), l3_var11, l3_param5.blockPosition(), 48, PoiManager.Occupancy.ANY
                        )
                        .collect(Collectors.toSet());
                     Path l3_var13 = AcquirePoi.findPathToPois(l3_param5, l3_var12);
                     if (l3_var13 != null && l3_var13.canReach()) {
                        BlockPos l3_var14 = l3_var13.getTarget();
                        Optional<Holder<PoiType>> l3_var15 = l3_var8.getType(l3_var14);
                        if (l3_var15.isPresent()) {
                           l2_param3.set(new WalkTarget(l3_var14, param0, 1));
                           DebugPackets.sendPoiTicketCountPacket(l3_param4, l3_var14);
                        }
                     } else if (l3_var10.getValue() < 5) {
                        var1.long2LongEntrySet().removeIf(l4_param1 -> l4_param1.getLongValue() < var2.getValue());
                     }

                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }
}

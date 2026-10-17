package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.pathfinder.Path;

public class NearestBedSensor extends Sensor<Mob> {
   private static final int CACHE_TIMEOUT = 40;
   private static final int BATCH_SIZE = 5;
   private static final int RATE = 20;
   private final Long2LongMap batchCache = new Long2LongOpenHashMap();
   private int triedCount;
   private long lastUpdate;

   public NearestBedSensor() {
      super(20);
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_BED);
   }

   protected void doTick(ServerLevel param0, Mob param1) {
      if (param1.isBaby()) {
         this.triedCount = 0;
         this.lastUpdate = param0.getGameTime() + param0.getRandom().nextInt(20);
         PoiManager var3 = param0.getPoiManager();
         Predicate<BlockPos> var4 = l_param0 -> {
            long l_var2 = l_param0.asLong();
            if (this.batchCache.containsKey(l_var2)) {
               return false;
            }

            if (++this.triedCount >= 5) {
               return false;
            }

            this.batchCache.put(l_var2, this.lastUpdate + 40L);
            return true;
         };
         Set<Pair<Holder<PoiType>, BlockPos>> var5 = var3.findAllWithType(
               l_param0 -> l_param0.is(PoiTypes.HOME), var4, param1.blockPosition(), 48, PoiManager.Occupancy.ANY
            )
            .collect(Collectors.toSet());
         Path var6 = AcquirePoi.findPathToPois(param1, var5);
         if (var6 != null && var6.canReach()) {
            BlockPos var7 = var6.getTarget();
            Optional<Holder<PoiType>> var8 = var3.getType(var7);
            if (var8.isPresent()) {
               param1.getBrain().setMemory(MemoryModuleType.NEAREST_BED, var7);
            }
         } else if (this.triedCount < 5) {
            this.batchCache.long2LongEntrySet().removeIf(l_param0 -> l_param0.getLongValue() < this.lastUpdate);
         }
      }
   }
}

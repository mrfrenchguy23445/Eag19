package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;

public class NearestItemSensor extends Sensor<Mob> {
   private static final long XZ_RANGE = 32L;
   private static final long Y_RANGE = 16L;
   public static final int MAX_DISTANCE_TO_WANTED_ITEM = 32;

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
   }

   protected void doTick(ServerLevel param0, Mob param1) {
      Brain<?> var3 = param1.getBrain();
      List<ItemEntity> var4 = param0.getEntitiesOfClass(ItemEntity.class, param1.getBoundingBox().inflate(32.0, 16.0, 32.0), l_param0 -> true);
      var4.sort(Comparator.comparingDouble(param1::distanceToSqr));
      Optional<ItemEntity> var5 = var4.stream()
         .filter(l_param1 -> param1.wantsToPickUp(l_param1.getItem()))
         .filter(l_param1 -> l_param1.closerThan(param1, 32.0))
         .filter(param1::hasLineOfSight)
         .findFirst();
      var3.setMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, var5);
   }
}

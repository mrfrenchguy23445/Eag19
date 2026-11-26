package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.phys.AABB;

public class NearestLivingEntitySensor<T extends LivingEntity> extends Sensor<T> {
   @Override
   protected void doTick(ServerLevel param0, T param1) {
      AABB var3 = param1.getBoundingBox().inflate(this.radiusXZ(), this.radiusY(), this.radiusXZ());
      List<LivingEntity> var4 = param0.getEntitiesOfClass(LivingEntity.class, var3, l_param1 -> l_param1 != param1 && l_param1.isAlive());
      var4.sort(Comparator.comparingDouble(param1::distanceToSqr));
      Brain<?> var5 = param1.getBrain();
      var5.setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, var4);
      var5.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, new NearestVisibleLivingEntities(param1, var4));
   }

   protected int radiusXZ() {
      return 16;
   }

   protected int radiusY() {
      return 16;
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
   }
}

package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class HurtBySensor extends Sensor<LivingEntity> {
   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY);
   }

   @Override
   protected void doTick(ServerLevel param0, LivingEntity param1) {
      Brain<?> var3 = param1.getBrain();
      DamageSource var4 = param1.getLastDamageSource();
      if (var4 != null) {
         var3.setMemory(MemoryModuleType.HURT_BY, param1.getLastDamageSource());
         Entity var5 = var4.getEntity();
         if (var5 instanceof LivingEntity) {
            var3.setMemory(MemoryModuleType.HURT_BY_ENTITY, (LivingEntity)var5);
         }
      } else {
         var3.eraseMemory(MemoryModuleType.HURT_BY);
      }

      var3.getMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent(l_param2 -> {
         if (!l_param2.isAlive() || l_param2.level != param0) {
            var3.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
         }
      });
   }
}

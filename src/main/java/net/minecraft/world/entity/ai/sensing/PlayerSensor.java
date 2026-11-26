package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;

public class PlayerSensor extends Sensor<LivingEntity> {
   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_PLAYERS, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
   }

   @Override
   protected void doTick(ServerLevel param0, LivingEntity param1) {
      List<Player> var3 = param0.players()
         .stream()
         .filter(EntitySelector.NO_SPECTATORS)
         .filter(l_param1 -> param1.closerThan(l_param1, 16.0))
         .sorted(Comparator.comparingDouble(param1::distanceToSqr))
         .collect(Collectors.toList());
      Brain<?> var4 = param1.getBrain();
      var4.setMemory(MemoryModuleType.NEAREST_PLAYERS, var3);
      List<Player> var5 = var3.stream().filter(l_param1 -> isEntityTargetable(param1, l_param1)).collect(Collectors.toList());
      var4.setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, var5.isEmpty() ? null : var5.get(0));
      Optional<Player> var6 = var5.stream().filter(l_param1 -> isEntityAttackable(param1, l_param1)).findFirst();
      var4.setMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, var6);
   }
}

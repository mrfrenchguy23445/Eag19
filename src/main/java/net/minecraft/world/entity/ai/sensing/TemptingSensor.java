package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class TemptingSensor extends Sensor<PathfinderMob> {
   public static final int TEMPTATION_RANGE = 10;
   private static final TargetingConditions TEMPT_TARGETING = TargetingConditions.forNonCombat().range(10.0).ignoreLineOfSight();
   private final Ingredient temptations;

   public TemptingSensor(Ingredient param0) {
      this.temptations = param0;
   }

   protected void doTick(ServerLevel param0, PathfinderMob param1) {
      Brain<?> var3 = param1.getBrain();
      List<Player> var4 = param0.players()
         .stream()
         .filter(EntitySelector.NO_SPECTATORS)
         .filter(l_param1 -> TEMPT_TARGETING.test(param1, l_param1))
         .filter(l_param1 -> param1.closerThan(l_param1, 10.0))
         .filter(this::playerHoldingTemptation)
         .filter(l_param1 -> !param1.hasPassenger(l_param1))
         .sorted(Comparator.comparingDouble(param1::distanceToSqr))
         .collect(Collectors.toList());
      if (!var4.isEmpty()) {
         Player var5 = var4.get(0);
         var3.setMemory(MemoryModuleType.TEMPTING_PLAYER, var5);
      } else {
         var3.eraseMemory(MemoryModuleType.TEMPTING_PLAYER);
      }
   }

   private boolean playerHoldingTemptation(Player param0) {
      return this.isTemptation(param0.getMainHandItem()) || this.isTemptation(param0.getOffhandItem());
   }

   private boolean isTemptation(ItemStack param0) {
      return this.temptations.test(param0);
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.TEMPTING_PLAYER);
   }
}

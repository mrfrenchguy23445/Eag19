package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;

public class FollowTemptation extends Behavior<PathfinderMob> {
   public static final int TEMPTATION_COOLDOWN = 100;
   public static final double CLOSE_ENOUGH_DIST = 2.5;
   private final Function<LivingEntity, Float> speedModifier;

   public FollowTemptation(Function<LivingEntity, Float> param0) {
      super(Util.make(() -> {
         Builder<MemoryModuleType<?>, MemoryStatus> l_var0 = ImmutableMap.builder();
         l_var0.put(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED);
         l_var0.put(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED);
         l_var0.put(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT);
         l_var0.put(MemoryModuleType.IS_TEMPTED, MemoryStatus.REGISTERED);
         l_var0.put(MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_PRESENT);
         l_var0.put(MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT);
         l_var0.put(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT);
         return l_var0.build();
      }));
      this.speedModifier = param0;
   }

   protected float getSpeedModifier(PathfinderMob param0) {
      return this.speedModifier.apply(param0);
   }

   private Optional<Player> getTemptingPlayer(PathfinderMob param0) {
      return param0.getBrain().getMemory(MemoryModuleType.TEMPTING_PLAYER);
   }

   @Override
   protected boolean timedOut(long param0) {
      return false;
   }

   protected boolean canStillUse(ServerLevel param0, PathfinderMob param1, long param2) {
      return this.getTemptingPlayer(param1).isPresent()
         && !param1.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)
         && !param1.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
   }

   protected void start(ServerLevel param0, PathfinderMob param1, long param2) {
      param1.getBrain().setMemory(MemoryModuleType.IS_TEMPTED, true);
   }

   protected void stop(ServerLevel param0, PathfinderMob param1, long param2) {
      Brain<?> var5 = param1.getBrain();
      var5.setMemory(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, 100);
      var5.setMemory(MemoryModuleType.IS_TEMPTED, false);
      var5.eraseMemory(MemoryModuleType.WALK_TARGET);
      var5.eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   protected void tick(ServerLevel param0, PathfinderMob param1, long param2) {
      Player var5 = this.getTemptingPlayer(param1).get();
      Brain<?> var6 = param1.getBrain();
      var6.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(var5, true));
      if (param1.distanceToSqr(var5) < 6.25) {
         var6.eraseMemory(MemoryModuleType.WALK_TARGET);
      } else {
         var6.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(var5, false), this.getSpeedModifier(param1), 2));
      }
   }
}

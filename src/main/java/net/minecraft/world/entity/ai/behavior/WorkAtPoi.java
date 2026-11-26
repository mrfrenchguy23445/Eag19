package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;

public class WorkAtPoi extends Behavior<Villager> {
   private static final int CHECK_COOLDOWN = 300;
   private static final double DISTANCE = 1.73;
   private long lastCheck;

   public WorkAtPoi() {
      super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, Villager param1) {
      if (param0.getGameTime() - this.lastCheck < 300L) {
         return false;
      }

      if (param0.random.nextInt(2) != 0) {
         return false;
      }

      this.lastCheck = param0.getGameTime();
      GlobalPos var3 = param1.getBrain().getMemory(MemoryModuleType.JOB_SITE).get();
      return var3.dimension() == param0.dimension() && var3.pos().closerToCenterThan(param1.position(), 1.73);
   }

   protected void start(ServerLevel param0, Villager param1, long param2) {
      Brain<Villager> var5 = param1.getBrain();
      var5.setMemory(MemoryModuleType.LAST_WORKED_AT_POI, param2);
      var5.getMemory(MemoryModuleType.JOB_SITE).ifPresent(l_param1 -> var5.setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(l_param1.pos())));
      param1.playWorkSound();
      this.useWorkstation(param0, param1);
      if (param1.shouldRestock()) {
         param1.restock();
      }
   }

   protected void useWorkstation(ServerLevel param0, Villager param1) {
   }

   protected boolean canStillUse(ServerLevel param0, Villager param1, long param2) {
      Optional<GlobalPos> var5 = param1.getBrain().getMemory(MemoryModuleType.JOB_SITE);
      if (!var5.isPresent()) {
         return false;
      }

      GlobalPos var6 = var5.get();
      return var6.dimension() == param0.dimension() && var6.pos().closerToCenterThan(param1.position(), 1.73);
   }
}

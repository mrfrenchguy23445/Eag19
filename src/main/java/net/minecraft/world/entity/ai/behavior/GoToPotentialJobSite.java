package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

public class GoToPotentialJobSite extends Behavior<Villager> {
   private static final int TICKS_UNTIL_TIMEOUT = 1200;
   final float speedModifier;

   public GoToPotentialJobSite(float param0) {
      super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_PRESENT), 1200);
      this.speedModifier = param0;
   }

   protected boolean checkExtraStartConditions(ServerLevel param0, Villager param1) {
      return param1.getBrain()
         .getActiveNonCoreActivity()
         .map(l_param0 -> l_param0 == Activity.IDLE || l_param0 == Activity.WORK || l_param0 == Activity.PLAY)
         .orElse(true);
   }

   protected boolean canStillUse(ServerLevel param0, Villager param1, long param2) {
      return param1.getBrain().hasMemoryValue(MemoryModuleType.POTENTIAL_JOB_SITE);
   }

   protected void tick(ServerLevel param0, Villager param1, long param2) {
      BehaviorUtils.setWalkAndLookTargetMemories(param1, param1.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get().pos(), this.speedModifier, 1);
   }

   protected void stop(ServerLevel param0, Villager param1, long param2) {
      Optional<GlobalPos> var5 = param1.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
      var5.ifPresent(l_param1 -> {
         BlockPos l_var2 = l_param1.pos();
         ServerLevel l_var3 = param0.getServer().getLevel(l_param1.dimension());
         if (l_var3 != null) {
            PoiManager l_var4 = l_var3.getPoiManager();
            if (l_var4.exists(l_var2, l2_param0 -> true)) {
               l_var4.release(l_var2);
            }

            DebugPackets.sendPoiTicketCountPacket(param0, l_var2);
         }
      });
      param1.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
   }
}

package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

public class SecondaryPoiSensor extends Sensor<Villager> {
   private static final int SCAN_RATE = 40;

   public SecondaryPoiSensor() {
      super(40);
   }

   protected void doTick(ServerLevel param0, Villager param1) {
      ResourceKey<Level> var3 = param0.dimension();
      BlockPos var4 = param1.blockPosition();
      List<GlobalPos> var5 = Lists.newArrayList();
      int var6 = 4;

      for (int var7 = -4; var7 <= 4; var7++) {
         for (int var8 = -2; var8 <= 2; var8++) {
            for (int var9 = -4; var9 <= 4; var9++) {
               BlockPos var10 = var4.offset(var7, var8, var9);
               if (param1.getVillagerData().getProfession().secondaryPoi().contains(param0.getBlockState(var10).getBlock())) {
                  var5.add(GlobalPos.of(var3, var10));
               }
            }
         }
      }

      Brain<?> var7 = param1.getBrain();
      if (!var5.isEmpty()) {
         var7.setMemory(MemoryModuleType.SECONDARY_JOB_SITE, var5);
      } else {
         var7.eraseMemory(MemoryModuleType.SECONDARY_JOB_SITE);
      }
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.SECONDARY_JOB_SITE);
   }
}

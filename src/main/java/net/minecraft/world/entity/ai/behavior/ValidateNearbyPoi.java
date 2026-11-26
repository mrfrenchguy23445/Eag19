package net.minecraft.world.entity.ai.behavior;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ValidateNearbyPoi {
   private static final int MAX_DISTANCE = 16;

   public static BehaviorControl<LivingEntity> create(Predicate<Holder<PoiType>> param0, MemoryModuleType<GlobalPos> param1) {
      return BehaviorBuilder.create(l_param2 -> l_param2.group(l_param2.present(param1)).apply(l_param2, l2_param2 -> (l3_param3, l3_param4, l3_param5) -> {
         GlobalPos l3_var7 = l_param2.get(l2_param2);
         BlockPos l3_var8 = l3_var7.pos();
         if (l3_param3.dimension() == l3_var7.dimension() && l3_var8.closerToCenterThan(l3_param4.position(), 16.0)) {
            ServerLevel l3_var9 = l3_param3.getServer().getLevel(l3_var7.dimension());
            if (l3_var9 == null || !l3_var9.getPoiManager().exists(l3_var8, param0)) {
               l2_param2.erase();
            } else if (bedIsOccupied(l3_var9, l3_var8, l3_param4)) {
               l2_param2.erase();
               l3_param3.getPoiManager().release(l3_var8);
               DebugPackets.sendPoiTicketCountPacket(l3_param3, l3_var8);
            }

            return true;
         } else {
            return false;
         }
      }));
   }

   private static boolean bedIsOccupied(ServerLevel param0, BlockPos param1, LivingEntity param2) {
      BlockState var3 = param0.getBlockState(param1);
      return var3.is(BlockTags.BEDS) && var3.getValue(BedBlock.OCCUPIED) && !param2.isSleeping();
   }
}

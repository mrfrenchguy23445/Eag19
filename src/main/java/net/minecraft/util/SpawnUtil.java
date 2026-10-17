package net.minecraft.util;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnUtil {
   public static <T extends Mob> Optional<T> trySpawnMob(
      EntityType<T> param0, MobSpawnType param1, ServerLevel param2, BlockPos param3, int param4, int param5, int param6, SpawnUtil.Strategy param7
   ) {
      BlockPos.MutableBlockPos var8 = param3.mutable();

      for (int var9 = 0; var9 < param4; var9++) {
         int var10 = Mth.randomBetweenInclusive(param2.random, -param5, param5);
         int var11 = Mth.randomBetweenInclusive(param2.random, -param5, param5);
         var8.setWithOffset(param3, var10, param6, var11);
         if (param2.getWorldBorder().isWithinBounds(var8) && moveToPossibleSpawnPosition(param2, param6, var8, param7)) {
            T var12 = (T)param0.create(param2, null, null, var8, param1, false, false);
            if (var12 != null) {
               if (var12.checkSpawnRules(param2, param1) && var12.checkSpawnObstruction(param2)) {
                  param2.addFreshEntityWithPassengers(var12);
                  return Optional.of(var12);
               }

               var12.discard();
            }
         }
      }

      return Optional.empty();
   }

   private static boolean moveToPossibleSpawnPosition(ServerLevel param0, int param1, BlockPos.MutableBlockPos param2, SpawnUtil.Strategy param3) {
      BlockPos.MutableBlockPos var4 = new BlockPos.MutableBlockPos().set(param2);
      BlockState var5 = param0.getBlockState(var4);

      for (int var6 = param1; var6 >= -param1; var6--) {
         param2.move(Direction.DOWN);
         var4.setWithOffset(param2, Direction.UP);
         BlockState var7 = param0.getBlockState(param2);
         if (param3.canSpawnOn(param0, param2, var7, var4, var5)) {
            param2.move(Direction.UP);
            return true;
         }

         var5 = var7;
      }

      return false;
   }

   public interface Strategy {
      SpawnUtil.Strategy LEGACY_IRON_GOLEM = (param0, param1, param2, param3, param4) -> (param4.isAir() || param4.getMaterial().isLiquid())
         && param2.getMaterial().isSolidBlocking();
      SpawnUtil.Strategy ON_TOP_OF_COLLIDER = (param0, param1, param2, param3, param4) -> param4.getCollisionShape(param0, param3).isEmpty()
         && Block.isFaceFull(param2.getCollisionShape(param0, param1), Direction.UP);

      boolean canSpawnOn(ServerLevel var1, BlockPos var2, BlockState var3, BlockPos var4, BlockState var5);
   }
}

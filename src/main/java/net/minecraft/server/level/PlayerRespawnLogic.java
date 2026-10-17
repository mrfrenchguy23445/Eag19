package net.minecraft.server.level;

import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public class PlayerRespawnLogic {
   @Nullable
   protected static BlockPos getOverworldRespawnPos(ServerLevel param0, int param1, int param2) {
      boolean var3 = param0.dimensionType().hasCeiling();
      LevelChunk var4 = param0.getChunk(SectionPos.blockToSectionCoord(param1), SectionPos.blockToSectionCoord(param2));
      int var5 = var3
         ? param0.getChunkSource().getGenerator().getSpawnHeight(param0)
         : var4.getHeight(Heightmap.Types.MOTION_BLOCKING, param1 & 15, param2 & 15);
      if (var5 < param0.getMinBuildHeight()) {
         return null;
      }

      int var6 = var4.getHeight(Heightmap.Types.WORLD_SURFACE, param1 & 15, param2 & 15);
      if (var6 <= var5 && var6 > var4.getHeight(Heightmap.Types.OCEAN_FLOOR, param1 & 15, param2 & 15)) {
         return null;
      }

      BlockPos.MutableBlockPos var7 = new BlockPos.MutableBlockPos();

      for (int var8 = var5 + 1; var8 >= param0.getMinBuildHeight(); var8--) {
         var7.set(param1, var8, param2);
         BlockState var9 = param0.getBlockState(var7);
         if (!var9.getFluidState().isEmpty()) {
            break;
         }

         if (Block.isFaceFull(var9.getCollisionShape(param0, var7), Direction.UP)) {
            return var7.above().immutable();
         }
      }

      return null;
   }

   @Nullable
   public static BlockPos getSpawnPosInChunk(ServerLevel param0, ChunkPos param1) {
      if (SharedConstants.debugVoidTerrain(param1)) {
         return null;
      }

      for (int var2 = param1.getMinBlockX(); var2 <= param1.getMaxBlockX(); var2++) {
         for (int var3 = param1.getMinBlockZ(); var3 <= param1.getMaxBlockZ(); var3++) {
            BlockPos var4 = getOverworldRespawnPos(param0, var2, var3);
            if (var4 != null) {
               return var4;
            }
         }
      }

      return null;
   }
}

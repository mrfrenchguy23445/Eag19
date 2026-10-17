package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class VoidStartPlatformFeature extends Feature<NoneFeatureConfiguration> {
   private static final BlockPos PLATFORM_OFFSET = new BlockPos(8, 3, 8);
   private static final ChunkPos PLATFORM_ORIGIN_CHUNK = new ChunkPos(PLATFORM_OFFSET);
   private static final int PLATFORM_RADIUS = 16;
   private static final int PLATFORM_RADIUS_CHUNKS = 1;

   public VoidStartPlatformFeature(Codec<NoneFeatureConfiguration> param0) {
      super(param0);
   }

   private static int checkerboardDistance(int param0, int param1, int param2, int param3) {
      return Math.max(Math.abs(param0 - param2), Math.abs(param1 - param3));
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      ChunkPos var3 = new ChunkPos(param0.origin());
      if (checkerboardDistance(var3.x, var3.z, PLATFORM_ORIGIN_CHUNK.x, PLATFORM_ORIGIN_CHUNK.z) > 1) {
         return true;
      }

      BlockPos var4 = PLATFORM_OFFSET.atY(param0.origin().getY() + PLATFORM_OFFSET.getY());
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos();

      for (int var6 = var3.getMinBlockZ(); var6 <= var3.getMaxBlockZ(); var6++) {
         for (int var7 = var3.getMinBlockX(); var7 <= var3.getMaxBlockX(); var7++) {
            if (checkerboardDistance(var4.getX(), var4.getZ(), var7, var6) <= 16) {
               var5.set(var7, var4.getY(), var6);
               if (var5.equals(var4)) {
                  var2.setBlock(var5, Blocks.COBBLESTONE.defaultBlockState(), 2);
               } else {
                  var2.setBlock(var5, Blocks.STONE.defaultBlockState(), 2);
               }
            }
         }
      }

      return true;
   }
}

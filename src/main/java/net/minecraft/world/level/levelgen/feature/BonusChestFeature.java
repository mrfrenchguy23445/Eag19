package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.stream.IntStream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class BonusChestFeature extends Feature<NoneFeatureConfiguration> {
   public BonusChestFeature(Codec<NoneFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> param0) {
      RandomSource var2 = param0.random();
      WorldGenLevel var3 = param0.level();
      ChunkPos var4 = new ChunkPos(param0.origin());
      IntArrayList var5 = Util.toShuffledList(IntStream.rangeClosed(var4.getMinBlockX(), var4.getMaxBlockX()), var2);
      IntArrayList var6 = Util.toShuffledList(IntStream.rangeClosed(var4.getMinBlockZ(), var4.getMaxBlockZ()), var2);
      BlockPos.MutableBlockPos var7 = new BlockPos.MutableBlockPos();

      for (Integer var9 : var5) {
         for (Integer var11 : var6) {
            var7.set(var9, 0, var11);
            BlockPos var12 = var3.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, var7);
            if (var3.isEmptyBlock(var12) || var3.getBlockState(var12).getCollisionShape(var3, var12).isEmpty()) {
               var3.setBlock(var12, Blocks.CHEST.defaultBlockState(), 2);
               RandomizableContainerBlockEntity.setLootTable(var3, var2, var12, BuiltInLootTables.SPAWN_BONUS_CHEST);
               BlockState var13 = Blocks.TORCH.defaultBlockState();

               for (Direction var15 : Direction.Plane.HORIZONTAL) {
                  BlockPos var16 = var12.relative(var15);
                  if (var13.canSurvive(var3, var16)) {
                     var3.setBlock(var16, var13, 2);
                  }
               }

               return true;
            }
         }
      }

      return false;
   }
}

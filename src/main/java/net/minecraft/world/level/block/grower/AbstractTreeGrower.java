package net.minecraft.world.level.block.grower;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public abstract class AbstractTreeGrower {
   @Nullable
   protected abstract ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource var1, boolean var2);

   public boolean growTree(ServerLevel param0, ChunkGenerator param1, BlockPos param2, BlockState param3, RandomSource param4) {
      ResourceKey<ConfiguredFeature<?, ?>> var6 = this.getConfiguredFeature(param4, this.hasFlowers(param0, param2));
      if (var6 == null) {
         return false;
      }

      Holder<ConfiguredFeature<?, ?>> var7 = param0.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(var6).orElse(null);
      if (var7 == null) {
         return false;
      }

      ConfiguredFeature<?, ?> var8 = var7.value();
      BlockState var9 = param0.getFluidState(param2).createLegacyBlock();
      param0.setBlock(param2, var9, 4);
      if (var8.place(param0, param1, param4, param2)) {
         if (param0.getBlockState(param2) == var9) {
            param0.sendBlockUpdated(param2, param3, var9, 2);
         }

         return true;
      } else {
         param0.setBlock(param2, param3, 4);
         return false;
      }
   }

   private boolean hasFlowers(LevelAccessor param0, BlockPos param1) {
      for (BlockPos var4 : BlockPos.MutableBlockPos.betweenClosed(param1.below().north(2).west(2), param1.above().south(2).east(2))) {
         if (param0.getBlockState(var4).is(BlockTags.FLOWERS)) {
            return true;
         }
      }

      return false;
   }
}

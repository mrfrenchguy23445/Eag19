package net.minecraft.world.level.block.grower;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public abstract class AbstractMegaTreeGrower extends AbstractTreeGrower {
   @Override
   public boolean growTree(ServerLevel param0, ChunkGenerator param1, BlockPos param2, BlockState param3, RandomSource param4) {
      for (int var6 = 0; var6 >= -1; var6--) {
         for (int var7 = 0; var7 >= -1; var7--) {
            if (isTwoByTwoSapling(param3, param0, param2, var6, var7)) {
               return this.placeMega(param0, param1, param2, param3, param4, var6, var7);
            }
         }
      }

      return super.growTree(param0, param1, param2, param3, param4);
   }

   @Nullable
   protected abstract ResourceKey<ConfiguredFeature<?, ?>> getConfiguredMegaFeature(RandomSource var1);

   public boolean placeMega(ServerLevel param0, ChunkGenerator param1, BlockPos param2, BlockState param3, RandomSource param4, int param5, int param6) {
      ResourceKey<ConfiguredFeature<?, ?>> var8 = this.getConfiguredMegaFeature(param4);
      if (var8 == null) {
         return false;
      }

      Holder<ConfiguredFeature<?, ?>> var9 = param0.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(var8).orElse(null);
      if (var9 == null) {
         return false;
      }

      ConfiguredFeature<?, ?> var10 = var9.value();
      BlockState var11 = Blocks.AIR.defaultBlockState();
      param0.setBlock(param2.offset(param5, 0, param6), var11, 4);
      param0.setBlock(param2.offset(param5 + 1, 0, param6), var11, 4);
      param0.setBlock(param2.offset(param5, 0, param6 + 1), var11, 4);
      param0.setBlock(param2.offset(param5 + 1, 0, param6 + 1), var11, 4);
      if (var10.place(param0, param1, param4, param2.offset(param5, 0, param6))) {
         return true;
      }

      param0.setBlock(param2.offset(param5, 0, param6), param3, 4);
      param0.setBlock(param2.offset(param5 + 1, 0, param6), param3, 4);
      param0.setBlock(param2.offset(param5, 0, param6 + 1), param3, 4);
      param0.setBlock(param2.offset(param5 + 1, 0, param6 + 1), param3, 4);
      return false;
   }

   public static boolean isTwoByTwoSapling(BlockState param0, BlockGetter param1, BlockPos param2, int param3, int param4) {
      Block var5 = param0.getBlock();
      return param1.getBlockState(param2.offset(param3, 0, param4)).is(var5)
         && param1.getBlockState(param2.offset(param3 + 1, 0, param4)).is(var5)
         && param1.getBlockState(param2.offset(param3, 0, param4 + 1)).is(var5)
         && param1.getBlockState(param2.offset(param3 + 1, 0, param4 + 1)).is(var5);
   }
}

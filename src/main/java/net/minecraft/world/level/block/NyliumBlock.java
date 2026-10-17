package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.NetherFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.lighting.LayerLightEngine;

public class NyliumBlock extends Block implements BonemealableBlock {
   protected NyliumBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   private static boolean canBeNylium(BlockState param0, LevelReader param1, BlockPos param2) {
      BlockPos var3 = param2.above();
      BlockState var4 = param1.getBlockState(var3);
      int var5 = LayerLightEngine.getLightBlockInto(param1, param0, param2, var4, var3, Direction.UP, var4.getLightBlock(param1, var3));
      return var5 < param1.getMaxLightLevel();
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (!canBeNylium(param0, param1, param2)) {
         param1.setBlockAndUpdate(param2, Blocks.NETHERRACK.defaultBlockState());
      }
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader param0, BlockPos param1, BlockState param2, boolean param3) {
      return param0.getBlockState(param1.above()).isAir();
   }

   @Override
   public boolean isBonemealSuccess(Level param0, RandomSource param1, BlockPos param2, BlockState param3) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel param0, RandomSource param1, BlockPos param2, BlockState param3) {
      BlockState var5 = param0.getBlockState(param2);
      BlockPos var6 = param2.above();
      ChunkGenerator var7 = param0.getChunkSource().getGenerator();
      Registry<ConfiguredFeature<?, ?>> var8 = param0.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
      if (var5.is(Blocks.CRIMSON_NYLIUM)) {
         this.place(var8, NetherFeatures.CRIMSON_FOREST_VEGETATION_BONEMEAL, param0, var7, param1, var6);
      } else if (var5.is(Blocks.WARPED_NYLIUM)) {
         this.place(var8, NetherFeatures.WARPED_FOREST_VEGETATION_BONEMEAL, param0, var7, param1, var6);
         this.place(var8, NetherFeatures.NETHER_SPROUTS_BONEMEAL, param0, var7, param1, var6);
         if (param1.nextInt(8) == 0) {
            this.place(var8, NetherFeatures.TWISTING_VINES_BONEMEAL, param0, var7, param1, var6);
         }
      }
   }

   private void place(
      Registry<ConfiguredFeature<?, ?>> param0,
      ResourceKey<ConfiguredFeature<?, ?>> param1,
      ServerLevel param2,
      ChunkGenerator param3,
      RandomSource param4,
      BlockPos param5
   ) {
      param0.getHolder(param1).ifPresent(l_param4 -> l_param4.value().place(param2, param3, param4, param5));
   }
}

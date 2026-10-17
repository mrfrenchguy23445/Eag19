package net.minecraft.world.level.block;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class GrassBlock extends SpreadingSnowyDirtBlock implements BonemealableBlock {
   public GrassBlock(BlockBehaviour.Properties param0) {
      super(param0);
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
      BlockPos var5 = param2.above();
      BlockState var6 = Blocks.GRASS.defaultBlockState();
      Optional<Holder.Reference<PlacedFeature>> var7 = param0.registryAccess()
         .registryOrThrow(Registries.PLACED_FEATURE)
         .getHolder(VegetationPlacements.GRASS_BONEMEAL);

      label49:
      for (int var8 = 0; var8 < 128; var8++) {
         BlockPos var9 = var5;

         for (int var10 = 0; var10 < var8 / 16; var10++) {
            var9 = var9.offset(param1.nextInt(3) - 1, (param1.nextInt(3) - 1) * param1.nextInt(3) / 2, param1.nextInt(3) - 1);
            if (!param0.getBlockState(var9.below()).is(this) || param0.getBlockState(var9).isCollisionShapeFullBlock(param0, var9)) {
               continue label49;
            }
         }

         BlockState var10 = param0.getBlockState(var9);
         if (var10.is(var6.getBlock()) && param1.nextInt(10) == 0) {
            ((BonemealableBlock)var6.getBlock()).performBonemeal(param0, param1, var9, var10);
         }

         if (var10.isAir()) {
            Holder<PlacedFeature> var11;
            if (param1.nextInt(8) == 0) {
               List<ConfiguredFeature<?, ?>> var12 = param0.getBiome(var9).value().getGenerationSettings().getFlowerFeatures();
               if (var12.isEmpty()) {
                  continue;
               }

               var11 = ((RandomPatchConfiguration)var12.get(0).config()).feature();
            } else {
               if (!var7.isPresent()) {
                  continue;
               }

               var11 = var7.get();
            }

            var11.value().place(param0, param0.getChunkSource().getGenerator(), param1, var9);
         }
      }
   }
}

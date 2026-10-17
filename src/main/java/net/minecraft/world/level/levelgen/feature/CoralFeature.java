package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public abstract class CoralFeature extends Feature<NoneFeatureConfiguration> {
   public CoralFeature(Codec<NoneFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> param0) {
      RandomSource var2 = param0.random();
      WorldGenLevel var3 = param0.level();
      BlockPos var4 = param0.origin();
      Optional<Block> var5 = BuiltInRegistries.BLOCK.getTag(BlockTags.CORAL_BLOCKS).flatMap(l_param1 -> l_param1.getRandomElement(var2)).map(Holder::value);
      return var5.isEmpty() ? false : this.placeFeature(var3, var2, var4, var5.get().defaultBlockState());
   }

   protected abstract boolean placeFeature(LevelAccessor var1, RandomSource var2, BlockPos var3, BlockState var4);

   protected boolean placeCoralBlock(LevelAccessor param0, RandomSource param1, BlockPos param2, BlockState param3) {
      BlockPos var5 = param2.above();
      BlockState var6 = param0.getBlockState(param2);
      if ((var6.is(Blocks.WATER) || var6.is(BlockTags.CORALS)) && param0.getBlockState(var5).is(Blocks.WATER)) {
         param0.setBlock(param2, param3, 3);
         if (param1.nextFloat() < 0.25F) {
            BuiltInRegistries.BLOCK
               .getTag(BlockTags.CORALS)
               .flatMap(l_param1 -> l_param1.getRandomElement(param1))
               .map(Holder::value)
               .ifPresent(l_param2 -> param0.setBlock(var5, l_param2.defaultBlockState(), 2));
         } else if (param1.nextFloat() < 0.05F) {
            param0.setBlock(var5, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, param1.nextInt(4) + 1), 2);
         }

         for (Direction var8 : Direction.Plane.HORIZONTAL) {
            if (param1.nextFloat() < 0.2F) {
               BlockPos var9 = param2.relative(var8);
               if (param0.getBlockState(var9).is(Blocks.WATER)) {
                  BuiltInRegistries.BLOCK
                     .getTag(BlockTags.WALL_CORALS)
                     .flatMap(l_param1 -> l_param1.getRandomElement(param1))
                     .map(Holder::value)
                     .ifPresent(l_param3 -> {
                        BlockState l_var4 = l_param3.defaultBlockState();
                        if (l_var4.hasProperty(BaseCoralWallFanBlock.FACING)) {
                           l_var4 = l_var4.setValue(BaseCoralWallFanBlock.FACING, var8);
                        }

                        param0.setBlock(var9, l_var4, 2);
                     });
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }
}

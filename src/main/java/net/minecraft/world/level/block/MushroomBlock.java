package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MushroomBlock extends BushBlock implements BonemealableBlock {
   protected static final float AABB_OFFSET = 3.0F;
   protected static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0);
   private final ResourceKey<ConfiguredFeature<?, ?>> feature;

   public MushroomBlock(BlockBehaviour.Properties param0, ResourceKey<ConfiguredFeature<?, ?>> param1) {
      super(param0);
      this.feature = param1;
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE;
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param3.nextInt(25) == 0) {
         int var5 = 5;
         int var6 = 4;

         for (BlockPos var8 : BlockPos.betweenClosed(param2.offset(-4, -1, -4), param2.offset(4, 1, 4))) {
            if (param1.getBlockState(var8).is(this)) {
               if (--var5 <= 0) {
                  return;
               }
            }
         }

         BlockPos var7 = param2.offset(param3.nextInt(3) - 1, param3.nextInt(2) - param3.nextInt(2), param3.nextInt(3) - 1);

         for (int var8 = 0; var8 < 4; var8++) {
            if (param1.isEmptyBlock(var7) && param0.canSurvive(param1, var7)) {
               param2 = var7;
            }

            var7 = param2.offset(param3.nextInt(3) - 1, param3.nextInt(2) - param3.nextInt(2), param3.nextInt(3) - 1);
         }

         if (param1.isEmptyBlock(var7) && param0.canSurvive(param1, var7)) {
            param1.setBlock(var7, param0, 2);
         }
      }
   }

   @Override
   protected boolean mayPlaceOn(BlockState param0, BlockGetter param1, BlockPos param2) {
      return param0.isSolidRender(param1, param2);
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      BlockPos var4 = param2.below();
      BlockState var5 = param1.getBlockState(var4);
      return var5.is(BlockTags.MUSHROOM_GROW_BLOCK) ? true : param1.getRawBrightness(param2, 0) < 13 && this.mayPlaceOn(var5, param1, var4);
   }

   public boolean growMushroom(ServerLevel param0, BlockPos param1, BlockState param2, RandomSource param3) {
      Optional<? extends Holder<ConfiguredFeature<?, ?>>> var5 = param0.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(this.feature);
      if (var5.isEmpty()) {
         return false;
      }

      param0.removeBlock(param1, false);
      if (var5.get().value().place(param0, param0.getChunkSource().getGenerator(), param3, param1)) {
         return true;
      }

      param0.setBlock(param1, param2, 3);
      return false;
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader param0, BlockPos param1, BlockState param2, boolean param3) {
      return true;
   }

   @Override
   public boolean isBonemealSuccess(Level param0, RandomSource param1, BlockPos param2, BlockState param3) {
      return param1.nextFloat() < 0.4;
   }

   @Override
   public void performBonemeal(ServerLevel param0, RandomSource param1, BlockPos param2, BlockState param3) {
      this.growMushroom(param0, param2, param3, param1);
   }
}

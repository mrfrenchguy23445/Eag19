package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CactusBlock extends Block {
   public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
   public static final int MAX_AGE = 15;
   protected static final int AABB_OFFSET = 1;
   protected static final VoxelShape COLLISION_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);
   protected static final VoxelShape OUTLINE_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

   protected CactusBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (!param0.canSurvive(param1, param2)) {
         param1.destroyBlock(param2, true);
      }
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      BlockPos var5 = param2.above();
      if (param1.isEmptyBlock(var5)) {
         int var6 = 1;

         while (param1.getBlockState(param2.below(var6)).is(this)) {
            var6++;
         }

         if (var6 < 3) {
            int var7 = param0.getValue(AGE);
            if (var7 == 15) {
               param1.setBlockAndUpdate(var5, this.defaultBlockState());
               BlockState var8 = param0.setValue(AGE, 0);
               param1.setBlock(param2, var8, 4);
               param1.neighborChanged(var8, var5, this, param2, false);
            } else {
               param1.setBlock(param2, param0.setValue(AGE, var7 + 1), 4);
            }
         }
      }
   }

   @Override
   public VoxelShape getCollisionShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return COLLISION_SHAPE;
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return OUTLINE_SHAPE;
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (!param0.canSurvive(param3, param4)) {
         param3.scheduleTick(param4, this, 1);
      }

      return super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      for (Direction var5 : Direction.Plane.HORIZONTAL) {
         BlockState var6 = param1.getBlockState(param2.relative(var5));
         Material var7 = var6.getMaterial();
         if (var7.isSolid() || param1.getFluidState(param2.relative(var5)).is(FluidTags.LAVA)) {
            return false;
         }
      }

      BlockState var4 = param1.getBlockState(param2.below());
      return (var4.is(Blocks.CACTUS) || var4.is(Blocks.SAND) || var4.is(Blocks.RED_SAND)) && !param1.getBlockState(param2.above()).getMaterial().isLiquid();
   }

   @Override
   public void entityInside(BlockState param0, Level param1, BlockPos param2, Entity param3) {
      param3.hurt(param1.damageSources().cactus(), 1.0F);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(AGE);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }
}

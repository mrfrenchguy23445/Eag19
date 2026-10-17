package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LeavesBlock extends Block implements SimpleWaterloggedBlock {
   public static final int DECAY_DISTANCE = 7;
   public static final IntegerProperty DISTANCE = BlockStateProperties.DISTANCE;
   public static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final int TICK_DELAY = 1;

   public LeavesBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(DISTANCE, 7).setValue(PERSISTENT, false).setValue(WATERLOGGED, false));
   }

   @Override
   public VoxelShape getBlockSupportShape(BlockState param0, BlockGetter param1, BlockPos param2) {
      return Shapes.empty();
   }

   @Override
   public boolean isRandomlyTicking(BlockState param0) {
      return param0.getValue(DISTANCE) == 7 && !param0.getValue(PERSISTENT);
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (this.decaying(param0)) {
         dropResources(param0, param1, param2);
         param1.removeBlock(param2, false);
      }
   }

   protected boolean decaying(BlockState param0) {
      return !param0.getValue(PERSISTENT) && param0.getValue(DISTANCE) == 7;
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      param1.setBlock(param2, updateDistance(param0, param1, param2), 3);
   }

   @Override
   public int getLightBlock(BlockState param0, BlockGetter param1, BlockPos param2) {
      return 1;
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param0.getValue(WATERLOGGED)) {
         param3.scheduleTick(param4, Fluids.WATER, Fluids.WATER.getTickDelay(param3));
      }

      int var7 = getDistanceAt(param2) + 1;
      if (var7 != 1 || param0.getValue(DISTANCE) != var7) {
         param3.scheduleTick(param4, this, 1);
      }

      return param0;
   }

   private static BlockState updateDistance(BlockState param0, LevelAccessor param1, BlockPos param2) {
      int var3 = 7;
      BlockPos.MutableBlockPos var4 = new BlockPos.MutableBlockPos();

      for (Direction var8 : Direction.values()) {
         var4.setWithOffset(param2, var8);
         var3 = Math.min(var3, getDistanceAt(param1.getBlockState(var4)) + 1);
         if (var3 == 1) {
            break;
         }
      }

      return param0.setValue(DISTANCE, var3);
   }

   private static int getDistanceAt(BlockState param0) {
      if (param0.is(BlockTags.LOGS)) {
         return 0;
      } else {
         return param0.getBlock() instanceof LeavesBlock ? param0.getValue(DISTANCE) : 7;
      }
   }

   @Override
   public FluidState getFluidState(BlockState param0) {
      return param0.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(param0);
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      if (param1.isRainingAt(param2.above())) {
         if (param3.nextInt(15) == 1) {
            BlockPos var5 = param2.below();
            BlockState var6 = param1.getBlockState(var5);
            if (!var6.canOcclude() || !var6.isFaceSturdy(param1, var5, Direction.UP)) {
               ParticleUtils.spawnParticleBelow(param1, param2, param3, ParticleTypes.DRIPPING_WATER);
            }
         }
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(DISTANCE, PERSISTENT, WATERLOGGED);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      FluidState var2 = param0.getLevel().getFluidState(param0.getClickedPos());
      BlockState var3 = this.defaultBlockState().setValue(PERSISTENT, true).setValue(WATERLOGGED, var2.getType() == Fluids.WATER);
      return updateDistance(var3, param0.getLevel(), param0.getClickedPos());
   }
}

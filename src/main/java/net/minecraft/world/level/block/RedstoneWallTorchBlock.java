package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RedstoneWallTorchBlock extends RedstoneTorchBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

   protected RedstoneWallTorchBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, true));
   }

   @Override
   public String getDescriptionId() {
      return this.asItem().getDescriptionId();
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return WallTorchBlock.getShape(param0);
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      return Blocks.WALL_TORCH.canSurvive(param0, param1, param2);
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      return Blocks.WALL_TORCH.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      BlockState var2 = Blocks.WALL_TORCH.getStateForPlacement(param0);
      return var2 == null ? null : this.defaultBlockState().setValue(FACING, var2.getValue(FACING));
   }

   @Override
   public void animateTick(BlockState param0, Level param1, BlockPos param2, RandomSource param3) {
      if (param0.getValue(LIT)) {
         Direction var5 = param0.getValue(FACING).getOpposite();
         double var6 = 0.27;
         double var8 = param2.getX() + 0.5 + (param3.nextDouble() - 0.5) * 0.2 + 0.27 * var5.getStepX();
         double var10 = param2.getY() + 0.7 + (param3.nextDouble() - 0.5) * 0.2 + 0.22;
         double var12 = param2.getZ() + 0.5 + (param3.nextDouble() - 0.5) * 0.2 + 0.27 * var5.getStepZ();
         param1.addParticle(this.flameParticle, var8, var10, var12, 0.0, 0.0, 0.0);
      }
   }

   @Override
   protected boolean hasNeighborSignal(Level param0, BlockPos param1, BlockState param2) {
      Direction var4 = param2.getValue(FACING).getOpposite();
      return param0.hasSignal(param1.relative(var4), var4);
   }

   @Override
   public int getSignal(BlockState param0, BlockGetter param1, BlockPos param2, Direction param3) {
      return param0.getValue(LIT) && param0.getValue(FACING) != param3 ? 15 : 0;
   }

   @Override
   public BlockState rotate(BlockState param0, Rotation param1) {
      return Blocks.WALL_TORCH.rotate(param0, param1);
   }

   @Override
   public BlockState mirror(BlockState param0, Mirror param1) {
      return Blocks.WALL_TORCH.mirror(param0, param1);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(FACING, LIT);
   }
}

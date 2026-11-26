package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class IronBarsBlock extends CrossCollisionBlock {
   protected IronBarsBlock(BlockBehaviour.Properties param0) {
      super(1.0F, 1.0F, 16.0F, 16.0F, 16.0F, param0);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false)
      );
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      BlockGetter var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      FluidState var4 = param0.getLevel().getFluidState(param0.getClickedPos());
      BlockPos var5 = var3.north();
      BlockPos var6 = var3.south();
      BlockPos var7 = var3.west();
      BlockPos var8 = var3.east();
      BlockState var9 = var2.getBlockState(var5);
      BlockState var10 = var2.getBlockState(var6);
      BlockState var11 = var2.getBlockState(var7);
      BlockState var12 = var2.getBlockState(var8);
      return this.defaultBlockState()
         .setValue(NORTH, this.attachsTo(var9, var9.isFaceSturdy(var2, var5, Direction.SOUTH)))
         .setValue(SOUTH, this.attachsTo(var10, var10.isFaceSturdy(var2, var6, Direction.NORTH)))
         .setValue(WEST, this.attachsTo(var11, var11.isFaceSturdy(var2, var7, Direction.EAST)))
         .setValue(EAST, this.attachsTo(var12, var12.isFaceSturdy(var2, var8, Direction.WEST)))
         .setValue(WATERLOGGED, var4.getType() == Fluids.WATER);
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param0.getValue(WATERLOGGED)) {
         param3.scheduleTick(param4, Fluids.WATER, Fluids.WATER.getTickDelay(param3));
      }

      return param1.getAxis().isHorizontal()
         ? param0.setValue(PROPERTY_BY_DIRECTION.get(param1), this.attachsTo(param2, param2.isFaceSturdy(param3, param5, param1.getOpposite())))
         : super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public VoxelShape getVisualShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return Shapes.empty();
   }

   @Override
   public boolean skipRendering(BlockState param0, BlockState param1, Direction param2) {
      if (param1.is(this)) {
         if (!param2.getAxis().isHorizontal()) {
            return true;
         }

         if (param0.getValue(PROPERTY_BY_DIRECTION.get(param2)) && param1.getValue(PROPERTY_BY_DIRECTION.get(param2.getOpposite()))) {
            return true;
         }
      }

      return super.skipRendering(param0, param1, param2);
   }

   public final boolean attachsTo(BlockState param0, boolean param1) {
      return !isExceptionForConnection(param0) && param1 || param0.getBlock() instanceof IronBarsBlock || param0.is(BlockTags.WALLS);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }
}

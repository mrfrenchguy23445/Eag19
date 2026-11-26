package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FenceBlock extends CrossCollisionBlock {
   private final VoxelShape[] occlusionByIndex;

   public FenceBlock(BlockBehaviour.Properties param0) {
      super(2.0F, 2.0F, 16.0F, 16.0F, 24.0F, param0);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false)
      );
      this.occlusionByIndex = this.makeShapes(2.0F, 1.0F, 16.0F, 6.0F, 15.0F);
   }

   @Override
   public VoxelShape getOcclusionShape(BlockState param0, BlockGetter param1, BlockPos param2) {
      return this.occlusionByIndex[this.getAABBIndex(param0)];
   }

   @Override
   public VoxelShape getVisualShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return this.getShape(param0, param1, param2, param3);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }

   public boolean connectsTo(BlockState param0, boolean param1, Direction param2) {
      Block var4 = param0.getBlock();
      boolean var5 = this.isSameFence(param0);
      boolean var6 = var4 instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(param0, param2);
      return !isExceptionForConnection(param0) && param1 || var5 || var6;
   }

   private boolean isSameFence(BlockState param0) {
      return param0.is(BlockTags.FENCES) && param0.is(BlockTags.WOODEN_FENCES) == this.defaultBlockState().is(BlockTags.WOODEN_FENCES);
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      if (param1.isClientSide) {
         ItemStack var7 = param3.getItemInHand(param4);
         return var7.is(Items.LEAD) ? InteractionResult.SUCCESS : InteractionResult.PASS;
      } else {
         return LeadItem.bindPlayerMobs(param3, param1, param2);
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      BlockGetter var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      FluidState var4 = param0.getLevel().getFluidState(param0.getClickedPos());
      BlockPos var5 = var3.north();
      BlockPos var6 = var3.east();
      BlockPos var7 = var3.south();
      BlockPos var8 = var3.west();
      BlockState var9 = var2.getBlockState(var5);
      BlockState var10 = var2.getBlockState(var6);
      BlockState var11 = var2.getBlockState(var7);
      BlockState var12 = var2.getBlockState(var8);
      return super.getStateForPlacement(param0)
         .setValue(NORTH, this.connectsTo(var9, var9.isFaceSturdy(var2, var5, Direction.SOUTH), Direction.SOUTH))
         .setValue(EAST, this.connectsTo(var10, var10.isFaceSturdy(var2, var6, Direction.WEST), Direction.WEST))
         .setValue(SOUTH, this.connectsTo(var11, var11.isFaceSturdy(var2, var7, Direction.NORTH), Direction.NORTH))
         .setValue(WEST, this.connectsTo(var12, var12.isFaceSturdy(var2, var8, Direction.EAST), Direction.EAST))
         .setValue(WATERLOGGED, var4.getType() == Fluids.WATER);
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param0.getValue(WATERLOGGED)) {
         param3.scheduleTick(param4, Fluids.WATER, Fluids.WATER.getTickDelay(param3));
      }

      return param1.getAxis().getPlane() == Direction.Plane.HORIZONTAL
         ? param0.setValue(
            PROPERTY_BY_DIRECTION.get(param1), this.connectsTo(param2, param2.isFaceSturdy(param3, param5, param1.getOpposite()), param1.getOpposite())
         )
         : super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }
}

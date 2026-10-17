package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ConcretePowderBlock extends FallingBlock {
   private final BlockState concrete;

   public ConcretePowderBlock(Block param0, BlockBehaviour.Properties param1) {
      super(param1);
      this.concrete = param0.defaultBlockState();
   }

   @Override
   public void onLand(Level param0, BlockPos param1, BlockState param2, BlockState param3, FallingBlockEntity param4) {
      if (shouldSolidify(param0, param1, param3)) {
         param0.setBlock(param1, this.concrete, 3);
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      BlockGetter var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      BlockState var4 = var2.getBlockState(var3);
      return shouldSolidify(var2, var3, var4) ? this.concrete : super.getStateForPlacement(param0);
   }

   private static boolean shouldSolidify(BlockGetter param0, BlockPos param1, BlockState param2) {
      return canSolidify(param2) || touchesLiquid(param0, param1);
   }

   private static boolean touchesLiquid(BlockGetter param0, BlockPos param1) {
      boolean var2 = false;
      BlockPos.MutableBlockPos var3 = param1.mutable();

      for (Direction var7 : Direction.values()) {
         BlockState var8 = param0.getBlockState(var3);
         if (var7 != Direction.DOWN || canSolidify(var8)) {
            var3.setWithOffset(param1, var7);
            var8 = param0.getBlockState(var3);
            if (canSolidify(var8) && !var8.isFaceSturdy(param0, param1, var7.getOpposite())) {
               var2 = true;
               break;
            }
         }
      }

      return var2;
   }

   private static boolean canSolidify(BlockState param0) {
      return param0.getFluidState().is(FluidTags.WATER);
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      return touchesLiquid(param3, param4) ? this.concrete : super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public int getDustColor(BlockState param0, BlockGetter param1, BlockPos param2) {
      return param0.getMapColor(param1, param2).col;
   }
}

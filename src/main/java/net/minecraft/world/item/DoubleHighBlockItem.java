package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DoubleHighBlockItem extends BlockItem {
   public DoubleHighBlockItem(Block param0, Item.Properties param1) {
      super(param0, param1);
   }

   @Override
   protected boolean placeBlock(BlockPlaceContext param0, BlockState param1) {
      Level var3 = param0.getLevel();
      BlockPos var4 = param0.getClickedPos().above();
      BlockState var5 = var3.isWaterAt(var4) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
      var3.setBlock(var4, var5, 27);
      return super.placeBlock(param0, param1);
   }
}

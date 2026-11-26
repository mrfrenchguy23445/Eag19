package net.minecraft.world.item;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public class StandingAndWallBlockItem extends BlockItem {
   protected final Block wallBlock;
   private final Direction attachmentDirection;

   public StandingAndWallBlockItem(Block param0, Block param1, Item.Properties param2, Direction param3) {
      super(param0, param2);
      this.wallBlock = param1;
      this.attachmentDirection = param3;
   }

   protected boolean canPlace(LevelReader param0, BlockState param1, BlockPos param2) {
      return param1.canSurvive(param0, param2);
   }

   @Nullable
   @Override
   protected BlockState getPlacementState(BlockPlaceContext param0) {
      BlockState var2 = this.wallBlock.getStateForPlacement(param0);
      BlockState var3 = null;
      LevelReader var4 = param0.getLevel();
      BlockPos var5 = param0.getClickedPos();

      for (Direction var9 : param0.getNearestLookingDirections()) {
         if (var9 != this.attachmentDirection.getOpposite()) {
            BlockState var10 = var9 == this.attachmentDirection ? this.getBlock().getStateForPlacement(param0) : var2;
            if (var10 != null && this.canPlace(var4, var10, var5)) {
               var3 = var10;
               break;
            }
         }
      }

      return var3 != null && var4.isUnobstructed(var3, var5, CollisionContext.empty()) ? var3 : null;
   }

   @Override
   public void registerBlocks(Map<Block, Item> param0, Item param1) {
      super.registerBlocks(param0, param1);
      param0.put(this.wallBlock, param1);
   }
}

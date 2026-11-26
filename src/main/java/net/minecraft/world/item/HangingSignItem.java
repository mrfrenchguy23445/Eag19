package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class HangingSignItem extends StandingAndWallBlockItem {
   public HangingSignItem(Block param0, Block param1, Item.Properties param2) {
      super(param0, param1, param2, Direction.UP);
   }

   @Override
   protected boolean canPlace(LevelReader param0, BlockState param1, BlockPos param2) {
      return param1.getBlock() instanceof WallHangingSignBlock var4 && !var4.canPlace(param1, param0, param2) ? false : super.canPlace(param0, param1, param2);
   }

   @Override
   protected boolean updateCustomBlockEntityTag(BlockPos param0, Level param1, @Nullable Player param2, ItemStack param3, BlockState param4) {
      boolean var6 = super.updateCustomBlockEntityTag(param0, param1, param2, param3, param4);
      if (!param1.isClientSide && !var6 && param2 != null && param1.getBlockEntity(param0) instanceof SignBlockEntity var7) {
         param2.openTextEdit(var7);
      }

      return var6;
   }
}

package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ScaffoldingBlockItem extends BlockItem {
   public ScaffoldingBlockItem(Block param0, Item.Properties param1) {
      super(param0, param1);
   }

   @Nullable
   @Override
   public BlockPlaceContext updatePlacementContext(BlockPlaceContext param0) {
      BlockPos var2 = param0.getClickedPos();
      Level var3 = param0.getLevel();
      BlockState var4 = var3.getBlockState(var2);
      Block var5 = this.getBlock();
      if (!var4.is(var5)) {
         return ScaffoldingBlock.getDistance(var3, var2) == 7 ? null : param0;
      }

      Direction var6;
      if (param0.isSecondaryUseActive()) {
         var6 = param0.isInside() ? param0.getClickedFace().getOpposite() : param0.getClickedFace();
      } else {
         var6 = param0.getClickedFace() == Direction.UP ? param0.getHorizontalDirection() : Direction.UP;
      }

      int var7 = 0;
      BlockPos.MutableBlockPos var8 = var2.mutable().move(var6);

      while (var7 < 7) {
         if (!var3.isClientSide && !var3.isInWorldBounds(var8)) {
            Player var9 = param0.getPlayer();
            int var10 = var3.getMaxBuildHeight();
            if (var9 instanceof ServerPlayer && var8.getY() >= var10) {
               ((ServerPlayer)var9).sendSystemMessage(Component.translatable("build.tooHigh", var10 - 1).withStyle(ChatFormatting.RED), true);
            }
            break;
         }

         var4 = var3.getBlockState(var8);
         if (!var4.is(this.getBlock())) {
            if (var4.canBeReplaced(param0)) {
               return BlockPlaceContext.at(param0, var8, var6);
            }
            break;
         }

         var8.move(var6);
         if (var6.getAxis().isHorizontal()) {
            var7++;
         }
      }

      return null;
   }

   @Override
   protected boolean mustSurvive() {
      return false;
   }
}

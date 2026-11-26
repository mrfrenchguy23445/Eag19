package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

public class FireChargeItem extends Item {
   public FireChargeItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      BlockState var4 = var2.getBlockState(var3);
      boolean var5 = false;
      if (!CampfireBlock.canLight(var4) && !CandleBlock.canLight(var4) && !CandleCakeBlock.canLight(var4)) {
         var3 = var3.relative(param0.getClickedFace());
         if (BaseFireBlock.canBePlacedAt(var2, var3, param0.getHorizontalDirection())) {
            this.playSound(var2, var3);
            var2.setBlockAndUpdate(var3, BaseFireBlock.getState(var2, var3));
            var2.gameEvent(param0.getPlayer(), GameEvent.BLOCK_PLACE, var3);
            var5 = true;
         }
      } else {
         this.playSound(var2, var3);
         var2.setBlockAndUpdate(var3, var4.setValue(BlockStateProperties.LIT, true));
         var2.gameEvent(param0.getPlayer(), GameEvent.BLOCK_CHANGE, var3);
         var5 = true;
      }

      if (var5) {
         param0.getItemInHand().shrink(1);
         return InteractionResult.sidedSuccess(var2.isClientSide);
      } else {
         return InteractionResult.FAIL;
      }
   }

   private void playSound(Level param0, BlockPos param1) {
      RandomSource var3 = param0.getRandom();
      param0.playSound((Player)null, param1, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, (var3.nextFloat() - var3.nextFloat()) * 0.2F + 1.0F);
   }
}

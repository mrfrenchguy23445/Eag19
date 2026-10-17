package net.minecraft.world.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class ShearsItem extends Item {
   public ShearsItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public boolean mineBlock(ItemStack param0, Level param1, BlockState param2, BlockPos param3, LivingEntity param4) {
      if (!param1.isClientSide && !param2.is(BlockTags.FIRE)) {
         param0.hurtAndBreak(1, param4, l_param0 -> l_param0.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      }

      return !param2.is(BlockTags.LEAVES)
            && !param2.is(Blocks.COBWEB)
            && !param2.is(Blocks.GRASS)
            && !param2.is(Blocks.FERN)
            && !param2.is(Blocks.DEAD_BUSH)
            && !param2.is(Blocks.HANGING_ROOTS)
            && !param2.is(Blocks.VINE)
            && !param2.is(Blocks.TRIPWIRE)
            && !param2.is(BlockTags.WOOL)
         ? super.mineBlock(param0, param1, param2, param3, param4)
         : true;
   }

   @Override
   public boolean isCorrectToolForDrops(BlockState param0) {
      return param0.is(Blocks.COBWEB) || param0.is(Blocks.REDSTONE_WIRE) || param0.is(Blocks.TRIPWIRE);
   }

   @Override
   public float getDestroySpeed(ItemStack param0, BlockState param1) {
      if (param1.is(Blocks.COBWEB) || param1.is(BlockTags.LEAVES)) {
         return 15.0F;
      } else if (param1.is(BlockTags.WOOL)) {
         return 5.0F;
      } else {
         return !param1.is(Blocks.VINE) && !param1.is(Blocks.GLOW_LICHEN) ? super.getDestroySpeed(param0, param1) : 2.0F;
      }
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      BlockState var4 = var2.getBlockState(var3);
      if (var4.getBlock() instanceof GrowingPlantHeadBlock var6 && !var6.isMaxAge(var4)) {
         Player var7 = param0.getPlayer();
         ItemStack var8 = param0.getItemInHand();
         if (var7 instanceof ServerPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)var7, var3, var8);
         }

         var2.playSound(var7, var3, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
         BlockState var9 = var6.getMaxAgeState(var4);
         var2.setBlockAndUpdate(var3, var9);
         var2.gameEvent(GameEvent.BLOCK_CHANGE, var3, GameEvent.Context.of(param0.getPlayer(), var9));
         if (var7 != null) {
            var8.hurtAndBreak(1, var7, l_param1 -> l_param1.broadcastBreakEvent(param0.getHand()));
         }

         return InteractionResult.sidedSuccess(var2.isClientSide);
      } else {
         return super.useOn(param0);
      }
   }
}

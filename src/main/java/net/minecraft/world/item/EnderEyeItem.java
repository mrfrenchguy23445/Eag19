package net.minecraft.world.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class EnderEyeItem extends Item {
   public EnderEyeItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      BlockState var4 = var2.getBlockState(var3);
      if (!var4.is(Blocks.END_PORTAL_FRAME) || var4.getValue(EndPortalFrameBlock.HAS_EYE)) {
         return InteractionResult.PASS;
      }

      if (var2.isClientSide) {
         return InteractionResult.SUCCESS;
      }

      BlockState var5 = var4.setValue(EndPortalFrameBlock.HAS_EYE, true);
      Block.pushEntitiesUp(var4, var5, var2, var3);
      var2.setBlock(var3, var5, 2);
      var2.updateNeighbourForOutputSignal(var3, Blocks.END_PORTAL_FRAME);
      param0.getItemInHand().shrink(1);
      var2.levelEvent(1503, var3, 0);
      BlockPattern.BlockPatternMatch var6 = EndPortalFrameBlock.getOrCreatePortalShape().find(var2, var3);
      if (var6 != null) {
         BlockPos var7 = var6.getFrontTopLeft().offset(-3, 0, -3);

         for (int var8 = 0; var8 < 3; var8++) {
            for (int var9 = 0; var9 < 3; var9++) {
               var2.setBlock(var7.offset(var8, 0, var9), Blocks.END_PORTAL.defaultBlockState(), 2);
            }
         }

         var2.globalLevelEvent(1038, var7.offset(1, 0, 1), 0);
      }

      return InteractionResult.CONSUME;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      ItemStack var4 = param1.getItemInHand(param2);
      HitResult var5 = getPlayerPOVHitResult(param0, param1, ClipContext.Fluid.NONE);
      if (var5.getType() == HitResult.Type.BLOCK && param0.getBlockState(((BlockHitResult)var5).getBlockPos()).is(Blocks.END_PORTAL_FRAME)) {
         return InteractionResultHolder.pass(var4);
      }

      param1.startUsingItem(param2);
      if (param0 instanceof ServerLevel var6) {
         BlockPos var7 = var6.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, param1.blockPosition(), 100, false);
         if (var7 != null) {
            EyeOfEnder var8 = new EyeOfEnder(param0, param1.getX(), param1.getY(0.5), param1.getZ());
            var8.setItem(var4);
            var8.signalTo(var7);
            param0.gameEvent(GameEvent.PROJECTILE_SHOOT, var8.position(), GameEvent.Context.of(param1));
            param0.addFreshEntity(var8);
            if (param1 instanceof ServerPlayer) {
               CriteriaTriggers.USED_ENDER_EYE.trigger((ServerPlayer)param1, var7);
            }

            param0.playSound(
               null,
               param1.getX(),
               param1.getY(),
               param1.getZ(),
               SoundEvents.ENDER_EYE_LAUNCH,
               SoundSource.NEUTRAL,
               0.5F,
               0.4F / (param0.getRandom().nextFloat() * 0.4F + 0.8F)
            );
            param0.levelEvent(null, 1003, param1.blockPosition(), 0);
            if (!param1.getAbilities().instabuild) {
               var4.shrink(1);
            }

            param1.awardStat(Stats.ITEM_USED.get(this));
            param1.swing(param2, true);
            return InteractionResultHolder.success(var4);
         }
      }

      return InteractionResultHolder.consume(var4);
   }
}

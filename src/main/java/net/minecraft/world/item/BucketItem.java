package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BucketItem extends Item implements DispensibleContainerItem {
   private final Fluid content;

   public BucketItem(Fluid param0, Item.Properties param1) {
      super(param1);
      this.content = param0;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      ItemStack var4 = param1.getItemInHand(param2);
      BlockHitResult var5 = getPlayerPOVHitResult(param0, param1, this.content == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
      if (var5.getType() == HitResult.Type.MISS) {
         return InteractionResultHolder.pass(var4);
      }

      if (var5.getType() != HitResult.Type.BLOCK) {
         return InteractionResultHolder.pass(var4);
      }

      BlockPos var6 = var5.getBlockPos();
      Direction var7 = var5.getDirection();
      BlockPos var8 = var6.relative(var7);
      if (!param0.mayInteract(param1, var6) || !param1.mayUseItemAt(var8, var7, var4)) {
         return InteractionResultHolder.fail(var4);
      }

      if (this.content == Fluids.EMPTY) {
         BlockState var9 = param0.getBlockState(var6);
         if (var9.getBlock() instanceof BucketPickup) {
            BucketPickup var10 = (BucketPickup)var9.getBlock();
            ItemStack var11 = var10.pickupBlock(param0, var6, var9);
            if (!var11.isEmpty()) {
               param1.awardStat(Stats.ITEM_USED.get(this));
               var10.getPickupSound().ifPresent(l_param1 -> param1.playSound(l_param1, 1.0F, 1.0F));
               param0.gameEvent(param1, GameEvent.FLUID_PICKUP, var6);
               ItemStack var12 = ItemUtils.createFilledResult(var4, param1, var11);
               if (!param0.isClientSide) {
                  CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)param1, var11);
               }

               return InteractionResultHolder.sidedSuccess(var12, param0.isClientSide());
            }
         }

         return InteractionResultHolder.fail(var4);
      } else {
         BlockState var9 = param0.getBlockState(var6);
         BlockPos var10 = var9.getBlock() instanceof LiquidBlockContainer && this.content == Fluids.WATER ? var6 : var8;
         if (this.emptyContents(param1, param0, var10, var5)) {
            this.checkExtraContent(param1, param0, var4, var10);
            if (param1 instanceof ServerPlayer) {
               CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)param1, var10, var4);
            }

            param1.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.sidedSuccess(getEmptySuccessItem(var4, param1), param0.isClientSide());
         } else {
            return InteractionResultHolder.fail(var4);
         }
      }
   }

   public static ItemStack getEmptySuccessItem(ItemStack param0, Player param1) {
      return !param1.getAbilities().instabuild ? new ItemStack(Items.BUCKET) : param0;
   }

   @Override
   public void checkExtraContent(@Nullable Player param0, Level param1, ItemStack param2, BlockPos param3) {
   }

   @Override
   public boolean emptyContents(@Nullable Player param0, Level param1, BlockPos param2, @Nullable BlockHitResult param3) {
      if (!(this.content instanceof FlowingFluid)) {
         return false;
      }

      BlockState var5 = param1.getBlockState(param2);
      Block var6 = var5.getBlock();
      Material var7 = var5.getMaterial();
      boolean var8 = var5.canBeReplaced(this.content);
      boolean var9 = var5.isAir()
         || var8
         || var6 instanceof LiquidBlockContainer && ((LiquidBlockContainer)var6).canPlaceLiquid(param1, param2, var5, this.content);
      if (!var9) {
         return param3 != null && this.emptyContents(param0, param1, param3.getBlockPos().relative(param3.getDirection()), null);
      }

      if (param1.dimensionType().ultraWarm() && this.content.is(FluidTags.WATER)) {
         int var10 = param2.getX();
         int var11 = param2.getY();
         int var12 = param2.getZ();
         param1.playSound(
            param0, param2, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (param1.random.nextFloat() - param1.random.nextFloat()) * 0.8F
         );

         for (int var13 = 0; var13 < 8; var13++) {
            param1.addParticle(ParticleTypes.LARGE_SMOKE, var10 + Math.random(), var11 + Math.random(), var12 + Math.random(), 0.0, 0.0, 0.0);
         }

         return true;
      } else {
         if (var6 instanceof LiquidBlockContainer && this.content == Fluids.WATER) {
            ((LiquidBlockContainer)var6).placeLiquid(param1, param2, var5, ((FlowingFluid)this.content).getSource(false));
            this.playEmptySound(param0, param1, param2);
            return true;
         }

         if (!param1.isClientSide && var8 && !var7.isLiquid()) {
            param1.destroyBlock(param2, true);
         }

         if (!param1.setBlock(param2, this.content.defaultFluidState().createLegacyBlock(), 11) && !var5.getFluidState().isSource()) {
            return false;
         }

         this.playEmptySound(param0, param1, param2);
         return true;
      }
   }

   protected void playEmptySound(@Nullable Player param0, LevelAccessor param1, BlockPos param2) {
      SoundEvent var4 = this.content.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
      param1.playSound(param0, param2, var4, SoundSource.BLOCKS, 1.0F, 1.0F);
      param1.gameEvent(param0, GameEvent.FLUID_PLACE, param2);
   }
}

package net.minecraft.world.level.block;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class SignBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   protected static final float AABB_OFFSET = 4.0F;
   protected static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
   private final WoodType type;

   protected SignBlock(BlockBehaviour.Properties param0, WoodType param1) {
      super(param0);
      this.type = param1;
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param0.getValue(WATERLOGGED)) {
         param3.scheduleTick(param4, Fluids.WATER, Fluids.WATER.getTickDelay(param3));
      }

      return super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE;
   }

   @Override
   public boolean isPossibleToRespawnInThis() {
      return true;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new SignBlockEntity(param0, param1);
   }

   @Override
   public InteractionResult use(BlockState param0, Level param1, BlockPos param2, Player param3, InteractionHand param4, BlockHitResult param5) {
      ItemStack var7 = param3.getItemInHand(param4);
      Item var8 = var7.getItem();
      boolean var9 = var8 instanceof DyeItem;
      boolean var10 = var7.is(Items.GLOW_INK_SAC);
      boolean var11 = var7.is(Items.INK_SAC);
      boolean var12 = (var10 || var9 || var11) && param3.getAbilities().mayBuild;
      if (param1.isClientSide) {
         return var12 ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
      } else if (!(param1.getBlockEntity(param2) instanceof SignBlockEntity var13)) {
         return InteractionResult.PASS;
      } else {
         boolean var14 = var13.hasGlowingText();
         if ((!var10 || !var14) && (!var11 || var14)) {
            if (var12) {
               boolean var15;
               if (var10) {
                  param1.playSound((Player)null, param2, SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                  var15 = var13.setHasGlowingText(true);
                  if (param3 instanceof ServerPlayer) {
                     CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)param3, param2, var7);
                  }
               } else if (var11) {
                  param1.playSound((Player)null, param2, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                  var15 = var13.setHasGlowingText(false);
               } else {
                  param1.playSound((Player)null, param2, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                  var15 = var13.setColor(((DyeItem)var8).getDyeColor());
               }

               if (var15) {
                  if (!param3.isCreative()) {
                     var7.shrink(1);
                  }

                  param3.awardStat(Stats.ITEM_USED.get(var8));
               }
            }

            return var13.executeClickCommands((ServerPlayer)param3) ? InteractionResult.SUCCESS : InteractionResult.PASS;
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   @Override
   public FluidState getFluidState(BlockState param0) {
      return param0.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(param0);
   }

   public WoodType type() {
      return this.type;
   }

   public static WoodType getWoodType(Block param0) {
      WoodType var1;
      if (param0 instanceof SignBlock) {
         var1 = ((SignBlock)param0).type();
      } else {
         var1 = WoodType.OAK;
      }

      return var1;
   }
}

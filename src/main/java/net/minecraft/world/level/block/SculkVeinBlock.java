package net.minecraft.world.level.block;

import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;

public class SculkVeinBlock extends MultifaceBlock implements SculkBehaviour, SimpleWaterloggedBlock {
   private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private final MultifaceSpreader veinSpreader = new MultifaceSpreader(new SculkVeinBlock.SculkVeinSpreaderConfig(MultifaceSpreader.DEFAULT_SPREAD_ORDER));
   private final MultifaceSpreader sameSpaceSpreader = new MultifaceSpreader(
      new SculkVeinBlock.SculkVeinSpreaderConfig(MultifaceSpreader.SpreadType.SAME_POSITION)
   );

   public SculkVeinBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
   }

   @Override
   public MultifaceSpreader getSpreader() {
      return this.veinSpreader;
   }

   public MultifaceSpreader getSameSpaceSpreader() {
      return this.sameSpaceSpreader;
   }

   public static boolean regrow(LevelAccessor param0, BlockPos param1, BlockState param2, Collection<Direction> param3) {
      boolean var4 = false;
      BlockState var5 = Blocks.SCULK_VEIN.defaultBlockState();

      for (Direction var7 : param3) {
         BlockPos var8 = param1.relative(var7);
         if (canAttachTo(param0, var7, var8, param0.getBlockState(var8))) {
            var5 = var5.setValue(getFaceProperty(var7), true);
            var4 = true;
         }
      }

      if (!var4) {
         return false;
      }

      if (!param2.getFluidState().isEmpty()) {
         var5 = var5.setValue(WATERLOGGED, true);
      }

      param0.setBlock(param1, var5, 3);
      return true;
   }

   @Override
   public void onDischarged(LevelAccessor param0, BlockState param1, BlockPos param2, RandomSource param3) {
      if (param1.is(this)) {
         for (Direction var8 : DIRECTIONS) {
            BooleanProperty var9 = getFaceProperty(var8);
            if (param1.getValue(var9) && param0.getBlockState(param2.relative(var8)).is(Blocks.SCULK)) {
               param1 = param1.setValue(var9, false);
            }
         }

         if (!hasAnyFace(param1)) {
            FluidState var5 = param0.getFluidState(param2);
            param1 = (var5.isEmpty() ? Blocks.AIR : Blocks.WATER).defaultBlockState();
         }

         param0.setBlock(param2, param1, 3);
         SculkBehaviour.super.onDischarged(param0, param1, param2, param3);
      }
   }

   @Override
   public int attemptUseCharge(
      SculkSpreader.ChargeCursor param0, LevelAccessor param1, BlockPos param2, RandomSource param3, SculkSpreader param4, boolean param5
   ) {
      if (param5 && this.attemptPlaceSculk(param4, param1, param0.getPos(), param3)) {
         return param0.getCharge() - 1;
      } else {
         return param3.nextInt(param4.chargeDecayRate()) == 0 ? Mth.floor(param0.getCharge() * 0.5F) : param0.getCharge();
      }
   }

   private boolean attemptPlaceSculk(SculkSpreader param0, LevelAccessor param1, BlockPos param2, RandomSource param3) {
      BlockState var5 = param1.getBlockState(param2);
      TagKey<Block> var6 = param0.replaceableBlocks();

      for (Direction var8 : Direction.allShuffled(param3)) {
         if (hasFace(var5, var8)) {
            BlockPos var9 = param2.relative(var8);
            BlockState var10 = param1.getBlockState(var9);
            if (var10.is(var6)) {
               BlockState var11 = Blocks.SCULK.defaultBlockState();
               param1.setBlock(var9, var11, 3);
               Block.pushEntitiesUp(var10, var11, param1, var9);
               param1.playSound(null, var9, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 1.0F);
               this.veinSpreader.spreadAll(var11, param1, var9, param0.isWorldGeneration());
               Direction var12 = var8.getOpposite();

               for (Direction var16 : DIRECTIONS) {
                  if (var16 != var12) {
                     BlockPos var17 = var9.relative(var16);
                     BlockState var18 = param1.getBlockState(var17);
                     if (var18.is(this)) {
                        this.onDischarged(param1, var18, var17, param3);
                     }
                  }
               }

               return true;
            }
         }
      }

      return false;
   }

   public static boolean hasSubstrateAccess(LevelAccessor param0, BlockState param1, BlockPos param2) {
      if (!param1.is(Blocks.SCULK_VEIN)) {
         return false;
      }

      for (Direction var6 : DIRECTIONS) {
         if (hasFace(param1, var6) && param0.getBlockState(param2.relative(var6)).is(BlockTags.SCULK_REPLACEABLE)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param0.getValue(WATERLOGGED)) {
         param3.scheduleTick(param4, Fluids.WATER, Fluids.WATER.getTickDelay(param3));
      }

      return super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      super.createBlockStateDefinition(param0);
      param0.add(WATERLOGGED);
   }

   @Override
   public boolean canBeReplaced(BlockState param0, BlockPlaceContext param1) {
      return !param1.getItemInHand().is(Items.SCULK_VEIN) || super.canBeReplaced(param0, param1);
   }

   @Override
   public FluidState getFluidState(BlockState param0) {
      return param0.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(param0);
   }

   @Override
   public PushReaction getPistonPushReaction(BlockState param0) {
      return PushReaction.DESTROY;
   }

   class SculkVeinSpreaderConfig extends MultifaceSpreader.DefaultSpreaderConfig {
      private final MultifaceSpreader.SpreadType[] spreadTypes;

      public SculkVeinSpreaderConfig(MultifaceSpreader.SpreadType... param1) {
         super(SculkVeinBlock.this);
         this.spreadTypes = param1;
      }

      @Override
      public boolean stateCanBeReplaced(BlockGetter param0, BlockPos param1, BlockPos param2, Direction param3, BlockState param4) {
         BlockState var6 = param0.getBlockState(param2.relative(param3));
         if (!var6.is(Blocks.SCULK) && !var6.is(Blocks.SCULK_CATALYST) && !var6.is(Blocks.MOVING_PISTON)) {
            if (param1.distManhattan(param2) == 2) {
               BlockPos var7 = param1.relative(param3.getOpposite());
               if (param0.getBlockState(var7).isFaceSturdy(param0, var7, param3)) {
                  return false;
               }
            }

            FluidState var7 = param4.getFluidState();
            if (!var7.isEmpty() && !var7.is(Fluids.WATER)) {
               return false;
            }

            Material var8 = param4.getMaterial();
            return var8 == Material.FIRE ? false : param4.canBeReplaced() || super.stateCanBeReplaced(param0, param1, param2, param3, param4);
         } else {
            return false;
         }
      }

      @Override
      public MultifaceSpreader.SpreadType[] getSpreadTypes() {
         return this.spreadTypes;
      }

      @Override
      public boolean isOtherBlockValidAsSource(BlockState param0) {
         return !param0.is(Blocks.SCULK_VEIN);
      }
   }
}

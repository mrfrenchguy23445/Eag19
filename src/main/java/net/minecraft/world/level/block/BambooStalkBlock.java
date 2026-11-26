package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BambooStalkBlock extends Block implements BonemealableBlock {
   protected static final float SMALL_LEAVES_AABB_OFFSET = 3.0F;
   protected static final float LARGE_LEAVES_AABB_OFFSET = 5.0F;
   protected static final float COLLISION_AABB_OFFSET = 1.5F;
   protected static final VoxelShape SMALL_SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
   protected static final VoxelShape LARGE_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
   protected static final VoxelShape COLLISION_SHAPE = Block.box(6.5, 0.0, 6.5, 9.5, 16.0, 9.5);
   public static final IntegerProperty AGE = BlockStateProperties.AGE_1;
   public static final EnumProperty<BambooLeaves> LEAVES = BlockStateProperties.BAMBOO_LEAVES;
   public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
   public static final int MAX_HEIGHT = 16;
   public static final int STAGE_GROWING = 0;
   public static final int STAGE_DONE_GROWING = 1;
   public static final int AGE_THIN_BAMBOO = 0;
   public static final int AGE_THICK_BAMBOO = 1;

   public BambooStalkBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(LEAVES, BambooLeaves.NONE).setValue(STAGE, 0));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(AGE, LEAVES, STAGE);
   }

   @Override
   public boolean propagatesSkylightDown(BlockState param0, BlockGetter param1, BlockPos param2) {
      return true;
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      VoxelShape var5 = param0.getValue(LEAVES) == BambooLeaves.LARGE ? LARGE_SHAPE : SMALL_SHAPE;
      Vec3 var6 = param0.getOffset(param1, param2);
      return var5.move(var6.x, var6.y, var6.z);
   }

   @Override
   public boolean isPathfindable(BlockState param0, BlockGetter param1, BlockPos param2, PathComputationType param3) {
      return false;
   }

   @Override
   public VoxelShape getCollisionShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      Vec3 var5 = param0.getOffset(param1, param2);
      return COLLISION_SHAPE.move(var5.x, var5.y, var5.z);
   }

   @Override
   public boolean isCollisionShapeFullBlock(BlockState param0, BlockGetter param1, BlockPos param2) {
      return false;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext param0) {
      FluidState var2 = param0.getLevel().getFluidState(param0.getClickedPos());
      if (!var2.isEmpty()) {
         return null;
      }

      BlockState var3 = param0.getLevel().getBlockState(param0.getClickedPos().below());
      if (var3.is(BlockTags.BAMBOO_PLANTABLE_ON)) {
         if (var3.is(Blocks.BAMBOO_SAPLING)) {
            return this.defaultBlockState().setValue(AGE, 0);
         } else if (var3.is(Blocks.BAMBOO)) {
            int var4 = var3.getValue(AGE) > 0 ? 1 : 0;
            return this.defaultBlockState().setValue(AGE, var4);
         } else {
            BlockState var4 = param0.getLevel().getBlockState(param0.getClickedPos().above());
            return var4.is(Blocks.BAMBOO) ? this.defaultBlockState().setValue(AGE, var4.getValue(AGE)) : Blocks.BAMBOO_SAPLING.defaultBlockState();
         }
      } else {
         return null;
      }
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (!param0.canSurvive(param1, param2)) {
         param1.destroyBlock(param2, true);
      }
   }

   @Override
   public boolean isRandomlyTicking(BlockState param0) {
      return param0.getValue(STAGE) == 0;
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param0.getValue(STAGE) == 0) {
         if (param3.nextInt(3) == 0 && param1.isEmptyBlock(param2.above()) && param1.getRawBrightness(param2.above(), 0) >= 9) {
            int var5 = this.getHeightBelowUpToMax(param1, param2) + 1;
            if (var5 < 16) {
               this.growBamboo(param0, param1, param2, param3, var5);
            }
         }
      }
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      return param1.getBlockState(param2.below()).is(BlockTags.BAMBOO_PLANTABLE_ON);
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (!param0.canSurvive(param3, param4)) {
         param3.scheduleTick(param4, this, 1);
      }

      if (param1 == Direction.UP && param2.is(Blocks.BAMBOO) && param2.getValue(AGE) > param0.getValue(AGE)) {
         param3.setBlock(param4, param0.cycle(AGE), 2);
      }

      return super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader param0, BlockPos param1, BlockState param2, boolean param3) {
      int var5 = this.getHeightAboveUpToMax(param0, param1);
      int var6 = this.getHeightBelowUpToMax(param0, param1);
      return var5 + var6 + 1 < 16 && param0.getBlockState(param1.above(var5)).getValue(STAGE) != 1;
   }

   @Override
   public boolean isBonemealSuccess(Level param0, RandomSource param1, BlockPos param2, BlockState param3) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel param0, RandomSource param1, BlockPos param2, BlockState param3) {
      int var5 = this.getHeightAboveUpToMax(param0, param2);
      int var6 = this.getHeightBelowUpToMax(param0, param2);
      int var7 = var5 + var6 + 1;
      int var8 = 1 + param1.nextInt(2);

      for (int var9 = 0; var9 < var8; var9++) {
         BlockPos var10 = param2.above(var5);
         BlockState var11 = param0.getBlockState(var10);
         if (var7 >= 16 || var11.getValue(STAGE) == 1 || !param0.isEmptyBlock(var10.above())) {
            return;
         }

         this.growBamboo(var11, param0, var10, param1, var7);
         var5++;
         var7++;
      }
   }

   @Override
   public float getDestroyProgress(BlockState param0, Player param1, BlockGetter param2, BlockPos param3) {
      return param1.getMainHandItem().getItem() instanceof SwordItem ? 1.0F : super.getDestroyProgress(param0, param1, param2, param3);
   }

   protected void growBamboo(BlockState param0, Level param1, BlockPos param2, RandomSource param3, int param4) {
      BlockState var6 = param1.getBlockState(param2.below());
      BlockPos var7 = param2.below(2);
      BlockState var8 = param1.getBlockState(var7);
      BambooLeaves var9 = BambooLeaves.NONE;
      if (param4 >= 1) {
         if (!var6.is(Blocks.BAMBOO) || var6.getValue(LEAVES) == BambooLeaves.NONE) {
            var9 = BambooLeaves.SMALL;
         } else if (var6.is(Blocks.BAMBOO) && var6.getValue(LEAVES) != BambooLeaves.NONE) {
            var9 = BambooLeaves.LARGE;
            if (var8.is(Blocks.BAMBOO)) {
               param1.setBlock(param2.below(), var6.setValue(LEAVES, BambooLeaves.SMALL), 3);
               param1.setBlock(var7, var8.setValue(LEAVES, BambooLeaves.NONE), 3);
            }
         }
      }

      int var10 = param0.getValue(AGE) != 1 && !var8.is(Blocks.BAMBOO) ? 0 : 1;
      int var11 = (param4 < 11 || !(param3.nextFloat() < 0.25F)) && param4 != 15 ? 0 : 1;
      param1.setBlock(param2.above(), this.defaultBlockState().setValue(AGE, var10).setValue(LEAVES, var9).setValue(STAGE, var11), 3);
   }

   protected int getHeightAboveUpToMax(BlockGetter param0, BlockPos param1) {
      int var3 = 0;

      while (var3 < 16 && param0.getBlockState(param1.above(var3 + 1)).is(Blocks.BAMBOO)) {
         var3++;
      }

      return var3;
   }

   protected int getHeightBelowUpToMax(BlockGetter param0, BlockPos param1) {
      int var3 = 0;

      while (var3 < 16 && param0.getBlockState(param1.below(var3 + 1)).is(Blocks.BAMBOO)) {
         var3++;
      }

      return var3;
   }
}

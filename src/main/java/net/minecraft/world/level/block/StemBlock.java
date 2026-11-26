package net.minecraft.world.level.block;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StemBlock extends BushBlock implements BonemealableBlock {
   public static final int MAX_AGE = 7;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
   protected static final float AABB_OFFSET = 1.0F;
   protected static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
      Block.box(7.0, 0.0, 7.0, 9.0, 2.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 4.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 6.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 8.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 10.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 12.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 14.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0)
   };
   private final StemGrownBlock fruit;
   private final Supplier<Item> seedSupplier;

   protected StemBlock(StemGrownBlock param0, Supplier<Item> param1, BlockBehaviour.Properties param2) {
      super(param2);
      this.fruit = param0;
      this.seedSupplier = param1;
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE_BY_AGE[param0.getValue(AGE)];
   }

   @Override
   protected boolean mayPlaceOn(BlockState param0, BlockGetter param1, BlockPos param2) {
      return param0.is(Blocks.FARMLAND);
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param1.getRawBrightness(param2, 0) >= 9) {
         float var5 = CropBlock.getGrowthSpeed(this, param1, param2);
         if (param3.nextInt((int)(25.0F / var5) + 1) == 0) {
            int var6 = param0.getValue(AGE);
            if (var6 < 7) {
               param0 = param0.setValue(AGE, var6 + 1);
               param1.setBlock(param2, param0, 2);
            } else {
               Direction var7 = Direction.Plane.HORIZONTAL.getRandomDirection(param3);
               BlockPos var8 = param2.relative(var7);
               BlockState var9 = param1.getBlockState(var8.below());
               if (param1.getBlockState(var8).isAir() && (var9.is(Blocks.FARMLAND) || var9.is(BlockTags.DIRT))) {
                  param1.setBlockAndUpdate(var8, this.fruit.defaultBlockState());
                  param1.setBlockAndUpdate(param2, this.fruit.getAttachedStem().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, var7));
               }
            }
         }
      }
   }

   @Override
   public ItemStack getCloneItemStack(BlockGetter param0, BlockPos param1, BlockState param2) {
      return new ItemStack(this.seedSupplier.get());
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader param0, BlockPos param1, BlockState param2, boolean param3) {
      return param2.getValue(AGE) != 7;
   }

   @Override
   public boolean isBonemealSuccess(Level param0, RandomSource param1, BlockPos param2, BlockState param3) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel param0, RandomSource param1, BlockPos param2, BlockState param3) {
      int var5 = Math.min(7, param3.getValue(AGE) + Mth.nextInt(param0.random, 2, 5));
      BlockState var6 = param3.setValue(AGE, var5);
      param0.setBlock(param2, var6, 2);
      if (var5 == 7) {
         var6.randomTick(param0, param2, param0.random);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(AGE);
   }

   public StemGrownBlock getFruit() {
      return this.fruit;
   }
}

package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CropBlock extends BushBlock implements BonemealableBlock {
   public static final int MAX_AGE = 7;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
   private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
      Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
   };

   protected CropBlock(BlockBehaviour.Properties param0) {
      super(param0);
      this.registerDefaultState(this.stateDefinition.any().setValue(this.getAgeProperty(), 0));
   }

   @Override
   public VoxelShape getShape(BlockState param0, BlockGetter param1, BlockPos param2, CollisionContext param3) {
      return SHAPE_BY_AGE[param0.getValue(this.getAgeProperty())];
   }

   @Override
   protected boolean mayPlaceOn(BlockState param0, BlockGetter param1, BlockPos param2) {
      return param0.is(Blocks.FARMLAND);
   }

   public IntegerProperty getAgeProperty() {
      return AGE;
   }

   public int getMaxAge() {
      return 7;
   }

   protected int getAge(BlockState param0) {
      return param0.getValue(this.getAgeProperty());
   }

   public BlockState getStateForAge(int param0) {
      return this.defaultBlockState().setValue(this.getAgeProperty(), param0);
   }

   public boolean isMaxAge(BlockState param0) {
      return param0.getValue(this.getAgeProperty()) >= this.getMaxAge();
   }

   @Override
   public boolean isRandomlyTicking(BlockState param0) {
      return !this.isMaxAge(param0);
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (param1.getRawBrightness(param2, 0) >= 9) {
         int var5 = this.getAge(param0);
         if (var5 < this.getMaxAge()) {
            float var6 = getGrowthSpeed(this, param1, param2);
            if (param3.nextInt((int)(25.0F / var6) + 1) == 0) {
               param1.setBlock(param2, this.getStateForAge(var5 + 1), 2);
            }
         }
      }
   }

   public void growCrops(Level param0, BlockPos param1, BlockState param2) {
      int var4 = this.getAge(param2) + this.getBonemealAgeIncrease(param0);
      int var5 = this.getMaxAge();
      if (var4 > var5) {
         var4 = var5;
      }

      param0.setBlock(param1, this.getStateForAge(var4), 2);
   }

   protected int getBonemealAgeIncrease(Level param0) {
      return Mth.nextInt(param0.random, 2, 5);
   }

   protected static float getGrowthSpeed(Block param0, BlockGetter param1, BlockPos param2) {
      float var3 = 1.0F;
      BlockPos var4 = param2.below();

      for (int var5 = -1; var5 <= 1; var5++) {
         for (int var6 = -1; var6 <= 1; var6++) {
            float var7 = 0.0F;
            BlockState var8 = param1.getBlockState(var4.offset(var5, 0, var6));
            if (var8.is(Blocks.FARMLAND)) {
               var7 = 1.0F;
               if (var8.getValue(FarmBlock.MOISTURE) > 0) {
                  var7 = 3.0F;
               }
            }

            if (var5 != 0 || var6 != 0) {
               var7 /= 4.0F;
            }

            var3 += var7;
         }
      }

      BlockPos var5 = param2.north();
      BlockPos var6 = param2.south();
      BlockPos var7 = param2.west();
      BlockPos var8 = param2.east();
      boolean var9 = param1.getBlockState(var7).is(param0) || param1.getBlockState(var8).is(param0);
      boolean var10 = param1.getBlockState(var5).is(param0) || param1.getBlockState(var6).is(param0);
      if (var9 && var10) {
         var3 /= 2.0F;
      } else {
         boolean var11 = param1.getBlockState(var7.north()).is(param0)
            || param1.getBlockState(var8.north()).is(param0)
            || param1.getBlockState(var8.south()).is(param0)
            || param1.getBlockState(var7.south()).is(param0);
         if (var11) {
            var3 /= 2.0F;
         }
      }

      return var3;
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      return (param1.getRawBrightness(param2, 0) >= 8 || param1.canSeeSky(param2)) && super.canSurvive(param0, param1, param2);
   }

   @Override
   public void entityInside(BlockState param0, Level param1, BlockPos param2, Entity param3) {
      if (param3 instanceof Ravager && param1.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
         param1.destroyBlock(param2, true, param3);
      }

      super.entityInside(param0, param1, param2, param3);
   }

   protected ItemLike getBaseSeedId() {
      return Items.WHEAT_SEEDS;
   }

   @Override
   public ItemStack getCloneItemStack(BlockGetter param0, BlockPos param1, BlockState param2) {
      return new ItemStack(this.getBaseSeedId());
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader param0, BlockPos param1, BlockState param2, boolean param3) {
      return !this.isMaxAge(param2);
   }

   @Override
   public boolean isBonemealSuccess(Level param0, RandomSource param1, BlockPos param2, BlockState param3) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel param0, RandomSource param1, BlockPos param2, BlockState param3) {
      this.growCrops(param0, param2, param3);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(AGE);
   }
}

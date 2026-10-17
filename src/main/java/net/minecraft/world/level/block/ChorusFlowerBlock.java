package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ChorusFlowerBlock extends Block {
   public static final int DEAD_AGE = 5;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_5;
   private final ChorusPlantBlock plant;

   protected ChorusFlowerBlock(ChorusPlantBlock param0, BlockBehaviour.Properties param1) {
      super(param1);
      this.plant = param0;
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
   }

   @Override
   public void tick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      if (!param0.canSurvive(param1, param2)) {
         param1.destroyBlock(param2, true);
      }
   }

   @Override
   public boolean isRandomlyTicking(BlockState param0) {
      return param0.getValue(AGE) < 5;
   }

   @Override
   public void randomTick(BlockState param0, ServerLevel param1, BlockPos param2, RandomSource param3) {
      BlockPos var5 = param2.above();
      if (param1.isEmptyBlock(var5) && var5.getY() < param1.getMaxBuildHeight()) {
         int var6 = param0.getValue(AGE);
         if (var6 < 5) {
            boolean var7 = false;
            boolean var8 = false;
            BlockState var9 = param1.getBlockState(param2.below());
            if (var9.is(Blocks.END_STONE)) {
               var7 = true;
            } else if (var9.is(this.plant)) {
               int var10 = 1;

               for (int var11 = 0; var11 < 4; var11++) {
                  BlockState var12 = param1.getBlockState(param2.below(var10 + 1));
                  if (!var12.is(this.plant)) {
                     if (var12.is(Blocks.END_STONE)) {
                        var8 = true;
                     }
                     break;
                  }

                  var10++;
               }

               if (var10 < 2 || var10 <= param3.nextInt(var8 ? 5 : 4)) {
                  var7 = true;
               }
            } else if (var9.isAir()) {
               var7 = true;
            }

            if (var7 && allNeighborsEmpty(param1, var5, null) && param1.isEmptyBlock(param2.above(2))) {
               param1.setBlock(param2, this.plant.getStateForPlacement(param1, param2), 2);
               this.placeGrownFlower(param1, var5, var6);
            } else if (var6 < 4) {
               int var10 = param3.nextInt(4);
               if (var8) {
                  var10++;
               }

               boolean var11 = false;

               for (int var12 = 0; var12 < var10; var12++) {
                  Direction var13 = Direction.Plane.HORIZONTAL.getRandomDirection(param3);
                  BlockPos var14 = param2.relative(var13);
                  if (param1.isEmptyBlock(var14) && param1.isEmptyBlock(var14.below()) && allNeighborsEmpty(param1, var14, var13.getOpposite())) {
                     this.placeGrownFlower(param1, var14, var6 + 1);
                     var11 = true;
                  }
               }

               if (var11) {
                  param1.setBlock(param2, this.plant.getStateForPlacement(param1, param2), 2);
               } else {
                  this.placeDeadFlower(param1, param2);
               }
            } else {
               this.placeDeadFlower(param1, param2);
            }
         }
      }
   }

   private void placeGrownFlower(Level param0, BlockPos param1, int param2) {
      param0.setBlock(param1, this.defaultBlockState().setValue(AGE, param2), 2);
      param0.levelEvent(1033, param1, 0);
   }

   private void placeDeadFlower(Level param0, BlockPos param1) {
      param0.setBlock(param1, this.defaultBlockState().setValue(AGE, 5), 2);
      param0.levelEvent(1034, param1, 0);
   }

   private static boolean allNeighborsEmpty(LevelReader param0, BlockPos param1, @Nullable Direction param2) {
      for (Direction var4 : Direction.Plane.HORIZONTAL) {
         if (var4 != param2 && !param0.isEmptyBlock(param1.relative(var4))) {
            return false;
         }
      }

      return true;
   }

   @Override
   public BlockState updateShape(BlockState param0, Direction param1, BlockState param2, LevelAccessor param3, BlockPos param4, BlockPos param5) {
      if (param1 != Direction.UP && !param0.canSurvive(param3, param4)) {
         param3.scheduleTick(param4, this, 1);
      }

      return super.updateShape(param0, param1, param2, param3, param4, param5);
   }

   @Override
   public boolean canSurvive(BlockState param0, LevelReader param1, BlockPos param2) {
      BlockState var4 = param1.getBlockState(param2.below());
      if (!var4.is(this.plant) && !var4.is(Blocks.END_STONE)) {
         if (!var4.isAir()) {
            return false;
         }

         boolean var5 = false;

         for (Direction var7 : Direction.Plane.HORIZONTAL) {
            BlockState var8 = param1.getBlockState(param2.relative(var7));
            if (var8.is(this.plant)) {
               if (var5) {
                  return false;
               }

               var5 = true;
            } else if (!var8.isAir()) {
               return false;
            }
         }

         return var5;
      } else {
         return true;
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> param0) {
      param0.add(AGE);
   }

   public static void generatePlant(LevelAccessor param0, BlockPos param1, RandomSource param2, int param3) {
      param0.setBlock(param1, ((ChorusPlantBlock)Blocks.CHORUS_PLANT).getStateForPlacement(param0, param1), 2);
      growTreeRecursive(param0, param1, param2, param1, param3, 0);
   }

   private static void growTreeRecursive(LevelAccessor param0, BlockPos param1, RandomSource param2, BlockPos param3, int param4, int param5) {
      ChorusPlantBlock var6 = (ChorusPlantBlock)Blocks.CHORUS_PLANT;
      int var7 = param2.nextInt(4) + 1;
      if (param5 == 0) {
         var7++;
      }

      for (int var8 = 0; var8 < var7; var8++) {
         BlockPos var9 = param1.above(var8 + 1);
         if (!allNeighborsEmpty(param0, var9, null)) {
            return;
         }

         param0.setBlock(var9, var6.getStateForPlacement(param0, var9), 2);
         param0.setBlock(var9.below(), var6.getStateForPlacement(param0, var9.below()), 2);
      }

      boolean var8 = false;
      if (param5 < 4) {
         int var9 = param2.nextInt(4);
         if (param5 == 0) {
            var9++;
         }

         for (int var10 = 0; var10 < var9; var10++) {
            Direction var11 = Direction.Plane.HORIZONTAL.getRandomDirection(param2);
            BlockPos var12 = param1.above(var7).relative(var11);
            if (Math.abs(var12.getX() - param3.getX()) < param4
               && Math.abs(var12.getZ() - param3.getZ()) < param4
               && param0.isEmptyBlock(var12)
               && param0.isEmptyBlock(var12.below())
               && allNeighborsEmpty(param0, var12, var11.getOpposite())) {
               var8 = true;
               param0.setBlock(var12, var6.getStateForPlacement(param0, var12), 2);
               param0.setBlock(var12.relative(var11.getOpposite()), var6.getStateForPlacement(param0, var12.relative(var11.getOpposite())), 2);
               growTreeRecursive(param0, var12, param2, param3, param4, param5 + 1);
            }
         }
      }

      if (!var8) {
         param0.setBlock(param1.above(var7), Blocks.CHORUS_FLOWER.defaultBlockState().setValue(AGE, 5), 2);
      }
   }

   @Override
   public void onProjectileHit(Level param0, BlockState param1, BlockHitResult param2, Projectile param3) {
      BlockPos var5 = param2.getBlockPos();
      if (!param0.isClientSide && param3.mayInteract(param0, var5) && param3.getType().is(EntityTypeTags.IMPACT_PROJECTILES)) {
         param0.destroyBlock(var5, true, param3);
      }
   }
}

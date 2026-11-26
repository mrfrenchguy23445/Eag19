package net.minecraft.world.level.levelgen.feature;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

public class DripstoneUtils {
   protected static double getDripstoneHeight(double param0, double param2, double param4, double param6) {
      if (param0 < param6) {
         param0 = param6;
      }

      double var8 = 0.384;
      double var10 = param0 / param2 * 0.384;
      double var12 = 0.75 * Math.pow(var10, 1.3333333333333333);
      double var14 = Math.pow(var10, 0.6666666666666666);
      double var16 = 0.3333333333333333 * Math.log(var10);
      double var18 = param4 * (var12 - var14 - var16);
      var18 = Math.max(var18, 0.0);
      return var18 / 0.384 * param2;
   }

   protected static boolean isCircleMostlyEmbeddedInStone(WorldGenLevel param0, BlockPos param1, int param2) {
      if (isEmptyOrWaterOrLava(param0, param1)) {
         return false;
      }

      float var3 = 6.0F;
      float var4 = 6.0F / param2;

      for (float var5 = 0.0F; var5 < (float) (Math.PI * 2); var5 += var4) {
         int var6 = (int)(Mth.cos(var5) * param2);
         int var7 = (int)(Mth.sin(var5) * param2);
         if (isEmptyOrWaterOrLava(param0, param1.offset(var6, 0, var7))) {
            return false;
         }
      }

      return true;
   }

   protected static boolean isEmptyOrWater(LevelAccessor param0, BlockPos param1) {
      return param0.isStateAtPosition(param1, DripstoneUtils::isEmptyOrWater);
   }

   protected static boolean isEmptyOrWaterOrLava(LevelAccessor param0, BlockPos param1) {
      return param0.isStateAtPosition(param1, DripstoneUtils::isEmptyOrWaterOrLava);
   }

   protected static void buildBaseToTipColumn(Direction param0, int param1, boolean param2, Consumer<BlockState> param3) {
      if (param1 >= 3) {
         param3.accept(createPointedDripstone(param0, DripstoneThickness.BASE));

         for (int var4 = 0; var4 < param1 - 3; var4++) {
            param3.accept(createPointedDripstone(param0, DripstoneThickness.MIDDLE));
         }
      }

      if (param1 >= 2) {
         param3.accept(createPointedDripstone(param0, DripstoneThickness.FRUSTUM));
      }

      if (param1 >= 1) {
         param3.accept(createPointedDripstone(param0, param2 ? DripstoneThickness.TIP_MERGE : DripstoneThickness.TIP));
      }
   }

   protected static void growPointedDripstone(LevelAccessor param0, BlockPos param1, Direction param2, int param3, boolean param4) {
      if (isDripstoneBase(param0.getBlockState(param1.relative(param2.getOpposite())))) {
         BlockPos.MutableBlockPos var5 = param1.mutable();
         buildBaseToTipColumn(param2, param3, param4, l_param3 -> {
            if (l_param3.is(Blocks.POINTED_DRIPSTONE)) {
               l_param3 = l_param3.setValue(PointedDripstoneBlock.WATERLOGGED, param0.isWaterAt(var5));
            }

            param0.setBlock(var5, l_param3, 2);
            var5.move(param2);
         });
      }
   }

   protected static boolean placeDripstoneBlockIfPossible(LevelAccessor param0, BlockPos param1) {
      BlockState var2 = param0.getBlockState(param1);
      if (var2.is(BlockTags.DRIPSTONE_REPLACEABLE)) {
         param0.setBlock(param1, Blocks.DRIPSTONE_BLOCK.defaultBlockState(), 2);
         return true;
      } else {
         return false;
      }
   }

   private static BlockState createPointedDripstone(Direction param0, DripstoneThickness param1) {
      return Blocks.POINTED_DRIPSTONE
         .defaultBlockState()
         .setValue(PointedDripstoneBlock.TIP_DIRECTION, param0)
         .setValue(PointedDripstoneBlock.THICKNESS, param1);
   }

   public static boolean isDripstoneBaseOrLava(BlockState param0) {
      return isDripstoneBase(param0) || param0.is(Blocks.LAVA);
   }

   public static boolean isDripstoneBase(BlockState param0) {
      return param0.is(Blocks.DRIPSTONE_BLOCK) || param0.is(BlockTags.DRIPSTONE_REPLACEABLE);
   }

   public static boolean isEmptyOrWater(BlockState param0) {
      return param0.isAir() || param0.is(Blocks.WATER);
   }

   public static boolean isNeitherEmptyNorWater(BlockState param0) {
      return !param0.isAir() && !param0.is(Blocks.WATER);
   }

   public static boolean isEmptyOrWaterOrLava(BlockState param0) {
      return param0.isAir() || param0.is(Blocks.WATER) || param0.is(Blocks.LAVA);
   }
}

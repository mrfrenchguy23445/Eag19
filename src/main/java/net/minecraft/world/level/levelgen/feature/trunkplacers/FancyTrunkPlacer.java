package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class FancyTrunkPlacer extends TrunkPlacer {
   public static final Codec<FancyTrunkPlacer> CODEC = RecordCodecBuilder.create(param0 -> trunkPlacerParts(param0).apply(param0, FancyTrunkPlacer::new));
   private static final double TRUNK_HEIGHT_SCALE = 0.618;
   private static final double CLUSTER_DENSITY_MAGIC = 1.382;
   private static final double BRANCH_SLOPE = 0.381;
   private static final double BRANCH_LENGTH_MAGIC = 0.328;

   public FancyTrunkPlacer(int param0, int param1, int param2) {
      super(param0, param1, param2);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.FANCY_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader param0, BiConsumer<BlockPos, BlockState> param1, RandomSource param2, int param3, BlockPos param4, TreeConfiguration param5
   ) {
      int var7 = 5;
      int var8 = param3 + 2;
      int var9 = Mth.floor(var8 * 0.618);
      setDirtAt(param0, param1, param2, param4.below(), param5);
      double var10 = 1.0;
      int var12 = Math.min(1, Mth.floor(1.382 + Math.pow(1.0 * var8 / 13.0, 2.0)));
      int var13 = param4.getY() + var9;
      int var14 = var8 - 5;
      List<FancyTrunkPlacer.FoliageCoords> var15 = Lists.newArrayList();
      var15.add(new FancyTrunkPlacer.FoliageCoords(param4.above(var14), var13));

      for (; var14 >= 0; var14--) {
         float var16 = treeShape(var8, var14);
         if (!(var16 < 0.0F)) {
            for (int var17 = 0; var17 < var12; var17++) {
               double var18 = 1.0;
               double var20 = 1.0 * var16 * (param2.nextFloat() + 0.328);
               double var22 = param2.nextFloat() * 2.0F * Math.PI;
               double var24 = var20 * Math.sin(var22) + 0.5;
               double var26 = var20 * Math.cos(var22) + 0.5;
               BlockPos var28 = param4.offset(Mth.floor(var24), var14 - 1, Mth.floor(var26));
               BlockPos var29 = var28.above(5);
               if (this.makeLimb(param0, param1, param2, var28, var29, false, param5)) {
                  int var30 = param4.getX() - var28.getX();
                  int var31 = param4.getZ() - var28.getZ();
                  double var32 = var28.getY() - Math.sqrt(var30 * var30 + var31 * var31) * 0.381;
                  int var34 = var32 > var13 ? var13 : (int)var32;
                  BlockPos var35 = new BlockPos(param4.getX(), var34, param4.getZ());
                  if (this.makeLimb(param0, param1, param2, var35, var28, false, param5)) {
                     var15.add(new FancyTrunkPlacer.FoliageCoords(var28, var35.getY()));
                  }
               }
            }
         }
      }

      this.makeLimb(param0, param1, param2, param4, param4.above(var9), true, param5);
      this.makeBranches(param0, param1, param2, var8, param4, var15, param5);
      List<FoliagePlacer.FoliageAttachment> var16 = Lists.newArrayList();

      for (FancyTrunkPlacer.FoliageCoords var18 : var15) {
         if (this.trimBranches(var8, var18.getBranchBase() - param4.getY())) {
            var16.add(var18.attachment);
         }
      }

      return var16;
   }

   private boolean makeLimb(
      LevelSimulatedReader param0,
      BiConsumer<BlockPos, BlockState> param1,
      RandomSource param2,
      BlockPos param3,
      BlockPos param4,
      boolean param5,
      TreeConfiguration param6
   ) {
      if (!param5 && Objects.equals(param3, param4)) {
         return true;
      }

      BlockPos var8 = param4.offset(-param3.getX(), -param3.getY(), -param3.getZ());
      int var9 = this.getSteps(var8);
      float var10 = (float)var8.getX() / var9;
      float var11 = (float)var8.getY() / var9;
      float var12 = (float)var8.getZ() / var9;

      for (int var13 = 0; var13 <= var9; var13++) {
         BlockPos var14 = param3.offset(Mth.floor(0.5F + var13 * var10), Mth.floor(0.5F + var13 * var11), Mth.floor(0.5F + var13 * var12));
         if (param5) {
            this.placeLog(param0, param1, param2, var14, param6, l_param2 -> l_param2.trySetValue(RotatedPillarBlock.AXIS, this.getLogAxis(param3, var14)));
         } else if (!this.isFree(param0, var14)) {
            return false;
         }
      }

      return true;
   }

   private int getSteps(BlockPos param0) {
      int var2 = Mth.abs(param0.getX());
      int var3 = Mth.abs(param0.getY());
      int var4 = Mth.abs(param0.getZ());
      return Math.max(var2, Math.max(var3, var4));
   }

   private Direction.Axis getLogAxis(BlockPos param0, BlockPos param1) {
      Direction.Axis var3 = Direction.Axis.Y;
      int var4 = Math.abs(param1.getX() - param0.getX());
      int var5 = Math.abs(param1.getZ() - param0.getZ());
      int var6 = Math.max(var4, var5);
      if (var6 > 0) {
         if (var4 == var6) {
            var3 = Direction.Axis.X;
         } else {
            var3 = Direction.Axis.Z;
         }
      }

      return var3;
   }

   private boolean trimBranches(int param0, int param1) {
      return param1 >= param0 * 0.2;
   }

   private void makeBranches(
      LevelSimulatedReader param0,
      BiConsumer<BlockPos, BlockState> param1,
      RandomSource param2,
      int param3,
      BlockPos param4,
      List<FancyTrunkPlacer.FoliageCoords> param5,
      TreeConfiguration param6
   ) {
      for (FancyTrunkPlacer.FoliageCoords var9 : param5) {
         int var10 = var9.getBranchBase();
         BlockPos var11 = new BlockPos(param4.getX(), var10, param4.getZ());
         if (!var11.equals(var9.attachment.pos()) && this.trimBranches(param3, var10 - param4.getY())) {
            this.makeLimb(param0, param1, param2, var11, var9.attachment.pos(), true, param6);
         }
      }
   }

   private static float treeShape(int param0, int param1) {
      if (param1 < param0 * 0.3F) {
         return -1.0F;
      }

      float var2 = param0 / 2.0F;
      float var3 = var2 - param1;
      float var4 = Mth.sqrt(var2 * var2 - var3 * var3);
      if (var3 == 0.0F) {
         var4 = var2;
      } else if (Math.abs(var3) >= var2) {
         return 0.0F;
      }

      return var4 * 0.5F;
   }

   static class FoliageCoords {
      final FoliagePlacer.FoliageAttachment attachment;
      private final int branchBase;

      public FoliageCoords(BlockPos param0, int param1) {
         this.attachment = new FoliagePlacer.FoliageAttachment(param0, 0, false);
         this.branchBase = param1;
      }

      public int getBranchBase() {
         return this.branchBase;
      }
   }
}

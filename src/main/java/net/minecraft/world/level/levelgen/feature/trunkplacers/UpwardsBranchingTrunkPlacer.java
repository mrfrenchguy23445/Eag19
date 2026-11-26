package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class UpwardsBranchingTrunkPlacer extends TrunkPlacer {
   public static final Codec<UpwardsBranchingTrunkPlacer> CODEC = RecordCodecBuilder.create(
      param0 -> trunkPlacerParts(param0)
         .and(
            param0.group(
               IntProvider.POSITIVE_CODEC.fieldOf("extra_branch_steps").forGetter(l_param0 -> l_param0.extraBranchSteps),
               Codec.floatRange(0.0F, 1.0F).fieldOf("place_branch_per_log_probability").forGetter(l_param0 -> l_param0.placeBranchPerLogProbability),
               IntProvider.NON_NEGATIVE_CODEC.fieldOf("extra_branch_length").forGetter(l_param0 -> l_param0.extraBranchLength),
               RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("can_grow_through").forGetter(l_param0 -> l_param0.canGrowThrough)
            )
         )
         .apply(param0, UpwardsBranchingTrunkPlacer::new)
   );
   private final IntProvider extraBranchSteps;
   private final float placeBranchPerLogProbability;
   private final IntProvider extraBranchLength;
   private final HolderSet<Block> canGrowThrough;

   public UpwardsBranchingTrunkPlacer(int param0, int param1, int param2, IntProvider param3, float param4, IntProvider param5, HolderSet<Block> param6) {
      super(param0, param1, param2);
      this.extraBranchSteps = param3;
      this.placeBranchPerLogProbability = param4;
      this.extraBranchLength = param5;
      this.canGrowThrough = param6;
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.UPWARDS_BRANCHING_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader param0, BiConsumer<BlockPos, BlockState> param1, RandomSource param2, int param3, BlockPos param4, TreeConfiguration param5
   ) {
      List<FoliagePlacer.FoliageAttachment> var7 = Lists.newArrayList();
      BlockPos.MutableBlockPos var8 = new BlockPos.MutableBlockPos();

      for (int var9 = 0; var9 < param3; var9++) {
         int var10 = param4.getY() + var9;
         if (this.placeLog(param0, param1, param2, var8.set(param4.getX(), var10, param4.getZ()), param5)
            && var9 < param3 - 1
            && param2.nextFloat() < this.placeBranchPerLogProbability) {
            Direction var11 = Direction.Plane.HORIZONTAL.getRandomDirection(param2);
            int var12 = this.extraBranchLength.sample(param2);
            int var13 = Math.max(0, var12 - this.extraBranchLength.sample(param2) - 1);
            int var14 = this.extraBranchSteps.sample(param2);
            this.placeBranch(param0, param1, param2, param3, param5, var7, var8, var10, var11, var13, var14);
         }

         if (var9 == param3 - 1) {
            var7.add(new FoliagePlacer.FoliageAttachment(var8.set(param4.getX(), var10 + 1, param4.getZ()), 0, false));
         }
      }

      return var7;
   }

   private void placeBranch(
      LevelSimulatedReader param0,
      BiConsumer<BlockPos, BlockState> param1,
      RandomSource param2,
      int param3,
      TreeConfiguration param4,
      List<FoliagePlacer.FoliageAttachment> param5,
      BlockPos.MutableBlockPos param6,
      int param7,
      Direction param8,
      int param9,
      int param10
   ) {
      int var12 = param7 + param9;
      int var13 = param6.getX();
      int var14 = param6.getZ();
      int var15 = param9;

      while (var15 < param3 && param10 > 0) {
         if (var15 >= 1) {
            int var16 = param7 + var15;
            var13 += param8.getStepX();
            var14 += param8.getStepZ();
            var12 = var16;
            if (this.placeLog(param0, param1, param2, param6.set(var13, var16, var14), param4)) {
               var12++;
            }

            param5.add(new FoliagePlacer.FoliageAttachment(param6.immutable(), 0, false));
         }

         var15++;
         param10--;
      }

      if (var12 - param7 > 1) {
         BlockPos var15x = new BlockPos(var13, var12, var14);
         param5.add(new FoliagePlacer.FoliageAttachment(var15x, 0, false));
         param5.add(new FoliagePlacer.FoliageAttachment(var15x.below(2), 0, false));
      }
   }

   @Override
   protected boolean validTreePos(LevelSimulatedReader param0, BlockPos param1) {
      return super.validTreePos(param0, param1) || param0.isStateAtPosition(param1, l_param0 -> l_param0.is(this.canGrowThrough));
   }
}

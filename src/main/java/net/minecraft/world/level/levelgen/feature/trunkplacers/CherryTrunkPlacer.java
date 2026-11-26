package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class CherryTrunkPlacer extends TrunkPlacer {
   private static final Codec<UniformInt> BRANCH_START_CODEC = ExtraCodecs.validate(
      UniformInt.CODEC,
      param0 -> param0.getMaxValue() - param0.getMinValue() < 1
         ? DataResult.error(() -> "Need at least 2 blocks variation for the branch starts to fit both branches")
         : DataResult.success(param0)
   );
   public static final Codec<CherryTrunkPlacer> CODEC = RecordCodecBuilder.create(
      param0 -> trunkPlacerParts(param0)
         .and(
            param0.group(
               IntProvider.codec(1, 3).fieldOf("branch_count").forGetter(l_param0 -> l_param0.branchCount),
               IntProvider.codec(2, 16).fieldOf("branch_horizontal_length").forGetter(l_param0 -> l_param0.branchHorizontalLength),
               IntProvider.codec(-16, 0, BRANCH_START_CODEC).fieldOf("branch_start_offset_from_top").forGetter(l_param0 -> l_param0.branchStartOffsetFromTop),
               IntProvider.codec(-16, 16).fieldOf("branch_end_offset_from_top").forGetter(l_param0 -> l_param0.branchEndOffsetFromTop)
            )
         )
         .apply(param0, CherryTrunkPlacer::new)
   );
   private final IntProvider branchCount;
   private final IntProvider branchHorizontalLength;
   private final UniformInt branchStartOffsetFromTop;
   private final UniformInt secondBranchStartOffsetFromTop;
   private final IntProvider branchEndOffsetFromTop;

   public CherryTrunkPlacer(int param0, int param1, int param2, IntProvider param3, IntProvider param4, UniformInt param5, IntProvider param6) {
      super(param0, param1, param2);
      this.branchCount = param3;
      this.branchHorizontalLength = param4;
      this.branchStartOffsetFromTop = param5;
      this.secondBranchStartOffsetFromTop = UniformInt.of(param5.getMinValue(), param5.getMaxValue() - 1);
      this.branchEndOffsetFromTop = param6;
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.CHERRY_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader param0, BiConsumer<BlockPos, BlockState> param1, RandomSource param2, int param3, BlockPos param4, TreeConfiguration param5
   ) {
      setDirtAt(param0, param1, param2, param4.below(), param5);
      int var7 = Math.max(0, param3 - 1 + this.branchStartOffsetFromTop.sample(param2));
      int var8 = Math.max(0, param3 - 1 + this.secondBranchStartOffsetFromTop.sample(param2));
      if (var8 >= var7) {
         var8++;
      }

      int var9 = this.branchCount.sample(param2);
      boolean var10 = var9 == 3;
      boolean var11 = var9 >= 2;
      int var12;
      if (var10) {
         var12 = param3;
      } else if (var11) {
         var12 = Math.max(var7, var8) + 1;
      } else {
         var12 = var7 + 1;
      }

      for (int var13 = 0; var13 < var12; var13++) {
         this.placeLog(param0, param1, param2, param4.above(var13), param5);
      }

      List<FoliagePlacer.FoliageAttachment> var13 = new ArrayList<>();
      if (var10) {
         var13.add(new FoliagePlacer.FoliageAttachment(param4.above(var12), 0, false));
      }

      BlockPos.MutableBlockPos var14 = new BlockPos.MutableBlockPos();
      Direction var15 = Direction.Plane.HORIZONTAL.getRandomDirection(param2);
      Function<BlockState, BlockState> var16 = l_param1 -> l_param1.trySetValue(RotatedPillarBlock.AXIS, var15.getAxis());
      var13.add(this.generateBranch(param0, param1, param2, param3, param4, param5, var16, var15, var7, var7 < var12 - 1, var14));
      if (var11) {
         var13.add(this.generateBranch(param0, param1, param2, param3, param4, param5, var16, var15.getOpposite(), var8, var8 < var12 - 1, var14));
      }

      return var13;
   }

   private FoliagePlacer.FoliageAttachment generateBranch(
      LevelSimulatedReader param0,
      BiConsumer<BlockPos, BlockState> param1,
      RandomSource param2,
      int param3,
      BlockPos param4,
      TreeConfiguration param5,
      Function<BlockState, BlockState> param6,
      Direction param7,
      int param8,
      boolean param9,
      BlockPos.MutableBlockPos param10
   ) {
      param10.set(param4).move(Direction.UP, param8);
      int var12 = param3 - 1 + this.branchEndOffsetFromTop.sample(param2);
      boolean var13 = param9 || var12 < param8;
      int var14 = this.branchHorizontalLength.sample(param2) + (var13 ? 1 : 0);
      BlockPos var15 = param4.relative(param7, var14).above(var12);
      int var16 = var13 ? 2 : 1;

      for (int var17 = 0; var17 < var16; var17++) {
         this.placeLog(param0, param1, param2, param10.move(param7), param5, param6);
      }

      Direction var17 = var15.getY() > param10.getY() ? Direction.UP : Direction.DOWN;

      while (true) {
         int var18 = param10.distManhattan(var15);
         if (var18 == 0) {
            return new FoliagePlacer.FoliageAttachment(var15.above(), 0, false);
         }

         float var19 = (float)Math.abs(var15.getY() - param10.getY()) / var18;
         boolean var20 = param2.nextFloat() < var19;
         param10.move(var20 ? var17 : param7);
         this.placeLog(param0, param1, param2, param10, param5, var20 ? Function.identity() : param6);
      }
   }
}

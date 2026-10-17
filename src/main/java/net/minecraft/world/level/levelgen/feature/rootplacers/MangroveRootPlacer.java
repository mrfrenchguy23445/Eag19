package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class MangroveRootPlacer extends RootPlacer {
   public static final int ROOT_WIDTH_LIMIT = 8;
   public static final int ROOT_LENGTH_LIMIT = 15;
   public static final Codec<MangroveRootPlacer> CODEC = RecordCodecBuilder.create(
      param0 -> rootPlacerParts(param0)
         .and(MangroveRootPlacement.CODEC.fieldOf("mangrove_root_placement").forGetter(l_param0 -> l_param0.mangroveRootPlacement))
         .apply(param0, MangroveRootPlacer::new)
   );
   private final MangroveRootPlacement mangroveRootPlacement;

   public MangroveRootPlacer(IntProvider param0, BlockStateProvider param1, Optional<AboveRootPlacement> param2, MangroveRootPlacement param3) {
      super(param0, param1, param2);
      this.mangroveRootPlacement = param3;
   }

   @Override
   public boolean placeRoots(
      LevelSimulatedReader param0, BiConsumer<BlockPos, BlockState> param1, RandomSource param2, BlockPos param3, BlockPos param4, TreeConfiguration param5
   ) {
      List<BlockPos> var7 = Lists.newArrayList();
      BlockPos.MutableBlockPos var8 = param3.mutable();

      while (var8.getY() < param4.getY()) {
         if (!this.canPlaceRoot(param0, var8)) {
            return false;
         }

         var8.move(Direction.UP);
      }

      var7.add(param4.below());

      for (Direction var10 : Direction.Plane.HORIZONTAL) {
         BlockPos var11 = param4.relative(var10);
         List<BlockPos> var12 = Lists.newArrayList();
         if (!this.simulateRoots(param0, param2, var11, var10, param4, var12, 0)) {
            return false;
         }

         var7.addAll(var12);
         var7.add(param4.relative(var10));
      }

      for (BlockPos var10 : var7) {
         this.placeRoot(param0, param1, param2, var10, param5);
      }

      return true;
   }

   private boolean simulateRoots(
      LevelSimulatedReader param0, RandomSource param1, BlockPos param2, Direction param3, BlockPos param4, List<BlockPos> param5, int param6
   ) {
      int var8 = this.mangroveRootPlacement.maxRootLength();
      if (param6 != var8 && param5.size() <= var8) {
         for (BlockPos var11 : this.potentialRootPositions(param2, param3, param1, param4)) {
            if (this.canPlaceRoot(param0, var11)) {
               param5.add(var11);
               if (!this.simulateRoots(param0, param1, var11, param3, param4, param5, param6 + 1)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   protected List<BlockPos> potentialRootPositions(BlockPos param0, Direction param1, RandomSource param2, BlockPos param3) {
      BlockPos var5 = param0.below();
      BlockPos var6 = param0.relative(param1);
      int var7 = param0.distManhattan(param3);
      int var8 = this.mangroveRootPlacement.maxRootWidth();
      float var9 = this.mangroveRootPlacement.randomSkewChance();
      if (var7 > var8 - 3 && var7 <= var8) {
         return param2.nextFloat() < var9 ? List.of(var5, var6.below()) : List.of(var5);
      } else if (var7 > var8) {
         return List.of(var5);
      } else if (param2.nextFloat() < var9) {
         return List.of(var5);
      } else {
         return param2.nextBoolean() ? List.of(var6) : List.of(var5);
      }
   }

   @Override
   protected boolean canPlaceRoot(LevelSimulatedReader param0, BlockPos param1) {
      return super.canPlaceRoot(param0, param1) || param0.isStateAtPosition(param1, l_param0 -> l_param0.is(this.mangroveRootPlacement.canGrowThrough()));
   }

   @Override
   protected void placeRoot(
      LevelSimulatedReader param0, BiConsumer<BlockPos, BlockState> param1, RandomSource param2, BlockPos param3, TreeConfiguration param4
   ) {
      if (param0.isStateAtPosition(param3, l_param0 -> l_param0.is(this.mangroveRootPlacement.muddyRootsIn()))) {
         BlockState var6 = this.mangroveRootPlacement.muddyRootsProvider().getState(param2, param3);
         param1.accept(param3, this.getPotentiallyWaterloggedState(param0, param3, var6));
      } else {
         super.placeRoot(param0, param1, param2, param3, param4);
      }
   }

   @Override
   protected RootPlacerType<?> type() {
      return RootPlacerType.MANGROVE_ROOT_PLACER;
   }
}

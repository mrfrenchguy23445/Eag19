package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.UnderwaterMagmaConfiguration;
import net.minecraft.world.phys.AABB;

public class UnderwaterMagmaFeature extends Feature<UnderwaterMagmaConfiguration> {
   public UnderwaterMagmaFeature(Codec<UnderwaterMagmaConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<UnderwaterMagmaConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      BlockPos var3 = param0.origin();
      UnderwaterMagmaConfiguration var4 = param0.config();
      RandomSource var5 = param0.random();
      OptionalInt var6 = getFloorY(var2, var3, var4);
      if (!var6.isPresent()) {
         return false;
      }

      BlockPos var7 = var3.atY(var6.getAsInt());
      Vec3i var8 = new Vec3i(var4.placementRadiusAroundFloor, var4.placementRadiusAroundFloor, var4.placementRadiusAroundFloor);
      AABB var9 = new AABB(var7.subtract(var8), var7.offset(var8));
      return BlockPos.betweenClosedStream(var9)
            .filter(l_param2 -> var5.nextFloat() < var4.placementProbabilityPerValidPosition)
            .filter(l_param1 -> this.isValidPlacement(var2, l_param1))
            .mapToInt(l_param1 -> {
               var2.setBlock(l_param1, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
               return 1;
            })
            .sum()
         > 0;
   }

   private static OptionalInt getFloorY(WorldGenLevel param0, BlockPos param1, UnderwaterMagmaConfiguration param2) {
      Predicate<BlockState> var3 = l_param0 -> l_param0.is(Blocks.WATER);
      Predicate<BlockState> var4 = l_param0 -> !l_param0.is(Blocks.WATER);
      Optional<Column> var5 = Column.scan(param0, param1, param2.floorSearchRange, var3, var4);
      return var5.map(Column::getFloor).orElseGet(OptionalInt::empty);
   }

   private boolean isValidPlacement(WorldGenLevel param0, BlockPos param1) {
      if (!this.isWaterOrAir(param0, param1) && !this.isWaterOrAir(param0, param1.below())) {
         for (Direction var4 : Direction.Plane.HORIZONTAL) {
            if (this.isWaterOrAir(param0, param1.relative(var4))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isWaterOrAir(LevelAccessor param0, BlockPos param1) {
      BlockState var3 = param0.getBlockState(param1);
      return var3.is(Blocks.WATER) || var3.isAir();
   }
}

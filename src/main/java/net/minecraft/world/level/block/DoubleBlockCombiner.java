package net.minecraft.world.level.block;

import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class DoubleBlockCombiner {
   public static <S extends BlockEntity> DoubleBlockCombiner.NeighborCombineResult<S> combineWithNeigbour(
      BlockEntityType<S> param0,
      Function<BlockState, DoubleBlockCombiner.BlockType> param1,
      Function<BlockState, Direction> param2,
      DirectionProperty param3,
      BlockState param4,
      LevelAccessor param5,
      BlockPos param6,
      BiPredicate<LevelAccessor, BlockPos> param7
   ) {
      S var8 = param0.getBlockEntity(param5, param6);
      if (var8 == null) {
         return DoubleBlockCombiner.Combiner::acceptNone;
      }

      if (param7.test(param5, param6)) {
         return DoubleBlockCombiner.Combiner::acceptNone;
      }

      DoubleBlockCombiner.BlockType var9 = param1.apply(param4);
      boolean var10 = var9 == DoubleBlockCombiner.BlockType.SINGLE;
      boolean var11 = var9 == DoubleBlockCombiner.BlockType.FIRST;
      if (var10) {
         return new DoubleBlockCombiner.NeighborCombineResult.Single<>(var8);
      }

      BlockPos var12 = param6.relative(param2.apply(param4));
      BlockState var13 = param5.getBlockState(var12);
      if (var13.is(param4.getBlock())) {
         DoubleBlockCombiner.BlockType var14 = param1.apply(var13);
         if (var14 != DoubleBlockCombiner.BlockType.SINGLE && var9 != var14 && var13.getValue(param3) == param4.getValue(param3)) {
            if (param7.test(param5, var12)) {
               return DoubleBlockCombiner.Combiner::acceptNone;
            }

            S var15 = param0.getBlockEntity(param5, var12);
            if (var15 != null) {
               S var16 = var11 ? var8 : var15;
               S var17 = var11 ? var15 : var8;
               return new DoubleBlockCombiner.NeighborCombineResult.Double<>(var16, var17);
            }
         }
      }

      return new DoubleBlockCombiner.NeighborCombineResult.Single<>(var8);
   }

   public enum BlockType {
      SINGLE,
      FIRST,
      SECOND;
   }

   public interface Combiner<S, T> {
      T acceptDouble(S var1, S var2);

      T acceptSingle(S var1);

      T acceptNone();
   }

   public interface NeighborCombineResult<S> {
      <T> T apply(DoubleBlockCombiner.Combiner<? super S, T> var1);

      final class Double<S> implements DoubleBlockCombiner.NeighborCombineResult<S> {
         private final S first;
         private final S second;

         public Double(S param0, S param1) {
            this.first = param0;
            this.second = param1;
         }

         @Override
         public <T> T apply(DoubleBlockCombiner.Combiner<? super S, T> param0) {
            return param0.acceptDouble(this.first, this.second);
         }
      }

      final class Single<S> implements DoubleBlockCombiner.NeighborCombineResult<S> {
         private final S single;

         public Single(S param0) {
            this.single = param0;
         }

         @Override
         public <T> T apply(DoubleBlockCombiner.Combiner<? super S, T> param0) {
            return param0.acceptSingle(this.single);
         }
      }
   }
}

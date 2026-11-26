package net.minecraft.world.level.levelgen.placement;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public abstract class PlacementFilter extends PlacementModifier {
   @Override
   public final Stream<BlockPos> getPositions(PlacementContext param0, RandomSource param1, BlockPos param2) {
      return this.shouldPlace(param0, param1, param2) ? Stream.of(param2) : Stream.of();
   }

   @Override
   public final void appendPositions(PlacementContext param0, RandomSource param1, BlockPos param2, List<BlockPos> param3) {
      // Cooperative-port (TeaVM wasm-gc): no Stream.of per surviving candidate.
      if (this.shouldPlace(param0, param1, param2)) {
         param3.add(param2);
      }
   }

   protected abstract boolean shouldPlace(PlacementContext var1, RandomSource var2, BlockPos var3);
}

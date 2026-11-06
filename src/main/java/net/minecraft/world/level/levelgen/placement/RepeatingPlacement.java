package net.minecraft.world.level.levelgen.placement;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public abstract class RepeatingPlacement extends PlacementModifier {
   protected abstract int count(RandomSource var1, BlockPos var2);

   @Override
   public Stream<BlockPos> getPositions(PlacementContext param0, RandomSource param1, BlockPos param2) {
      return IntStream.range(0, this.count(param1, param2)).mapToObj(l_param1 -> param2);
   }

   @Override
   public void appendPositions(PlacementContext param0, RandomSource param1, BlockPos param2, List<BlockPos> param3) {
      // Cooperative-port (TeaVM wasm-gc): no IntStream per count call; matches the
      // stream API's repeated-same-instance semantics exactly.
      int var5 = this.count(param1, param2);
      for (int var6 = 0; var6 < var5; var6++) {
         param3.add(param2);
      }
   }
}

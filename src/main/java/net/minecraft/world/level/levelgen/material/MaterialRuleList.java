package net.minecraft.world.level.levelgen.material;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;

public record MaterialRuleList(List<NoiseChunk.BlockStateFiller> materialRuleList) implements NoiseChunk.BlockStateFiller {
   @Nullable
   @Override
   public BlockState calculate(DensityFunction.FunctionContext param0) {
      // Cooperative-port: indexed loop instead of forEach - this rule chain runs once
      // per terrain block (see NoiseChunk.getInterpolatedState), and an iterator
      // allocation per block is pure overhead in wasm-gc.
      List<NoiseChunk.BlockStateFiller> var3 = this.materialRuleList;
      for (int var4 = 0, var5 = var3.size(); var4 < var5; var4++) {
         BlockState var6 = var3.get(var4).calculate(param0);
         if (var6 != null) {
            return var6;
         }
      }

      return null;
   }
}

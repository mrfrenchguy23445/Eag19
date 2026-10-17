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
      for (NoiseChunk.BlockStateFiller var3 : this.materialRuleList) {
         BlockState var4 = var3.calculate(param0);
         if (var4 != null) {
            return var4;
         }
      }

      return null;
   }
}

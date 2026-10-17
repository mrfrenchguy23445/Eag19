package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;

public class CocoaDecorator extends TreeDecorator {
   public static final Codec<CocoaDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
      .fieldOf("probability")
      .xmap(CocoaDecorator::new, param0 -> param0.probability)
      .codec();
   private final float probability;

   public CocoaDecorator(float param0) {
      this.probability = param0;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.COCOA;
   }

   @Override
   public void place(TreeDecorator.Context param0) {
      RandomSource var2 = param0.random();
      if (!(var2.nextFloat() >= this.probability)) {
         List<BlockPos> var3 = param0.logs();
         int var4 = var3.get(0).getY();
         var3.stream().filter(l_param1 -> l_param1.getY() - var4 <= 2).forEach(l_param2 -> {
            for (Direction l_var4 : Direction.Plane.HORIZONTAL) {
               if (var2.nextFloat() <= 0.25F) {
                  Direction l_var5 = l_var4.getOpposite();
                  BlockPos l_var6 = l_param2.offset(l_var5.getStepX(), 0, l_var5.getStepZ());
                  if (param0.isAir(l_var6)) {
                     param0.setBlock(l_var6, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE, var2.nextInt(3)).setValue(CocoaBlock.FACING, l_var4));
                  }
               }
            }
         });
      }
   }
}

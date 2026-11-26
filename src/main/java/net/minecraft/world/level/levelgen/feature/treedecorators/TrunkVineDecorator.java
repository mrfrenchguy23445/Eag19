package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;

public class TrunkVineDecorator extends TreeDecorator {
   public static final Codec<TrunkVineDecorator> CODEC = Codec.unit(() -> TrunkVineDecorator.INSTANCE);
   public static final TrunkVineDecorator INSTANCE = new TrunkVineDecorator();

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.TRUNK_VINE;
   }

   @Override
   public void place(TreeDecorator.Context param0) {
      RandomSource var2 = param0.random();
      param0.logs().forEach(l_param2 -> {
         if (var2.nextInt(3) > 0) {
            BlockPos l_var3 = l_param2.west();
            if (param0.isAir(l_var3)) {
               param0.placeVine(l_var3, VineBlock.EAST);
            }
         }

         if (var2.nextInt(3) > 0) {
            BlockPos l_var3 = l_param2.east();
            if (param0.isAir(l_var3)) {
               param0.placeVine(l_var3, VineBlock.WEST);
            }
         }

         if (var2.nextInt(3) > 0) {
            BlockPos l_var3 = l_param2.north();
            if (param0.isAir(l_var3)) {
               param0.placeVine(l_var3, VineBlock.SOUTH);
            }
         }

         if (var2.nextInt(3) > 0) {
            BlockPos l_var3 = l_param2.south();
            if (param0.isAir(l_var3)) {
               param0.placeVine(l_var3, VineBlock.NORTH);
            }
         }
      });
   }
}

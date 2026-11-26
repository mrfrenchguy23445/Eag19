package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class LeaveVineDecorator extends TreeDecorator {
   public static final Codec<LeaveVineDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
      .fieldOf("probability")
      .xmap(LeaveVineDecorator::new, param0 -> param0.probability)
      .codec();
   private final float probability;

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.LEAVE_VINE;
   }

   public LeaveVineDecorator(float param0) {
      this.probability = param0;
   }

   @Override
   public void place(TreeDecorator.Context param0) {
      RandomSource var2 = param0.random();
      param0.leaves().forEach(l_param2 -> {
         if (var2.nextFloat() < this.probability) {
            BlockPos l_var4 = l_param2.west();
            if (param0.isAir(l_var4)) {
               addHangingVine(l_var4, VineBlock.EAST, param0);
            }
         }

         if (var2.nextFloat() < this.probability) {
            BlockPos l_var4 = l_param2.east();
            if (param0.isAir(l_var4)) {
               addHangingVine(l_var4, VineBlock.WEST, param0);
            }
         }

         if (var2.nextFloat() < this.probability) {
            BlockPos l_var4 = l_param2.north();
            if (param0.isAir(l_var4)) {
               addHangingVine(l_var4, VineBlock.SOUTH, param0);
            }
         }

         if (var2.nextFloat() < this.probability) {
            BlockPos l_var4 = l_param2.south();
            if (param0.isAir(l_var4)) {
               addHangingVine(l_var4, VineBlock.NORTH, param0);
            }
         }
      });
   }

   private static void addHangingVine(BlockPos param0, BooleanProperty param1, TreeDecorator.Context param2) {
      param2.placeVine(param0, param1);
      int var3 = 4;

      for (BlockPos var4 = param0.below(); param2.isAir(var4) && var3 > 0; var3--) {
         param2.placeVine(var4, param1);
         var4 = var4.below();
      }
   }
}

package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AlterGroundDecorator extends TreeDecorator {
   public static final Codec<AlterGroundDecorator> CODEC = BlockStateProvider.CODEC
      .fieldOf("provider")
      .xmap(AlterGroundDecorator::new, param0 -> param0.provider)
      .codec();
   private final BlockStateProvider provider;

   public AlterGroundDecorator(BlockStateProvider param0) {
      this.provider = param0;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.ALTER_GROUND;
   }

   @Override
   public void place(TreeDecorator.Context param0) {
      List<BlockPos> var2 = Lists.newArrayList();
      List<BlockPos> var3 = param0.roots();
      List<BlockPos> var4 = param0.logs();
      if (var3.isEmpty()) {
         var2.addAll(var4);
      } else if (!var4.isEmpty() && var3.get(0).getY() == var4.get(0).getY()) {
         var2.addAll(var4);
         var2.addAll(var3);
      } else {
         var2.addAll(var3);
      }

      if (!var2.isEmpty()) {
         int var5 = var2.get(0).getY();
         var2.stream().filter(l_param1 -> l_param1.getY() == var5).forEach(l_param1 -> {
            this.placeCircle(param0, l_param1.west().north());
            this.placeCircle(param0, l_param1.east(2).north());
            this.placeCircle(param0, l_param1.west().south(2));
            this.placeCircle(param0, l_param1.east(2).south(2));

            for (int l_var3 = 0; l_var3 < 5; l_var3++) {
               int l_var4 = param0.random().nextInt(64);
               int l_var5 = l_var4 % 8;
               int l_var6 = l_var4 / 8;
               if (l_var5 == 0 || l_var5 == 7 || l_var6 == 0 || l_var6 == 7) {
                  this.placeCircle(param0, l_param1.offset(-3 + l_var5, 0, -3 + l_var6));
               }
            }
         });
      }
   }

   private void placeCircle(TreeDecorator.Context param0, BlockPos param1) {
      for (int var3 = -2; var3 <= 2; var3++) {
         for (int var4 = -2; var4 <= 2; var4++) {
            if (Math.abs(var3) != 2 || Math.abs(var4) != 2) {
               this.placeBlockAt(param0, param1.offset(var3, 0, var4));
            }
         }
      }
   }

   private void placeBlockAt(TreeDecorator.Context param0, BlockPos param1) {
      for (int var3 = 2; var3 >= -3; var3--) {
         BlockPos var4 = param1.above(var3);
         if (Feature.isGrassOrDirt(param0.level(), var4)) {
            param0.setBlock(var4, this.provider.getState(param0.random(), param1));
            break;
         }

         if (!param0.isAir(var4) && var3 < 0) {
            break;
         }
      }
   }
}

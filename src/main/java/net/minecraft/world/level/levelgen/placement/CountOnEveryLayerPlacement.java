package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

@Deprecated
public class CountOnEveryLayerPlacement extends PlacementModifier {
   public static final Codec<CountOnEveryLayerPlacement> CODEC = IntProvider.codec(0, 256)
      .fieldOf("count")
      .xmap(CountOnEveryLayerPlacement::new, param0 -> param0.count)
      .codec();
   private final IntProvider count;

   private CountOnEveryLayerPlacement(IntProvider param0) {
      this.count = param0;
   }

   public static CountOnEveryLayerPlacement of(IntProvider param0) {
      return new CountOnEveryLayerPlacement(param0);
   }

   public static CountOnEveryLayerPlacement of(int param0) {
      return of(ConstantInt.of(param0));
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext param0, RandomSource param1, BlockPos param2) {
      Builder<BlockPos> var4 = Stream.builder();
      int var6 = 0;

      boolean var5;
      do {
         var5 = false;

         for (int var7 = 0; var7 < this.count.sample(param1); var7++) {
            int var8 = param1.nextInt(16) + param2.getX();
            int var9 = param1.nextInt(16) + param2.getZ();
            int var10 = param0.getHeight(Heightmap.Types.MOTION_BLOCKING, var8, var9);
            int var11 = findOnGroundYPosition(param0, var8, var10, var9, var6);
            if (var11 != Integer.MAX_VALUE) {
               var4.add(new BlockPos(var8, var11, var9));
               var5 = true;
            }
         }

         var6++;
      } while (var5);

      return var4.build();
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.COUNT_ON_EVERY_LAYER;
   }

   private static int findOnGroundYPosition(PlacementContext param0, int param1, int param2, int param3, int param4) {
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos(param1, param2, param3);
      int var6 = 0;
      BlockState var7 = param0.getBlockState(var5);

      for (int var8 = param2; var8 >= param0.getMinBuildHeight() + 1; var8--) {
         var5.setY(var8 - 1);
         BlockState var9 = param0.getBlockState(var5);
         if (!isEmpty(var9) && isEmpty(var7) && !var9.is(Blocks.BEDROCK)) {
            if (var6 == param4) {
               return var5.getY() + 1;
            }

            var6++;
         }

         var7 = var9;
      }

      return Integer.MAX_VALUE;
   }

   private static boolean isEmpty(BlockState param0) {
      return param0.isAir() || param0.is(Blocks.WATER) || param0.is(Blocks.LAVA);
   }
}

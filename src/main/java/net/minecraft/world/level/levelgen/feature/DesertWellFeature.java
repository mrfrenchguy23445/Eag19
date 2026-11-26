package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.loot.packs.UpdateOneTwentyBuiltInLootTables;
import net.minecraft.util.RandomSource;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.apache.commons.lang3.mutable.MutableInt;

public class DesertWellFeature extends Feature<NoneFeatureConfiguration> {
   private static final BlockStatePredicate IS_SAND = BlockStatePredicate.forBlock(Blocks.SAND);
   private final BlockState sandSlab = Blocks.SANDSTONE_SLAB.defaultBlockState();
   private final BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
   private final BlockState water = Blocks.WATER.defaultBlockState();

   public DesertWellFeature(Codec<NoneFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      BlockPos var3 = param0.origin();
      var3 = var3.above();

      while (var2.isEmptyBlock(var3) && var3.getY() > var2.getMinBuildHeight() + 2) {
         var3 = var3.below();
      }

      if (!IS_SAND.test(var2.getBlockState(var3))) {
         return false;
      }

      for (int var4 = -2; var4 <= 2; var4++) {
         for (int var5 = -2; var5 <= 2; var5++) {
            if (var2.isEmptyBlock(var3.offset(var4, -1, var5)) && var2.isEmptyBlock(var3.offset(var4, -2, var5))) {
               return false;
            }
         }
      }

      for (int var4 = -1; var4 <= 0; var4++) {
         for (int var5 = -2; var5 <= 2; var5++) {
            for (int var6 = -2; var6 <= 2; var6++) {
               var2.setBlock(var3.offset(var5, var4, var6), this.sandstone, 2);
            }
         }
      }

      if (var2.enabledFeatures().contains(FeatureFlags.UPDATE_1_20)) {
         placeSandFloor(var2, var3, param0.random());
      }

      var2.setBlock(var3, this.water, 2);

      for (Direction var5 : Direction.Plane.HORIZONTAL) {
         var2.setBlock(var3.relative(var5), this.water, 2);
      }

      for (int var4 = -2; var4 <= 2; var4++) {
         for (int var5 = -2; var5 <= 2; var5++) {
            if (var4 == -2 || var4 == 2 || var5 == -2 || var5 == 2) {
               var2.setBlock(var3.offset(var4, 1, var5), this.sandstone, 2);
            }
         }
      }

      var2.setBlock(var3.offset(2, 1, 0), this.sandSlab, 2);
      var2.setBlock(var3.offset(-2, 1, 0), this.sandSlab, 2);
      var2.setBlock(var3.offset(0, 1, 2), this.sandSlab, 2);
      var2.setBlock(var3.offset(0, 1, -2), this.sandSlab, 2);

      for (int var4 = -1; var4 <= 1; var4++) {
         for (int var5 = -1; var5 <= 1; var5++) {
            if (var4 == 0 && var5 == 0) {
               var2.setBlock(var3.offset(var4, 4, var5), this.sandstone, 2);
            } else {
               var2.setBlock(var3.offset(var4, 4, var5), this.sandSlab, 2);
            }
         }
      }

      for (int var4 = 1; var4 <= 3; var4++) {
         var2.setBlock(var3.offset(-1, var4, -1), this.sandstone, 2);
         var2.setBlock(var3.offset(-1, var4, 1), this.sandstone, 2);
         var2.setBlock(var3.offset(1, var4, -1), this.sandstone, 2);
         var2.setBlock(var3.offset(1, var4, 1), this.sandstone, 2);
      }

      return true;
   }

   private static void placeSandFloor(WorldGenLevel param0, BlockPos param1, RandomSource param2) {
      BlockPos var3 = param1.offset(0, -1, 0);
      ObjectArrayList<BlockPos> var4 = Util.make(new ObjectArrayList<>(), l_param1 -> {
         l_param1.add(var3.east());
         l_param1.add(var3.south());
         l_param1.add(var3.west());
         l_param1.add(var3.north());
      });
      Util.shuffle(var4, param2);
      MutableInt var5 = new MutableInt(param2.nextInt(2, 4));
      Stream.concat(Stream.of(var3), var4.stream())
         .forEach(
            l_param2 -> {
               if (var5.getAndDecrement() > 0) {
                  param0.setBlock(l_param2, Blocks.SUSPICIOUS_SAND.defaultBlockState(), 3);
                  param0.getBlockEntity(l_param2, BlockEntityType.SUSPICIOUS_SAND)
                     .ifPresent(l2_param1 -> l2_param1.setLootTable(UpdateOneTwentyBuiltInLootTables.DESERT_WELL_ARCHAEOLOGY, l_param2.asLong()));
               } else {
                  param0.setBlock(l_param2, Blocks.SAND.defaultBlockState(), 3);
               }
            }
         );
   }
}

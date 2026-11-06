package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class TreeFeature extends Feature<TreeConfiguration> {
   private static final int BLOCK_UPDATE_FLAGS = 19;

   public TreeFeature(Codec<TreeConfiguration> param0) {
      super(param0);
   }

   private static boolean isVine(LevelSimulatedReader param0, BlockPos param1) {
      return param0.isStateAtPosition(param1, l_param0 -> l_param0.is(Blocks.VINE));
   }

   public static boolean isBlockWater(LevelSimulatedReader param0, BlockPos param1) {
      return param0.isStateAtPosition(param1, l_param0 -> l_param0.is(Blocks.WATER));
   }

   public static boolean isAirOrLeaves(LevelSimulatedReader param0, BlockPos param1) {
      return param0.isStateAtPosition(param1, l_param0 -> l_param0.isAir() || l_param0.is(BlockTags.LEAVES));
   }

   private static boolean isReplaceablePlant(LevelSimulatedReader param0, BlockPos param1) {
      return param0.isStateAtPosition(param1, l_param0 -> {
         Material l_var1 = l_param0.getMaterial();
         return l_var1 == Material.REPLACEABLE_PLANT || l_var1 == Material.REPLACEABLE_WATER_PLANT || l_var1 == Material.REPLACEABLE_FIREPROOF_PLANT;
      });
   }

   private static void setBlockKnownShape(LevelWriter param0, BlockPos param1, BlockState param2) {
      param0.setBlock(param1, param2, 19);
   }

   public static boolean validTreePos(LevelSimulatedReader param0, BlockPos param1) {
      return isAirOrLeaves(param0, param1) || isReplaceablePlant(param0, param1) || isBlockWater(param0, param1);
   }

   private boolean doPlace(
      WorldGenLevel param0,
      RandomSource param1,
      BlockPos param2,
      BiConsumer<BlockPos, BlockState> param3,
      BiConsumer<BlockPos, BlockState> param4,
      FoliagePlacer.FoliageSetter param5,
      TreeConfiguration param6
   ) {
      int var8 = param6.trunkPlacer.getTreeHeight(param1);
      int var9 = param6.foliagePlacer.foliageHeight(param1, var8, param6);
      int var10 = var8 - var9;
      int var11 = param6.foliagePlacer.foliageRadius(param1, var10);
      BlockPos var12 = param6.rootPlacer.<BlockPos>map(l_param2 -> l_param2.getTrunkOrigin(param2, param1)).orElse(param2);
      int var13 = Math.min(param2.getY(), var12.getY());
      int var14 = Math.max(param2.getY(), var12.getY()) + var8 + 1;
      if (var13 >= param0.getMinBuildHeight() + 1 && var14 <= param0.getMaxBuildHeight()) {
         OptionalInt var15 = param6.minimumSize.minClippedHeight();
         int var16 = this.getMaxFreeTreeHeight(param0, var8, var12, param6);
         if (var16 >= var8 || !var15.isEmpty() && var16 >= var15.getAsInt()) {
            if (param6.rootPlacer.isPresent() && !param6.rootPlacer.get().placeRoots(param0, param3, param1, param2, var12, param6)) {
               return false;
            }

            List<FoliagePlacer.FoliageAttachment> var17 = param6.trunkPlacer.placeTrunk(param0, param4, param1, var16, var12, param6);
            var17.forEach(l_param7 -> param6.foliagePlacer.createFoliage(param0, param5, param1, param6, var16, l_param7, var9, var11));
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int getMaxFreeTreeHeight(LevelSimulatedReader param0, int param1, BlockPos param2, TreeConfiguration param3) {
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos();

      for (int var6 = 0; var6 <= param1 + 1; var6++) {
         int var7 = param3.minimumSize.getSizeAtHeight(param1, var6);

         for (int var8 = -var7; var8 <= var7; var8++) {
            for (int var9 = -var7; var9 <= var7; var9++) {
               var5.setWithOffset(param2, var8, var6, var9);
               if (!param3.trunkPlacer.isFree(param0, var5) || !param3.ignoreVines && isVine(param0, var5)) {
                  return var6 - 2;
               }
            }
         }
      }

      return param1;
   }

   @Override
   protected void setBlock(LevelWriter param0, BlockPos param1, BlockState param2) {
      setBlockKnownShape(param0, param1, param2);
   }

   @Override
   public final boolean place(FeaturePlaceContext<TreeConfiguration> param0) {
      final WorldGenLevel var2 = param0.level();
      RandomSource var3 = param0.random();
      BlockPos var4 = param0.origin();
      TreeConfiguration var5 = param0.config();
      Set<BlockPos> var6 = Sets.newHashSet();
      Set<BlockPos> var7 = Sets.newHashSet();
      final Set<BlockPos> var8 = Sets.newHashSet();
      Set<BlockPos> var9 = Sets.newHashSet();
      BiConsumer<BlockPos, BlockState> var10 = (l_param2, l_param3) -> {
         var6.add(l_param2.immutable());
         var2.setBlock(l_param2, l_param3, 19);
      };
      BiConsumer<BlockPos, BlockState> var11 = (l_param2, l_param3) -> {
         var7.add(l_param2.immutable());
         var2.setBlock(l_param2, l_param3, 19);
      };
      FoliagePlacer.FoliageSetter var12 = new FoliagePlacer.FoliageSetter() {
         @Override
         public void set(BlockPos l_param0, BlockState l_param1) {
            var8.add(l_param0.immutable());
            var2.setBlock(l_param0, l_param1, 19);
         }

         @Override
         public boolean isSet(BlockPos l_param0) {
            return var8.contains(l_param0);
         }
      };
      BiConsumer<BlockPos, BlockState> var13 = (l_param2, l_param3) -> {
         var9.add(l_param2.immutable());
         var2.setBlock(l_param2, l_param3, 19);
      };
      boolean var14 = this.doPlace(var2, var3, var4, var10, var11, var12, var5);
      if (var14 && (!var7.isEmpty() || !var8.isEmpty())) {
         if (!var5.decorators.isEmpty()) {
            TreeDecorator.Context var15 = new TreeDecorator.Context(var2, var13, var3, var7, var8, var6);
            var5.decorators.forEach(l_param1 -> l_param1.place(var15));
         }

         return BoundingBox.encapsulatingPositions(Iterables.concat(var6, var7, var8, var9)).map(l_param4 -> {
            DiscreteVoxelShape l_var5 = updateLeaves(var2, l_param4, var7, var9, var6);
            StructureTemplate.updateShapeAtEdge(var2, 3, l_var5, l_param4.minX(), l_param4.minY(), l_param4.minZ());
            return true;
         }).orElse(false);
      } else {
         return false;
      }
   }

   private static DiscreteVoxelShape updateLeaves(LevelAccessor param0, BoundingBox param1, Set<BlockPos> param2, Set<BlockPos> param3, Set<BlockPos> param4) {
      List<Set<BlockPos>> var5 = Lists.newArrayList();
      DiscreteVoxelShape var6 = new BitSetDiscreteVoxelShape(param1.getXSpan(), param1.getYSpan(), param1.getZSpan());
      int var7 = 6;

      for (int var8 = 0; var8 < 6; var8++) {
         var5.add(Sets.newHashSet());
      }

      BlockPos.MutableBlockPos var8 = new BlockPos.MutableBlockPos();

      // Cooperative-port: Sets.union + double ArrayList copy per tree removed; the
      // shape fill below is idempotent, so visiting param3 then param4 (with possible
      // repeats) is value-identical and allocation-free.
      for (BlockPos var10 : param3) {
         if (param1.isInside(var10)) {
            var6.fill(var10.getX() - param1.minX(), var10.getY() - param1.minY(), var10.getZ() - param1.minZ());
         }
      }

      for (BlockPos var10 : param4) {
         if (param1.isInside(var10)) {
            var6.fill(var10.getX() - param1.minX(), var10.getY() - param1.minY(), var10.getZ() - param1.minZ());
         }
      }

      // Cooperative-port: Lists.newArrayList(param2) defensive copy removed - param2 is
      // not mutated in this loop.
      for (BlockPos var10 : param2) {
         if (param1.isInside(var10)) {
            var6.fill(var10.getX() - param1.minX(), var10.getY() - param1.minY(), var10.getZ() - param1.minZ());
         }

         for (Direction var14 : Direction.values()) {
            var8.setWithOffset(var10, var14);
            if (!param2.contains(var8)) {
               BlockState var15 = param0.getBlockState(var8);
               if (var15.hasProperty(BlockStateProperties.DISTANCE)) {
                  var5.get(0).add(var8.immutable());
                  setBlockKnownShape(param0, var8, var15.setValue(BlockStateProperties.DISTANCE, 1));
                  if (param1.isInside(var8)) {
                     var6.fill(var8.getX() - param1.minX(), var8.getY() - param1.minY(), var8.getZ() - param1.minZ());
                  }
               }
            }
         }
      }

      for (int var9 = 1; var9 < 6; var9++) {
         Set<BlockPos> var10 = var5.get(var9 - 1);
         Set<BlockPos> var11 = var5.get(var9);

         for (BlockPos var13 : var10) {
            if (param1.isInside(var13)) {
               var6.fill(var13.getX() - param1.minX(), var13.getY() - param1.minY(), var13.getZ() - param1.minZ());
            }

            for (Direction var17 : Direction.values()) {
               var8.setWithOffset(var13, var17);
               if (!var10.contains(var8) && !var11.contains(var8)) {
                  BlockState var18 = param0.getBlockState(var8);
                  if (var18.hasProperty(BlockStateProperties.DISTANCE)) {
                     int var19 = var18.getValue(BlockStateProperties.DISTANCE);
                     if (var19 > var9 + 1) {
                        BlockState var20 = var18.setValue(BlockStateProperties.DISTANCE, var9 + 1);
                        setBlockKnownShape(param0, var8, var20);
                        if (param1.isInside(var8)) {
                           var6.fill(var8.getX() - param1.minX(), var8.getY() - param1.minY(), var8.getZ() - param1.minZ());
                        }

                        var11.add(var8.immutable());
                     }
                  }
               }
            }
         }
      }

      return var6;
   }
}

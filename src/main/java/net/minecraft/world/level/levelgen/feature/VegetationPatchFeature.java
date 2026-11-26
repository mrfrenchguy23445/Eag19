package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class VegetationPatchFeature extends Feature<VegetationPatchConfiguration> {
   public VegetationPatchFeature(Codec<VegetationPatchConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<VegetationPatchConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      VegetationPatchConfiguration var3 = param0.config();
      RandomSource var4 = param0.random();
      BlockPos var5 = param0.origin();
      Predicate<BlockState> var6 = l_param1 -> l_param1.is(var3.replaceable);
      int var7 = var3.xzRadius.sample(var4) + 1;
      int var8 = var3.xzRadius.sample(var4) + 1;
      Set<BlockPos> var9 = this.placeGroundPatch(var2, var3, var4, var5, var6, var7, var8);
      this.distributeVegetation(param0, var2, var3, var4, var9, var7, var8);
      return !var9.isEmpty();
   }

   protected Set<BlockPos> placeGroundPatch(
      WorldGenLevel param0, VegetationPatchConfiguration param1, RandomSource param2, BlockPos param3, Predicate<BlockState> param4, int param5, int param6
   ) {
      BlockPos.MutableBlockPos var8 = param3.mutable();
      BlockPos.MutableBlockPos var9 = var8.mutable();
      Direction var10 = param1.surface.getDirection();
      Direction var11 = var10.getOpposite();
      Set<BlockPos> var12 = new HashSet<>();

      for (int var13 = -param5; var13 <= param5; var13++) {
         boolean var14 = var13 == -param5 || var13 == param5;

         for (int var15 = -param6; var15 <= param6; var15++) {
            boolean var16 = var15 == -param6 || var15 == param6;
            boolean var17 = var14 || var16;
            boolean var18 = var14 && var16;
            boolean var19 = var17 && !var18;
            if (!var18 && (!var19 || param1.extraEdgeColumnChance != 0.0F && !(param2.nextFloat() > param1.extraEdgeColumnChance))) {
               var8.setWithOffset(param3, var13, 0, var15);

               for (int var20 = 0; param0.isStateAtPosition(var8, BlockBehaviour.BlockStateBase::isAir) && var20 < param1.verticalRange; var20++) {
                  var8.move(var10);
               }

               for (int var25 = 0; param0.isStateAtPosition(var8, l_param0 -> !l_param0.isAir()) && var25 < param1.verticalRange; var25++) {
                  var8.move(var11);
               }

               var9.setWithOffset(var8, param1.surface.getDirection());
               BlockState var21 = param0.getBlockState(var9);
               if (param0.isEmptyBlock(var8) && var21.isFaceSturdy(param0, var9, param1.surface.getDirection().getOpposite())) {
                  int var22 = param1.depth.sample(param2)
                     + (param1.extraBottomBlockChance > 0.0F && param2.nextFloat() < param1.extraBottomBlockChance ? 1 : 0);
                  BlockPos var23 = var9.immutable();
                  boolean var24 = this.placeGround(param0, param1, param4, param2, var9, var22);
                  if (var24) {
                     var12.add(var23);
                  }
               }
            }
         }
      }

      return var12;
   }

   protected void distributeVegetation(
      FeaturePlaceContext<VegetationPatchConfiguration> param0,
      WorldGenLevel param1,
      VegetationPatchConfiguration param2,
      RandomSource param3,
      Set<BlockPos> param4,
      int param5,
      int param6
   ) {
      for (BlockPos var9 : param4) {
         if (param2.vegetationChance > 0.0F && param3.nextFloat() < param2.vegetationChance) {
            this.placeVegetation(param1, param2, param0.chunkGenerator(), param3, var9);
         }
      }
   }

   protected boolean placeVegetation(WorldGenLevel param0, VegetationPatchConfiguration param1, ChunkGenerator param2, RandomSource param3, BlockPos param4) {
      return param1.vegetationFeature.value().place(param0, param2, param3, param4.relative(param1.surface.getDirection().getOpposite()));
   }

   protected boolean placeGround(
      WorldGenLevel param0, VegetationPatchConfiguration param1, Predicate<BlockState> param2, RandomSource param3, BlockPos.MutableBlockPos param4, int param5
   ) {
      for (int var7 = 0; var7 < param5; var7++) {
         BlockState var8 = param1.groundState.getState(param3, param4);
         BlockState var9 = param0.getBlockState(param4);
         if (!var8.is(var9.getBlock())) {
            if (!param2.test(var9)) {
               return var7 != 0;
            }

            param0.setBlock(param4, var8, 2);
            param4.move(param1.surface.getDirection());
         }
      }

      return true;
   }
}

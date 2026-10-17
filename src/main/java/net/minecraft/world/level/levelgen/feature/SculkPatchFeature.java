package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkBehaviour;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.SculkPatchConfiguration;

public class SculkPatchFeature extends Feature<SculkPatchConfiguration> {
   public SculkPatchFeature(Codec<SculkPatchConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<SculkPatchConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      BlockPos var3 = param0.origin();
      if (!this.canSpreadFrom(var2, var3)) {
         return false;
      }

      SculkPatchConfiguration var4 = param0.config();
      RandomSource var5 = param0.random();
      SculkSpreader var6 = SculkSpreader.createWorldGenSpreader();
      int var7 = var4.spreadRounds() + var4.growthRounds();

      for (int var8 = 0; var8 < var7; var8++) {
         for (int var9 = 0; var9 < var4.chargeCount(); var9++) {
            var6.addCursors(var3, var4.amountPerCharge());
         }

         boolean var9 = var8 < var4.spreadRounds();

         for (int var10 = 0; var10 < var4.spreadAttempts(); var10++) {
            var6.updateCursors(var2, var3, var5, var9);
         }

         var6.clear();
      }

      BlockPos var8 = var3.below();
      if (var5.nextFloat() <= var4.catalystChance() && var2.getBlockState(var8).isCollisionShapeFullBlock(var2, var8)) {
         var2.setBlock(var3, Blocks.SCULK_CATALYST.defaultBlockState(), 3);
      }

      int var9 = var4.extraRareGrowths().sample(var5);

      for (int var10 = 0; var10 < var9; var10++) {
         BlockPos var11 = var3.offset(var5.nextInt(5) - 2, 0, var5.nextInt(5) - 2);
         if (var2.getBlockState(var11).isAir() && var2.getBlockState(var11.below()).isFaceSturdy(var2, var11.below(), Direction.UP)) {
            var2.setBlock(var11, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true), 3);
         }
      }

      return true;
   }

   private boolean canSpreadFrom(LevelAccessor param0, BlockPos param1) {
      BlockState var3 = param0.getBlockState(param1);
      if (var3.getBlock() instanceof SculkBehaviour) {
         return true;
      } else {
         return !var3.isAir() && (!var3.is(Blocks.WATER) || !var3.getFluidState().isSource())
            ? false
            : Direction.stream().map(param1::relative).anyMatch(l_param1 -> param0.getBlockState(l_param1).isCollisionShapeFullBlock(param0, l_param1));
      }
   }
}

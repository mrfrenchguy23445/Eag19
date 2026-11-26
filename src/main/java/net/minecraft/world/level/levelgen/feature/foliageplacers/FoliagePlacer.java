package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.datafixers.Products.P2;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.material.Fluids;

public abstract class FoliagePlacer {
   public static final Codec<FoliagePlacer> CODEC = BuiltInRegistries.FOLIAGE_PLACER_TYPE.byNameCodec().dispatch(FoliagePlacer::type, FoliagePlacerType::codec);
   protected final IntProvider radius;
   protected final IntProvider offset;

   protected static <P extends FoliagePlacer> P2<Mu<P>, IntProvider, IntProvider> foliagePlacerParts(Instance<P> param0) {
      return param0.group(
         IntProvider.codec(0, 16).fieldOf("radius").forGetter(l_param0 -> l_param0.radius),
         IntProvider.codec(0, 16).fieldOf("offset").forGetter(l_param0 -> l_param0.offset)
      );
   }

   public FoliagePlacer(IntProvider param0, IntProvider param1) {
      this.radius = param0;
      this.offset = param1;
   }

   protected abstract FoliagePlacerType<?> type();

   public void createFoliage(
      LevelSimulatedReader param0,
      FoliagePlacer.FoliageSetter param1,
      RandomSource param2,
      TreeConfiguration param3,
      int param4,
      FoliagePlacer.FoliageAttachment param5,
      int param6,
      int param7
   ) {
      this.createFoliage(param0, param1, param2, param3, param4, param5, param6, param7, this.offset(param2));
   }

   protected abstract void createFoliage(
      LevelSimulatedReader var1,
      FoliagePlacer.FoliageSetter var2,
      RandomSource var3,
      TreeConfiguration var4,
      int var5,
      FoliagePlacer.FoliageAttachment var6,
      int var7,
      int var8,
      int var9
   );

   public abstract int foliageHeight(RandomSource var1, int var2, TreeConfiguration var3);

   public int foliageRadius(RandomSource param0, int param1) {
      return this.radius.sample(param0);
   }

   private int offset(RandomSource param0) {
      return this.offset.sample(param0);
   }

   protected abstract boolean shouldSkipLocation(RandomSource var1, int var2, int var3, int var4, int var5, boolean var6);

   protected boolean shouldSkipLocationSigned(RandomSource param0, int param1, int param2, int param3, int param4, boolean param5) {
      int var7;
      int var8;
      if (param5) {
         var7 = Math.min(Math.abs(param1), Math.abs(param1 - 1));
         var8 = Math.min(Math.abs(param3), Math.abs(param3 - 1));
      } else {
         var7 = Math.abs(param1);
         var8 = Math.abs(param3);
      }

      return this.shouldSkipLocation(param0, var7, param2, var8, param4, param5);
   }

   protected void placeLeavesRow(
      LevelSimulatedReader param0,
      FoliagePlacer.FoliageSetter param1,
      RandomSource param2,
      TreeConfiguration param3,
      BlockPos param4,
      int param5,
      int param6,
      boolean param7
   ) {
      int var9 = param7 ? 1 : 0;
      BlockPos.MutableBlockPos var10 = new BlockPos.MutableBlockPos();

      for (int var11 = -param5; var11 <= param5 + var9; var11++) {
         for (int var12 = -param5; var12 <= param5 + var9; var12++) {
            if (!this.shouldSkipLocationSigned(param2, var11, param6, var12, param5, param7)) {
               var10.setWithOffset(param4, var11, param6, var12);
               tryPlaceLeaf(param0, param1, param2, param3, var10);
            }
         }
      }
   }

   protected final void placeLeavesRowWithHangingLeavesBelow(
      LevelSimulatedReader param0,
      FoliagePlacer.FoliageSetter param1,
      RandomSource param2,
      TreeConfiguration param3,
      BlockPos param4,
      int param5,
      int param6,
      boolean param7,
      float param8,
      float param9
   ) {
      this.placeLeavesRow(param0, param1, param2, param3, param4, param5, param6, param7);
      int var11 = param7 ? 1 : 0;
      BlockPos.MutableBlockPos var12 = new BlockPos.MutableBlockPos();

      for (Direction var14 : Direction.Plane.HORIZONTAL) {
         Direction var15 = var14.getClockWise();
         int var16 = var15.getAxisDirection() == Direction.AxisDirection.POSITIVE ? param5 + var11 : param5;
         var12.setWithOffset(param4, 0, param6 - 1, 0).move(var15, var16).move(var14, -param5);
         int var17 = -param5;

         while (var17 < param5 + var11) {
            boolean var18 = param1.isSet(var12.move(Direction.UP));
            var12.move(Direction.DOWN);
            if (var18 && !(param2.nextFloat() > param8) && tryPlaceLeaf(param0, param1, param2, param3, var12) && !(param2.nextFloat() > param9)) {
               tryPlaceLeaf(param0, param1, param2, param3, var12.move(Direction.DOWN));
               var12.move(Direction.UP);
            }

            var17++;
            var12.move(var14);
         }
      }
   }

   protected static boolean tryPlaceLeaf(
      LevelSimulatedReader param0, FoliagePlacer.FoliageSetter param1, RandomSource param2, TreeConfiguration param3, BlockPos param4
   ) {
      if (!TreeFeature.validTreePos(param0, param4)) {
         return false;
      }

      BlockState var5 = param3.foliageProvider.getState(param2, param4);
      if (var5.hasProperty(BlockStateProperties.WATERLOGGED)) {
         var5 = var5.setValue(BlockStateProperties.WATERLOGGED, param0.isFluidAtPosition(param4, l_param0 -> l_param0.isSourceOfType(Fluids.WATER)));
      }

      param1.set(param4, var5);
      return true;
   }

   public static final class FoliageAttachment {
      private final BlockPos pos;
      private final int radiusOffset;
      private final boolean doubleTrunk;

      public FoliageAttachment(BlockPos param0, int param1, boolean param2) {
         this.pos = param0;
         this.radiusOffset = param1;
         this.doubleTrunk = param2;
      }

      public BlockPos pos() {
         return this.pos;
      }

      public int radiusOffset() {
         return this.radiusOffset;
      }

      public boolean doubleTrunk() {
         return this.doubleTrunk;
      }
   }

   public interface FoliageSetter {
      void set(BlockPos var1, BlockState var2);

      boolean isSet(BlockPos var1);
   }
}

package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.LargeDripstoneConfiguration;
import net.minecraft.world.phys.Vec3;

public class LargeDripstoneFeature extends Feature<LargeDripstoneConfiguration> {
   public LargeDripstoneFeature(Codec<LargeDripstoneConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<LargeDripstoneConfiguration> param0) {
      WorldGenLevel var2 = param0.level();
      BlockPos var3 = param0.origin();
      LargeDripstoneConfiguration var4 = param0.config();
      RandomSource var5 = param0.random();
      if (!DripstoneUtils.isEmptyOrWater(var2, var3)) {
         return false;
      }

      Optional<Column> var6 = Column.scan(var2, var3, var4.floorToCeilingSearchRange, DripstoneUtils::isEmptyOrWater, DripstoneUtils::isDripstoneBaseOrLava);
      if (var6.isPresent() && var6.get() instanceof Column.Range) {
         Column.Range var7 = (Column.Range)var6.get();
         if (var7.height() < 4) {
            return false;
         }

         int var8 = (int)(var7.height() * var4.maxColumnRadiusToCaveHeightRatio);
         int var9 = Mth.clamp(var8, var4.columnRadius.getMinValue(), var4.columnRadius.getMaxValue());
         int var10 = Mth.randomBetweenInclusive(var5, var4.columnRadius.getMinValue(), var9);
         LargeDripstoneFeature.LargeDripstone var11 = makeDripstone(
            var3.atY(var7.ceiling() - 1), false, var5, var10, var4.stalactiteBluntness, var4.heightScale
         );
         LargeDripstoneFeature.LargeDripstone var12 = makeDripstone(var3.atY(var7.floor() + 1), true, var5, var10, var4.stalagmiteBluntness, var4.heightScale);
         LargeDripstoneFeature.WindOffsetter var13;
         if (var11.isSuitableForWind(var4) && var12.isSuitableForWind(var4)) {
            var13 = new LargeDripstoneFeature.WindOffsetter(var3.getY(), var5, var4.windSpeed);
         } else {
            var13 = LargeDripstoneFeature.WindOffsetter.noWind();
         }

         boolean var14 = var11.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(var2, var13);
         boolean var15 = var12.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(var2, var13);
         if (var14) {
            var11.placeBlocks(var2, var5, var13);
         }

         if (var15) {
            var12.placeBlocks(var2, var5, var13);
         }

         return true;
      } else {
         return false;
      }
   }

   private static LargeDripstoneFeature.LargeDripstone makeDripstone(
      BlockPos param0, boolean param1, RandomSource param2, int param3, FloatProvider param4, FloatProvider param5
   ) {
      return new LargeDripstoneFeature.LargeDripstone(param0, param1, param3, param4.sample(param2), param5.sample(param2));
   }

   private void placeDebugMarkers(WorldGenLevel param0, BlockPos param1, Column.Range param2, LargeDripstoneFeature.WindOffsetter param3) {
      param0.setBlock(param3.offset(param1.atY(param2.ceiling() - 1)), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
      param0.setBlock(param3.offset(param1.atY(param2.floor() + 1)), Blocks.GOLD_BLOCK.defaultBlockState(), 2);

      for (BlockPos.MutableBlockPos var5 = param1.atY(param2.floor() + 2).mutable(); var5.getY() < param2.ceiling() - 1; var5.move(Direction.UP)) {
         BlockPos var6 = param3.offset(var5);
         if (DripstoneUtils.isEmptyOrWater(param0, var6) || param0.getBlockState(var6).is(Blocks.DRIPSTONE_BLOCK)) {
            param0.setBlock(var6, Blocks.CREEPER_HEAD.defaultBlockState(), 2);
         }
      }
   }

   static final class LargeDripstone {
      private BlockPos root;
      private final boolean pointingUp;
      private int radius;
      private final double bluntness;
      private final double scale;

      LargeDripstone(BlockPos param0, boolean param1, int param2, double param3, double param5) {
         this.root = param0;
         this.pointingUp = param1;
         this.radius = param2;
         this.bluntness = param3;
         this.scale = param5;
      }

      private int getHeight() {
         return this.getHeightAtRadius(0.0F);
      }

      private int getMinY() {
         return this.pointingUp ? this.root.getY() : this.root.getY() - this.getHeight();
      }

      private int getMaxY() {
         return !this.pointingUp ? this.root.getY() : this.root.getY() + this.getHeight();
      }

      boolean moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(WorldGenLevel param0, LargeDripstoneFeature.WindOffsetter param1) {
         while (this.radius > 1) {
            BlockPos.MutableBlockPos var3 = this.root.mutable();
            int var4 = Math.min(10, this.getHeight());

            for (int var5 = 0; var5 < var4; var5++) {
               if (param0.getBlockState(var3).is(Blocks.LAVA)) {
                  return false;
               }

               if (DripstoneUtils.isCircleMostlyEmbeddedInStone(param0, param1.offset(var3), this.radius)) {
                  this.root = var3;
                  return true;
               }

               var3.move(this.pointingUp ? Direction.DOWN : Direction.UP);
            }

            this.radius /= 2;
         }

         return false;
      }

      private int getHeightAtRadius(float param0) {
         return (int)DripstoneUtils.getDripstoneHeight(param0, this.radius, this.scale, this.bluntness);
      }

      void placeBlocks(WorldGenLevel param0, RandomSource param1, LargeDripstoneFeature.WindOffsetter param2) {
         for (int var4 = -this.radius; var4 <= this.radius; var4++) {
            for (int var5 = -this.radius; var5 <= this.radius; var5++) {
               float var6 = Mth.sqrt(var4 * var4 + var5 * var5);
               if (!(var6 > this.radius)) {
                  int var7 = this.getHeightAtRadius(var6);
                  if (var7 > 0) {
                     if (param1.nextFloat() < 0.2) {
                        var7 = (int)(var7 * Mth.randomBetween(param1, 0.8F, 1.0F));
                     }

                     BlockPos.MutableBlockPos var8 = this.root.offset(var4, 0, var5).mutable();
                     boolean var9 = false;
                     int var10 = this.pointingUp ? param0.getHeight(Heightmap.Types.WORLD_SURFACE_WG, var8.getX(), var8.getZ()) : Integer.MAX_VALUE;

                     for (int var11 = 0; var11 < var7 && var8.getY() < var10; var11++) {
                        BlockPos var12 = param2.offset(var8);
                        if (DripstoneUtils.isEmptyOrWaterOrLava(param0, var12)) {
                           var9 = true;
                           Block var13 = Blocks.DRIPSTONE_BLOCK;
                           param0.setBlock(var12, var13.defaultBlockState(), 2);
                        } else if (var9 && param0.getBlockState(var12).is(BlockTags.BASE_STONE_OVERWORLD)) {
                           break;
                        }

                        var8.move(this.pointingUp ? Direction.UP : Direction.DOWN);
                     }
                  }
               }
            }
         }
      }

      boolean isSuitableForWind(LargeDripstoneConfiguration param0) {
         return this.radius >= param0.minRadiusForWind && this.bluntness >= param0.minBluntnessForWind;
      }
   }

   static final class WindOffsetter {
      private final int originY;
      @Nullable
      private final Vec3 windSpeed;

      WindOffsetter(int param0, RandomSource param1, FloatProvider param2) {
         this.originY = param0;
         float var4 = param2.sample(param1);
         float var5 = Mth.randomBetween(param1, 0.0F, (float) Math.PI);
         this.windSpeed = new Vec3(Mth.cos(var5) * var4, 0.0, Mth.sin(var5) * var4);
      }

      private WindOffsetter() {
         this.originY = 0;
         this.windSpeed = null;
      }

      static LargeDripstoneFeature.WindOffsetter noWind() {
         return new LargeDripstoneFeature.WindOffsetter();
      }

      BlockPos offset(BlockPos param0) {
         if (this.windSpeed == null) {
            return param0;
         }

         int var2 = this.originY - param0.getY();
         Vec3 var3 = this.windSpeed.scale(var2);
         return param0.offset(Mth.floor(var3.x), 0, Mth.floor(var3.z));
      }
   }
}

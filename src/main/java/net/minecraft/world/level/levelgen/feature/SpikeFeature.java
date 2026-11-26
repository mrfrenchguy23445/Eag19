package net.minecraft.world.level.levelgen.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;
import net.minecraft.world.phys.AABB;

public class SpikeFeature extends Feature<SpikeConfiguration> {
   public static final int NUMBER_OF_SPIKES = 10;
   private static final int SPIKE_DISTANCE = 42;
   private static final LoadingCache<Long, List<SpikeFeature.EndSpike>> SPIKE_CACHE = CacheBuilder.newBuilder()
      .expireAfterWrite(5L, TimeUnit.MINUTES)
      .build(new SpikeFeature.SpikeCacheLoader());

   public SpikeFeature(Codec<SpikeConfiguration> param0) {
      super(param0);
   }

   public static List<SpikeFeature.EndSpike> getSpikesForLevel(WorldGenLevel param0) {
      RandomSource var1 = RandomSource.create(param0.getSeed());
      long var2 = var1.nextLong() & 65535L;
      return SPIKE_CACHE.getUnchecked(var2);
   }

   @Override
   public boolean place(FeaturePlaceContext<SpikeConfiguration> param0) {
      SpikeConfiguration var2 = param0.config();
      WorldGenLevel var3 = param0.level();
      RandomSource var4 = param0.random();
      BlockPos var5 = param0.origin();
      List<SpikeFeature.EndSpike> var6 = var2.getSpikes();
      if (var6.isEmpty()) {
         var6 = getSpikesForLevel(var3);
      }

      for (SpikeFeature.EndSpike var8 : var6) {
         if (var8.isCenterWithinChunk(var5)) {
            this.placeSpike(var3, var4, var2, var8);
         }
      }

      return true;
   }

   private void placeSpike(ServerLevelAccessor param0, RandomSource param1, SpikeConfiguration param2, SpikeFeature.EndSpike param3) {
      int var5 = param3.getRadius();

      for (BlockPos var7 : BlockPos.betweenClosed(
         new BlockPos(param3.getCenterX() - var5, param0.getMinBuildHeight(), param3.getCenterZ() - var5),
         new BlockPos(param3.getCenterX() + var5, param3.getHeight() + 10, param3.getCenterZ() + var5)
      )) {
         if (var7.distToLowCornerSqr(param3.getCenterX(), var7.getY(), param3.getCenterZ()) <= var5 * var5 + 1 && var7.getY() < param3.getHeight()) {
            this.setBlock(param0, var7, Blocks.OBSIDIAN.defaultBlockState());
         } else if (var7.getY() > 65) {
            this.setBlock(param0, var7, Blocks.AIR.defaultBlockState());
         }
      }

      if (param3.isGuarded()) {
         int var6 = -2;
         int var7 = 2;
         int var8 = 3;
         BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos();

         for (int var10 = -2; var10 <= 2; var10++) {
            for (int var11 = -2; var11 <= 2; var11++) {
               for (int var12 = 0; var12 <= 3; var12++) {
                  boolean var13 = Mth.abs(var10) == 2;
                  boolean var14 = Mth.abs(var11) == 2;
                  boolean var15 = var12 == 3;
                  if (var13 || var14 || var15) {
                     boolean var16 = var10 == -2 || var10 == 2 || var15;
                     boolean var17 = var11 == -2 || var11 == 2 || var15;
                     BlockState var18 = Blocks.IRON_BARS
                        .defaultBlockState()
                        .setValue(IronBarsBlock.NORTH, var16 && var11 != -2)
                        .setValue(IronBarsBlock.SOUTH, var16 && var11 != 2)
                        .setValue(IronBarsBlock.WEST, var17 && var10 != -2)
                        .setValue(IronBarsBlock.EAST, var17 && var10 != 2);
                     this.setBlock(param0, var9.set(param3.getCenterX() + var10, param3.getHeight() + var12, param3.getCenterZ() + var11), var18);
                  }
               }
            }
         }
      }

      EndCrystal var6 = EntityType.END_CRYSTAL.create(param0.getLevel());
      if (var6 != null) {
         var6.setBeamTarget(param2.getCrystalBeamTarget());
         var6.setInvulnerable(param2.isCrystalInvulnerable());
         var6.moveTo(param3.getCenterX() + 0.5, param3.getHeight() + 1, param3.getCenterZ() + 0.5, param1.nextFloat() * 360.0F, 0.0F);
         param0.addFreshEntity(var6);
         this.setBlock(param0, new BlockPos(param3.getCenterX(), param3.getHeight(), param3.getCenterZ()), Blocks.BEDROCK.defaultBlockState());
      }
   }

   public static class EndSpike {
      public static final Codec<SpikeFeature.EndSpike> CODEC = RecordCodecBuilder.create(
         param0 -> param0.group(
               Codec.INT.fieldOf("centerX").orElse(0).forGetter(l_param0 -> l_param0.centerX),
               Codec.INT.fieldOf("centerZ").orElse(0).forGetter(l_param0 -> l_param0.centerZ),
               Codec.INT.fieldOf("radius").orElse(0).forGetter(l_param0 -> l_param0.radius),
               Codec.INT.fieldOf("height").orElse(0).forGetter(l_param0 -> l_param0.height),
               Codec.BOOL.fieldOf("guarded").orElse(false).forGetter(l_param0 -> l_param0.guarded)
            )
            .apply(param0, SpikeFeature.EndSpike::new)
      );
      private final int centerX;
      private final int centerZ;
      private final int radius;
      private final int height;
      private final boolean guarded;
      private final AABB topBoundingBox;

      public EndSpike(int param0, int param1, int param2, int param3, boolean param4) {
         this.centerX = param0;
         this.centerZ = param1;
         this.radius = param2;
         this.height = param3;
         this.guarded = param4;
         this.topBoundingBox = new AABB(param0 - param2, DimensionType.MIN_Y, param1 - param2, param0 + param2, DimensionType.MAX_Y, param1 + param2);
      }

      public boolean isCenterWithinChunk(BlockPos param0) {
         return SectionPos.blockToSectionCoord(param0.getX()) == SectionPos.blockToSectionCoord(this.centerX)
            && SectionPos.blockToSectionCoord(param0.getZ()) == SectionPos.blockToSectionCoord(this.centerZ);
      }

      public int getCenterX() {
         return this.centerX;
      }

      public int getCenterZ() {
         return this.centerZ;
      }

      public int getRadius() {
         return this.radius;
      }

      public int getHeight() {
         return this.height;
      }

      public boolean isGuarded() {
         return this.guarded;
      }

      public AABB getTopBoundingBox() {
         return this.topBoundingBox;
      }
   }

   static class SpikeCacheLoader extends CacheLoader<Long, List<SpikeFeature.EndSpike>> {
      public List<SpikeFeature.EndSpike> load(Long param0) {
         IntArrayList var2 = Util.toShuffledList(IntStream.range(0, 10), RandomSource.create(param0));
         List<SpikeFeature.EndSpike> var3 = Lists.newArrayList();

         for (int var4 = 0; var4 < 10; var4++) {
            int var5 = Mth.floor(42.0 * Math.cos(2.0 * (-Math.PI + (Math.PI / 10) * var4)));
            int var6 = Mth.floor(42.0 * Math.sin(2.0 * (-Math.PI + (Math.PI / 10) * var4)));
            int var7 = var2.get(var4);
            int var8 = 2 + var7 / 3;
            int var9 = 76 + var7 * 3;
            boolean var10 = var7 == 1 || var7 == 2;
            var3.add(new SpikeFeature.EndSpike(var5, var6, var8, var9, var10));
         }

         return var3;
      }
   }
}

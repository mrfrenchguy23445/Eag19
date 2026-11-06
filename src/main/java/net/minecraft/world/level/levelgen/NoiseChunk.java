package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.MaterialRuleList;

public class NoiseChunk implements DensityFunction.ContextProvider, DensityFunction.FunctionContext {
   private final NoiseSettings noiseSettings;
   final int cellCountXZ;
   final int cellCountY;
   final int cellNoiseMinY;
   private final int firstCellX;
   private final int firstCellZ;
   final int firstNoiseX;
   final int firstNoiseZ;
   final List<NoiseChunk.NoiseInterpolator> interpolators;
   final List<NoiseChunk.CacheAllInCell> cellCaches;
   private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();
   private final Long2IntMap preliminarySurfaceLevel = new Long2IntOpenHashMap();
   private final Aquifer aquifer;
   private final DensityFunction initialDensityNoJaggedness;
   private final NoiseChunk.BlockStateFiller blockStateRule;
   private final Blender blender;
   private final NoiseChunk.FlatCache blendAlpha;
   private final NoiseChunk.FlatCache blendOffset;
   private final DensityFunctions.BeardifierOrMarker beardifier;
   private long lastBlendingDataPos = ChunkPos.INVALID_CHUNK_POS;
   private Blender.BlendingOutput lastBlendingOutput = new Blender.BlendingOutput(1.0, 0.0);
   final int noiseSizeXZ;
   final int cellWidth;
   final int cellHeight;
   boolean interpolating;
   boolean fillingCell;
   private int cellStartBlockX;
   int cellStartBlockY;
   private int cellStartBlockZ;
   int inCellX;
   int inCellY;
   int inCellZ;
   long interpolationCounter;
   long arrayInterpolationCounter;
   int arrayIndex;
   private final DensityFunction.ContextProvider sliceFillingContextProvider = new DensityFunction.ContextProvider() {
      @Override
      public DensityFunction.FunctionContext forIndex(int param0) {
         NoiseChunk.this.cellStartBlockY = (param0 + NoiseChunk.this.cellNoiseMinY) * NoiseChunk.this.cellHeight;
         NoiseChunk.this.interpolationCounter++;
         NoiseChunk.this.inCellY = 0;
         NoiseChunk.this.arrayIndex = param0;
         return NoiseChunk.this;
      }

      @Override
      public void fillAllDirectly(double[] param0, DensityFunction param1) {
         for (int var3 = 0; var3 < NoiseChunk.this.cellCountY + 1; var3++) {
            NoiseChunk.this.cellStartBlockY = (var3 + NoiseChunk.this.cellNoiseMinY) * NoiseChunk.this.cellHeight;
            NoiseChunk.this.interpolationCounter++;
            NoiseChunk.this.inCellY = 0;
            NoiseChunk.this.arrayIndex = var3;
            param0[var3] = param1.compute(NoiseChunk.this);
         }
      }
   };

   public static NoiseChunk forChunk(
      ChunkAccess param0,
      RandomState param1,
      DensityFunctions.BeardifierOrMarker param2,
      NoiseGeneratorSettings param3,
      Aquifer.FluidPicker param4,
      Blender param5
   ) {
      NoiseSettings var6 = param3.noiseSettings().clampToHeightAccessor(param0);
      ChunkPos var7 = param0.getPos();
      int var8 = 16 / var6.getCellWidth();
      return new NoiseChunk(var8, param1, var7.getMinBlockX(), var7.getMinBlockZ(), var6, param2, param3, param4, param5);
   }

   public NoiseChunk(
      int param0,
      RandomState param1,
      int param2,
      int param3,
      NoiseSettings param4,
      DensityFunctions.BeardifierOrMarker param5,
      NoiseGeneratorSettings param6,
      Aquifer.FluidPicker param7,
      Blender param8
   ) {
      this.noiseSettings = param4;
      this.cellWidth = param4.getCellWidth();
      this.cellHeight = param4.getCellHeight();
      this.cellCountXZ = param0;
      this.cellCountY = Mth.floorDiv(param4.height(), this.cellHeight);
      this.cellNoiseMinY = Mth.floorDiv(param4.minY(), this.cellHeight);
      this.firstCellX = Math.floorDiv(param2, this.cellWidth);
      this.firstCellZ = Math.floorDiv(param3, this.cellWidth);
      this.interpolators = Lists.newArrayList();
      this.cellCaches = Lists.newArrayList();
      this.firstNoiseX = QuartPos.fromBlock(param2);
      this.firstNoiseZ = QuartPos.fromBlock(param3);
      this.noiseSizeXZ = QuartPos.fromBlock(param0 * this.cellWidth);
      this.blender = param8;
      this.beardifier = param5;
      this.blendAlpha = new NoiseChunk.FlatCache(new NoiseChunk.BlendAlpha(), false);
      this.blendOffset = new NoiseChunk.FlatCache(new NoiseChunk.BlendOffset(), false);

      for (int var10 = 0; var10 <= this.noiseSizeXZ; var10++) {
         int var11 = this.firstNoiseX + var10;
         int var12 = QuartPos.toBlock(var11);

         for (int var13 = 0; var13 <= this.noiseSizeXZ; var13++) {
            int var14 = this.firstNoiseZ + var13;
            int var15 = QuartPos.toBlock(var14);
            Blender.BlendingOutput var16 = param8.blendOffsetAndFactor(var12, var15);
            this.blendAlpha.values[var10][var13] = var16.alpha();
            this.blendOffset.values[var10][var13] = var16.blendingOffset();
         }
      }

      NoiseRouter var10 = param1.router();
      NoiseRouter var11 = var10.mapAll(this::wrap);
      if (!param6.isAquifersEnabled()) {
         this.aquifer = Aquifer.createDisabled(param7);
      } else {
         int var12 = SectionPos.blockToSectionCoord(param2);
         int var13 = SectionPos.blockToSectionCoord(param3);
         this.aquifer = Aquifer.create(this, new ChunkPos(var12, var13), var11, param1.aquiferRandom(), param4.minY(), param4.height(), param7);
      }

      Builder<NoiseChunk.BlockStateFiller> var12 = ImmutableList.builder();
      DensityFunction var13 = DensityFunctions.cacheAllInCell(DensityFunctions.add(var11.finalDensity(), DensityFunctions.BeardifierMarker.INSTANCE))
         .mapAll(this::wrap);
      var12.add(l_param1 -> this.aquifer.computeSubstance(l_param1, var13.compute(l_param1)));
      if (param6.oreVeinsEnabled()) {
         var12.add(OreVeinifier.create(var11.veinToggle(), var11.veinRidged(), var11.veinGap(), param1.oreRandom()));
      }

      this.blockStateRule = new MaterialRuleList(var12.build());
      this.initialDensityNoJaggedness = var11.initialDensityWithoutJaggedness();
   }

   protected Climate.Sampler cachedClimateSampler(NoiseRouter param0, List<Climate.ParameterPoint> param1) {
      return new Climate.Sampler(
         param0.temperature().mapAll(this::wrap),
         param0.vegetation().mapAll(this::wrap),
         param0.continents().mapAll(this::wrap),
         param0.erosion().mapAll(this::wrap),
         param0.depth().mapAll(this::wrap),
         param0.ridges().mapAll(this::wrap),
         param1
      );
   }

   @Nullable
   protected BlockState getInterpolatedState() {
      return this.blockStateRule.calculate(this);
   }

   @Override
   public int blockX() {
      return this.cellStartBlockX + this.inCellX;
   }

   @Override
   public int blockY() {
      return this.cellStartBlockY + this.inCellY;
   }

   @Override
   public int blockZ() {
      return this.cellStartBlockZ + this.inCellZ;
   }

   public int preliminarySurfaceLevel(int param0, int param1) {
      int var3 = QuartPos.toBlock(QuartPos.fromBlock(param0));
      int var4 = QuartPos.toBlock(QuartPos.fromBlock(param1));
      return this.preliminarySurfaceLevel.computeIfAbsent(ColumnPos.asLong(var3, var4), this::computePreliminarySurfaceLevel);
   }

   private int computePreliminarySurfaceLevel(long param0) {
      int var3 = ColumnPos.getX(param0);
      int var4 = ColumnPos.getZ(param0);
      int var5 = this.noiseSettings.minY();

      for (int var6 = var5 + this.noiseSettings.height(); var6 >= var5; var6 -= this.cellHeight) {
         if (this.initialDensityNoJaggedness.compute(new DensityFunction.SinglePointContext(var3, var6, var4)) > 0.390625) {
            return var6;
         }
      }

      return Integer.MAX_VALUE;
   }

   @Override
   public Blender getBlender() {
      return this.blender;
   }

   private void fillSlice(boolean param0, int param1) {
      this.cellStartBlockX = param1 * this.cellWidth;
      this.inCellX = 0;

      for (int var3 = 0; var3 < this.cellCountXZ + 1; var3++) {
         int var4 = this.firstCellZ + var3;
         this.cellStartBlockZ = var4 * this.cellWidth;
         this.inCellZ = 0;
         this.arrayInterpolationCounter++;

         // Indexed loop instead of forEach: runs per z-row per chunk; iterator churn +
         // megamorphic dispatch is measurable in wasm-gc.
         for (int var5 = 0, var6 = this.interpolators.size(); var5 < var6; var5++) {
            NoiseChunk.NoiseInterpolator var7 = (NoiseChunk.NoiseInterpolator)this.interpolators.get(var5);
            double[] var8 = (param0 ? var7.slice0 : var7.slice1)[var3];
            var7.fillArray(var8, this.sliceFillingContextProvider);
         }
      }

      this.arrayInterpolationCounter++;
   }

   public void initializeForFirstCellX() {
      if (this.interpolating) {
         throw new IllegalStateException("Staring interpolation twice");
      }

      this.interpolating = true;
      this.interpolationCounter = 0L;
      this.fillSlice(true, this.firstCellX);
   }

   public void advanceCellX(int param0) {
      this.fillSlice(false, this.firstCellX + param0 + 1);
      this.cellStartBlockX = (this.firstCellX + param0) * this.cellWidth;
   }

   public NoiseChunk forIndex(int param0) {
      int var2 = Math.floorMod(param0, this.cellWidth);
      int var3 = Math.floorDiv(param0, this.cellWidth);
      int var4 = Math.floorMod(var3, this.cellWidth);
      int var5 = this.cellHeight - 1 - Math.floorDiv(var3, this.cellWidth);
      this.inCellX = var4;
      this.inCellY = var5;
      this.inCellZ = var2;
      this.arrayIndex = param0;
      return this;
   }

   @Override
   public void fillAllDirectly(double[] param0, DensityFunction param1) {
      this.arrayIndex = 0;

      for (int var3 = this.cellHeight - 1; var3 >= 0; var3--) {
         this.inCellY = var3;

         for (int var4 = 0; var4 < this.cellWidth; var4++) {
            this.inCellX = var4;

            for (int var5 = 0; var5 < this.cellWidth; var5++) {
               this.inCellZ = var5;
               param0[this.arrayIndex++] = param1.compute(this);
            }
         }
      }
   }

   public void selectCellYZ(int param0, int param1) {
      // Indexed loop instead of forEach: runs per block cell per slice, and a
      // capturing lambda here is allocation + megamorphic dispatch overhead in wasm-gc.
      for (int var2 = 0, var3 = this.interpolators.size(); var2 < var3; var2++) {
         ((NoiseChunk.NoiseInterpolator)this.interpolators.get(var2)).selectCellYZ(param0, param1);
      }
      this.fillingCell = true;
      this.cellStartBlockY = (param0 + this.cellNoiseMinY) * this.cellHeight;
      this.cellStartBlockZ = (this.firstCellZ + param1) * this.cellWidth;
      this.arrayInterpolationCounter++;

      // Indexed loop instead of forEach: ~448 calls per chunk (cellWidth 4); iterator
      // churn + dispatch is measurable in wasm-gc.
      for (int var4 = 0, var5 = this.cellCaches.size(); var4 < var5; var4++) {
         NoiseChunk.CacheAllInCell var6 = this.cellCaches.get(var4);
         var6.noiseFiller.fillArray(var6.values, this);
      }

      this.arrayInterpolationCounter++;
      this.fillingCell = false;
   }

   public void updateForY(int param0, double param1) {
      this.inCellY = param0 - this.cellStartBlockY;
      for (int var2 = 0, var3 = this.interpolators.size(); var2 < var3; var2++) {
         ((NoiseChunk.NoiseInterpolator)this.interpolators.get(var2)).updateForY(param1);
      }
   }

   public void updateForX(int param0, double param1) {
      this.inCellX = param0 - this.cellStartBlockX;
      for (int var2 = 0, var3 = this.interpolators.size(); var2 < var3; var2++) {
         ((NoiseChunk.NoiseInterpolator)this.interpolators.get(var2)).updateForX(param1);
      }
   }

   public void updateForZ(int param0, double param1) {
      this.inCellZ = param0 - this.cellStartBlockZ;
      this.interpolationCounter++;
      for (int var2 = 0, var3 = this.interpolators.size(); var2 < var3; var2++) {
         ((NoiseChunk.NoiseInterpolator)this.interpolators.get(var2)).updateForZ(param1);
      }
   }

   public void stopInterpolation() {
      if (!this.interpolating) {
         throw new IllegalStateException("Staring interpolation twice");
      }

      this.interpolating = false;
   }

   public void swapSlices() {
      for (int var1 = 0, var2 = this.interpolators.size(); var1 < var2; var1++) {
         ((NoiseChunk.NoiseInterpolator)this.interpolators.get(var1)).swapSlices();
      }
   }

   public Aquifer aquifer() {
      return this.aquifer;
   }

   protected int cellWidth() {
      return this.cellWidth;
   }

   protected int cellHeight() {
      return this.cellHeight;
   }

   Blender.BlendingOutput getOrComputeBlendingOutput(int param0, int param1) {
      long var3 = ChunkPos.asLong(param0, param1);
      if (this.lastBlendingDataPos == var3) {
         return this.lastBlendingOutput;
      }

      this.lastBlendingDataPos = var3;
      Blender.BlendingOutput var5 = this.blender.blendOffsetAndFactor(param0, param1);
      this.lastBlendingOutput = var5;
      return var5;
   }

   protected DensityFunction wrap(DensityFunction param0) {
      return this.wrapped.computeIfAbsent(param0, this::wrapNew);
   }

   private DensityFunction wrapNew(DensityFunction param0) {
      if (param0 instanceof DensityFunctions.Marker var2) {
         return switch (var2.type()) {
            case Interpolated -> new NoiseChunk.NoiseInterpolator(var2.wrapped());
            case FlatCache -> new NoiseChunk.FlatCache(var2.wrapped(), true);
            case Cache2D -> new NoiseChunk.Cache2D(var2.wrapped());
            case CacheOnce -> new NoiseChunk.CacheOnce(var2.wrapped());
            case CacheAllInCell -> new NoiseChunk.CacheAllInCell(var2.wrapped());
         };
      } else {
         if (this.blender != Blender.empty()) {
            if (param0 == DensityFunctions.BlendAlpha.INSTANCE) {
               return this.blendAlpha;
            }

            if (param0 == DensityFunctions.BlendOffset.INSTANCE) {
               return this.blendOffset;
            }
         }

         if (param0 == DensityFunctions.BeardifierMarker.INSTANCE) {
            return this.beardifier;
         } else {
            return param0 instanceof DensityFunctions.HolderHolder var2 ? var2.function().value() : param0;
         }
      }
   }

   class BlendAlpha implements NoiseChunk.NoiseChunkDensityFunction {
      @Override
      public DensityFunction wrapped() {
         return DensityFunctions.BlendAlpha.INSTANCE;
      }

      @Override
      public DensityFunction mapAll(DensityFunction.Visitor param0) {
         return this.wrapped().mapAll(param0);
      }

      @Override
      public double compute(DensityFunction.FunctionContext param0) {
         return NoiseChunk.this.getOrComputeBlendingOutput(param0.blockX(), param0.blockZ()).alpha();
      }

      @Override
      public void fillArray(double[] param0, DensityFunction.ContextProvider param1) {
         param1.fillAllDirectly(param0, this);
      }

      @Override
      public double minValue() {
         return 0.0;
      }

      @Override
      public double maxValue() {
         return 1.0;
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return DensityFunctions.BlendAlpha.CODEC;
      }
   }

   class BlendOffset implements NoiseChunk.NoiseChunkDensityFunction {
      @Override
      public DensityFunction wrapped() {
         return DensityFunctions.BlendOffset.INSTANCE;
      }

      @Override
      public DensityFunction mapAll(DensityFunction.Visitor param0) {
         return this.wrapped().mapAll(param0);
      }

      @Override
      public double compute(DensityFunction.FunctionContext param0) {
         return NoiseChunk.this.getOrComputeBlendingOutput(param0.blockX(), param0.blockZ()).blendingOffset();
      }

      @Override
      public void fillArray(double[] param0, DensityFunction.ContextProvider param1) {
         param1.fillAllDirectly(param0, this);
      }

      @Override
      public double minValue() {
         return Double.NEGATIVE_INFINITY;
      }

      @Override
      public double maxValue() {
         return Double.POSITIVE_INFINITY;
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return DensityFunctions.BlendOffset.CODEC;
      }
   }

   @FunctionalInterface
   public interface BlockStateFiller {
      @Nullable
      BlockState calculate(DensityFunction.FunctionContext var1);
   }

   static class Cache2D implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      private final DensityFunction function;
      private long lastPos2D = ChunkPos.INVALID_CHUNK_POS;
      private double lastValue;

      Cache2D(DensityFunction param0) {
         this.function = param0;
      }

      @Override
      public double compute(DensityFunction.FunctionContext param0) {
         int var2 = param0.blockX();
         int var3 = param0.blockZ();
         long var4 = ChunkPos.asLong(var2, var3);
         if (this.lastPos2D == var4) {
            return this.lastValue;
         }

         this.lastPos2D = var4;
         double var6 = this.function.compute(param0);
         this.lastValue = var6;
         return var6;
      }

      @Override
      public void fillArray(double[] param0, DensityFunction.ContextProvider param1) {
         this.function.fillArray(param0, param1);
      }

      @Override
      public DensityFunction wrapped() {
         return this.function;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.Cache2D;
      }
   }

   class CacheAllInCell implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      final DensityFunction noiseFiller;
      final double[] values;

      CacheAllInCell(DensityFunction param1) {
         this.noiseFiller = param1;
         this.values = new double[NoiseChunk.this.cellWidth * NoiseChunk.this.cellWidth * NoiseChunk.this.cellHeight];
         NoiseChunk.this.cellCaches.add(this);
      }

      @Override
      public double compute(DensityFunction.FunctionContext param0) {
         if (param0 != NoiseChunk.this) {
            return this.noiseFiller.compute(param0);
         }

         if (!NoiseChunk.this.interpolating) {
            throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
         }

         int var2 = NoiseChunk.this.inCellX;
         int var3 = NoiseChunk.this.inCellY;
         int var4 = NoiseChunk.this.inCellZ;
         return var2 >= 0
               && var3 >= 0
               && var4 >= 0
               && var2 < NoiseChunk.this.cellWidth
               && var3 < NoiseChunk.this.cellHeight
               && var4 < NoiseChunk.this.cellWidth
            ? this.values[((NoiseChunk.this.cellHeight - 1 - var3) * NoiseChunk.this.cellWidth + var2) * NoiseChunk.this.cellWidth + var4]
            : this.noiseFiller.compute(param0);
      }

      @Override
      public void fillArray(double[] param0, DensityFunction.ContextProvider param1) {
         param1.fillAllDirectly(param0, this);
      }

      @Override
      public DensityFunction wrapped() {
         return this.noiseFiller;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.CacheAllInCell;
      }
   }

   class CacheOnce implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      private final DensityFunction function;
      private long lastCounter;
      private long lastArrayCounter;
      private double lastValue;
      @Nullable
      private double[] lastArray;

      CacheOnce(DensityFunction param1) {
         this.function = param1;
      }

      @Override
      public double compute(DensityFunction.FunctionContext param0) {
         if (param0 != NoiseChunk.this) {
            return this.function.compute(param0);
         }

         if (this.lastArray != null && this.lastArrayCounter == NoiseChunk.this.arrayInterpolationCounter) {
            return this.lastArray[NoiseChunk.this.arrayIndex];
         }

         if (this.lastCounter == NoiseChunk.this.interpolationCounter) {
            return this.lastValue;
         }

         this.lastCounter = NoiseChunk.this.interpolationCounter;
         double var2 = this.function.compute(param0);
         this.lastValue = var2;
         return var2;
      }

      @Override
      public void fillArray(double[] param0, DensityFunction.ContextProvider param1) {
         if (this.lastArray != null && this.lastArrayCounter == NoiseChunk.this.arrayInterpolationCounter) {
            System.arraycopy(this.lastArray, 0, param0, 0, param0.length);
         } else {
            this.wrapped().fillArray(param0, param1);
            if (this.lastArray != null && this.lastArray.length == param0.length) {
               System.arraycopy(param0, 0, this.lastArray, 0, param0.length);
            } else {
               this.lastArray = (double[])param0.clone();
            }

            this.lastArrayCounter = NoiseChunk.this.arrayInterpolationCounter;
         }
      }

      @Override
      public DensityFunction wrapped() {
         return this.function;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.CacheOnce;
      }
   }

   class FlatCache implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      private final DensityFunction noiseFiller;
      final double[][] values;

      FlatCache(DensityFunction param1, boolean param2) {
         this.noiseFiller = param1;
         this.values = new double[NoiseChunk.this.noiseSizeXZ + 1][NoiseChunk.this.noiseSizeXZ + 1];
         if (param2) {
            for (int var4 = 0; var4 <= NoiseChunk.this.noiseSizeXZ; var4++) {
               int var5 = NoiseChunk.this.firstNoiseX + var4;
               int var6 = QuartPos.toBlock(var5);

               for (int var7 = 0; var7 <= NoiseChunk.this.noiseSizeXZ; var7++) {
                  int var8 = NoiseChunk.this.firstNoiseZ + var7;
                  int var9 = QuartPos.toBlock(var8);
                  this.values[var4][var7] = param1.compute(new DensityFunction.SinglePointContext(var6, 0, var9));
               }
            }
         }
      }

      @Override
      public double compute(DensityFunction.FunctionContext param0) {
         int var2 = QuartPos.fromBlock(param0.blockX());
         int var3 = QuartPos.fromBlock(param0.blockZ());
         int var4 = var2 - NoiseChunk.this.firstNoiseX;
         int var5 = var3 - NoiseChunk.this.firstNoiseZ;
         int var6 = this.values.length;
         return var4 >= 0 && var5 >= 0 && var4 < var6 && var5 < var6 ? this.values[var4][var5] : this.noiseFiller.compute(param0);
      }

      @Override
      public void fillArray(double[] param0, DensityFunction.ContextProvider param1) {
         param1.fillAllDirectly(param0, this);
      }

      @Override
      public DensityFunction wrapped() {
         return this.noiseFiller;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.FlatCache;
      }
   }

   interface NoiseChunkDensityFunction extends DensityFunction {
      DensityFunction wrapped();

      @Override
      default double minValue() {
         return this.wrapped().minValue();
      }

      @Override
      default double maxValue() {
         return this.wrapped().maxValue();
      }
   }

   public class NoiseInterpolator implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      double[][] slice0;
      double[][] slice1;
      private final DensityFunction noiseFiller;
      private double noise000;
      private double noise001;
      private double noise100;
      private double noise101;
      private double noise010;
      private double noise011;
      private double noise110;
      private double noise111;
      private double valueXZ00;
      private double valueXZ10;
      private double valueXZ01;
      private double valueXZ11;
      private double valueZ0;
      private double valueZ1;
      private double value;

      NoiseInterpolator(DensityFunction param1) {
         this.noiseFiller = param1;
         this.slice0 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
         this.slice1 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
         NoiseChunk.this.interpolators.add(this);
      }

      private double[][] allocateSlice(int param0, int param1) {
         int var3 = param1 + 1;
         int var4 = param0 + 1;
         double[][] var5 = new double[var3][var4];

         for (int var6 = 0; var6 < var3; var6++) {
            var5[var6] = new double[var4];
         }

         return var5;
      }

      void selectCellYZ(int param0, int param1) {
         this.noise000 = this.slice0[param1][param0];
         this.noise001 = this.slice0[param1 + 1][param0];
         this.noise100 = this.slice1[param1][param0];
         this.noise101 = this.slice1[param1 + 1][param0];
         this.noise010 = this.slice0[param1][param0 + 1];
         this.noise011 = this.slice0[param1 + 1][param0 + 1];
         this.noise110 = this.slice1[param1][param0 + 1];
         this.noise111 = this.slice1[param1 + 1][param0 + 1];
      }

      void updateForY(double param0) {
         this.valueXZ00 = Mth.lerp(param0, this.noise000, this.noise010);
         this.valueXZ10 = Mth.lerp(param0, this.noise100, this.noise110);
         this.valueXZ01 = Mth.lerp(param0, this.noise001, this.noise011);
         this.valueXZ11 = Mth.lerp(param0, this.noise101, this.noise111);
      }

      void updateForX(double param0) {
         this.valueZ0 = Mth.lerp(param0, this.valueXZ00, this.valueXZ10);
         this.valueZ1 = Mth.lerp(param0, this.valueXZ01, this.valueXZ11);
      }

      void updateForZ(double param0) {
         this.value = Mth.lerp(param0, this.valueZ0, this.valueZ1);
      }

      @Override
      public double compute(DensityFunction.FunctionContext param0) {
         if (param0 != NoiseChunk.this) {
            return this.noiseFiller.compute(param0);
         } else if (!NoiseChunk.this.interpolating) {
            throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
         } else {
            return NoiseChunk.this.fillingCell
               ? Mth.lerp3(
                  (double)NoiseChunk.this.inCellX / NoiseChunk.this.cellWidth,
                  (double)NoiseChunk.this.inCellY / NoiseChunk.this.cellHeight,
                  (double)NoiseChunk.this.inCellZ / NoiseChunk.this.cellWidth,
                  this.noise000,
                  this.noise100,
                  this.noise010,
                  this.noise110,
                  this.noise001,
                  this.noise101,
                  this.noise011,
                  this.noise111
               )
               : this.value;
         }
      }

      @Override
      public void fillArray(double[] param0, DensityFunction.ContextProvider param1) {
         if (NoiseChunk.this.fillingCell) {
            param1.fillAllDirectly(param0, this);
         } else {
            this.wrapped().fillArray(param0, param1);
         }
      }

      @Override
      public DensityFunction wrapped() {
         return this.noiseFiller;
      }

      private void swapSlices() {
         double[][] var1 = this.slice0;
         this.slice0 = this.slice1;
         this.slice1 = var1;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.Interpolated;
      }
   }
}

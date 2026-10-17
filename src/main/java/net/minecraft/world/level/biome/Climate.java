package net.minecraft.world.level.biome;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public class Climate {
   private static final boolean DEBUG_SLOW_BIOME_SEARCH = false;
   private static final float QUANTIZATION_FACTOR = 10000.0F;
   @VisibleForTesting
   protected static final int PARAMETER_COUNT = 7;

   public static Climate.TargetPoint target(float param0, float param1, float param2, float param3, float param4, float param5) {
      return new Climate.TargetPoint(
         quantizeCoord(param0), quantizeCoord(param1), quantizeCoord(param2), quantizeCoord(param3), quantizeCoord(param4), quantizeCoord(param5)
      );
   }

   public static Climate.ParameterPoint parameters(float param0, float param1, float param2, float param3, float param4, float param5, float param6) {
      return new Climate.ParameterPoint(
         Climate.Parameter.point(param0),
         Climate.Parameter.point(param1),
         Climate.Parameter.point(param2),
         Climate.Parameter.point(param3),
         Climate.Parameter.point(param4),
         Climate.Parameter.point(param5),
         quantizeCoord(param6)
      );
   }

   public static Climate.ParameterPoint parameters(
      Climate.Parameter param0,
      Climate.Parameter param1,
      Climate.Parameter param2,
      Climate.Parameter param3,
      Climate.Parameter param4,
      Climate.Parameter param5,
      float param6
   ) {
      return new Climate.ParameterPoint(param0, param1, param2, param3, param4, param5, quantizeCoord(param6));
   }

   public static long quantizeCoord(float param0) {
      return (long)(param0 * 10000.0F);
   }

   public static float unquantizeCoord(long param0) {
      return (float)param0 / 10000.0F;
   }

   public static Climate.Sampler empty() {
      DensityFunction var0 = DensityFunctions.zero();
      return new Climate.Sampler(var0, var0, var0, var0, var0, var0, List.of());
   }

   public static BlockPos findSpawnPosition(List<Climate.ParameterPoint> param0, Climate.Sampler param1) {
      return (new Climate.SpawnFinder(param0, param1)).result.location();
   }

   interface DistanceMetric<T> {
      long distance(Climate.RTree.Node<T> var1, long[] var2);
   }

   public record Parameter(long min, long max) {
      public static final Codec<Climate.Parameter> CODEC = ExtraCodecs.intervalCodec(
         Codec.floatRange(-2.0F, 2.0F),
         "min",
         "max",
         (param0, param1) -> param0.compareTo(param1) > 0
            ? DataResult.error(() -> "Cannon construct interval, min > max (" + param0 + " > " + param1 + ")")
            : DataResult.success(new Climate.Parameter(Climate.quantizeCoord(param0), Climate.quantizeCoord(param1))),
         param0 -> Climate.unquantizeCoord(param0.min()),
         param0 -> Climate.unquantizeCoord(param0.max())
      );

      public static Climate.Parameter point(float param0) {
         return span(param0, param0);
      }

      public static Climate.Parameter span(float param0, float param1) {
         if (param0 > param1) {
            throw new IllegalArgumentException("min > max: " + param0 + " " + param1);
         } else {
            return new Climate.Parameter(Climate.quantizeCoord(param0), Climate.quantizeCoord(param1));
         }
      }

      public static Climate.Parameter span(Climate.Parameter param0, Climate.Parameter param1) {
         if (param0.min() > param1.max()) {
            throw new IllegalArgumentException("min > max: " + param0 + " " + param1);
         } else {
            return new Climate.Parameter(param0.min(), param1.max());
         }
      }

      @Override
      public String toString() {
         return this.min == this.max ? String.format(Locale.ROOT, "%d", this.min) : String.format(Locale.ROOT, "[%d-%d]", this.min, this.max);
      }

      public long distance(long param0) {
         long var3 = param0 - this.max;
         long var5 = this.min - param0;
         return var3 > 0L ? var3 : Math.max(var5, 0L);
      }

      public long distance(Climate.Parameter param0) {
         long var2 = param0.min() - this.max;
         long var4 = this.min - param0.max();
         return var2 > 0L ? var2 : Math.max(var4, 0L);
      }

      public Climate.Parameter span(@Nullable Climate.Parameter param0) {
         return param0 == null ? this : new Climate.Parameter(Math.min(this.min, param0.min()), Math.max(this.max, param0.max()));
      }
   }

   public static class ParameterList<T> {
      private final List<Pair<Climate.ParameterPoint, T>> values;
      private final Climate.RTree<T> index;

      public static <T> Codec<Climate.ParameterList<T>> codec(MapCodec<T> param0) {
         return ExtraCodecs.nonEmptyList(
               RecordCodecBuilder.<Pair<Climate.ParameterPoint, T>>create(
                     l_param1 -> l_param1.group(Climate.ParameterPoint.CODEC.fieldOf("parameters").forGetter(Pair::getFirst), param0.forGetter(Pair::getSecond))
                        .apply(l_param1, Pair::of)
                  )
                  .listOf()
            )
            .xmap(Climate.ParameterList::new, Climate.ParameterList::values);
      }

      public ParameterList(List<Pair<Climate.ParameterPoint, T>> param0) {
         this.values = param0;
         this.index = Climate.RTree.create(param0);
      }

      public List<Pair<Climate.ParameterPoint, T>> values() {
         return this.values;
      }

      public T findValue(Climate.TargetPoint param0) {
         return this.findValueIndex(param0);
      }

      @VisibleForTesting
      public T findValueBruteForce(Climate.TargetPoint param0) {
         Iterator<Pair<Climate.ParameterPoint, T>> var2 = this.values().iterator();
         Pair<Climate.ParameterPoint, T> var3 = var2.next();
         long var4 = var3.getFirst().fitness(param0);
         T var6 = var3.getSecond();

         while (var2.hasNext()) {
            Pair<Climate.ParameterPoint, T> var7 = var2.next();
            long var8 = var7.getFirst().fitness(param0);
            if (var8 < var4) {
               var4 = var8;
               var6 = var7.getSecond();
            }
         }

         return var6;
      }

      public T findValueIndex(Climate.TargetPoint param0) {
         return this.findValueIndex(param0, Climate.RTree.Node::distance);
      }

      protected T findValueIndex(Climate.TargetPoint param0, Climate.DistanceMetric<T> param1) {
         return this.index.search(param0, param1);
      }
   }

   public record ParameterPoint(
      Climate.Parameter temperature,
      Climate.Parameter humidity,
      Climate.Parameter continentalness,
      Climate.Parameter erosion,
      Climate.Parameter depth,
      Climate.Parameter weirdness,
      long offset
   ) {
      public static final Codec<Climate.ParameterPoint> CODEC = RecordCodecBuilder.create(
         param0 -> param0.group(
               Climate.Parameter.CODEC.fieldOf("temperature").forGetter(l_param0 -> l_param0.temperature),
               Climate.Parameter.CODEC.fieldOf("humidity").forGetter(l_param0 -> l_param0.humidity),
               Climate.Parameter.CODEC.fieldOf("continentalness").forGetter(l_param0 -> l_param0.continentalness),
               Climate.Parameter.CODEC.fieldOf("erosion").forGetter(l_param0 -> l_param0.erosion),
               Climate.Parameter.CODEC.fieldOf("depth").forGetter(l_param0 -> l_param0.depth),
               Climate.Parameter.CODEC.fieldOf("weirdness").forGetter(l_param0 -> l_param0.weirdness),
               Codec.floatRange(0.0F, 1.0F).fieldOf("offset").xmap(Climate::quantizeCoord, Climate::unquantizeCoord).forGetter(l_param0 -> l_param0.offset)
            )
            .apply(param0, Climate.ParameterPoint::new)
      );

      long fitness(Climate.TargetPoint param0) {
         return Mth.square(this.temperature.distance(param0.temperature))
            + Mth.square(this.humidity.distance(param0.humidity))
            + Mth.square(this.continentalness.distance(param0.continentalness))
            + Mth.square(this.erosion.distance(param0.erosion))
            + Mth.square(this.depth.distance(param0.depth))
            + Mth.square(this.weirdness.distance(param0.weirdness))
            + Mth.square(this.offset);
      }

      protected List<Climate.Parameter> parameterSpace() {
         return ImmutableList.of(
            this.temperature, this.humidity, this.continentalness, this.erosion, this.depth, this.weirdness, new Climate.Parameter(this.offset, this.offset)
         );
      }
   }

   protected static final class RTree<T> {
      private static final int CHILDREN_PER_NODE = 6;
      private final Climate.RTree.Node<T> root;
      private final ThreadLocal<Climate.RTree.Leaf<T>> lastResult = new ThreadLocal<>();

      private RTree(Climate.RTree.Node<T> param0) {
         this.root = param0;
      }

      public static <T> Climate.RTree<T> create(List<Pair<Climate.ParameterPoint, T>> param0) {
         if (param0.isEmpty()) {
            throw new IllegalArgumentException("Need at least one value to build the search tree.");
         }

         int var1 = param0.get(0).getFirst().parameterSpace().size();
         if (var1 != 7) {
            throw new IllegalStateException("Expecting parameter space to be 7, got " + var1);
         }

         List<Climate.RTree.Leaf<T>> var2 = param0.stream()
            .map(l_param0 -> new Climate.RTree.Leaf<T>(l_param0.getFirst(), l_param0.getSecond()))
            .collect(Collectors.toCollection(ArrayList::new));
         return new Climate.RTree<>(build(var1, var2));
      }

      private static <T> Climate.RTree.Node<T> build(int param0, List<? extends Climate.RTree.Node<T>> param1) {
         if (param1.isEmpty()) {
            throw new IllegalStateException("Need at least one child to build a node");
         }

         if (param1.size() == 1) {
            return (Climate.RTree.Node<T>)param1.get(0);
         }

         if (param1.size() <= 6) {
            param1.sort(Comparator.comparingLong(l_param1 -> {
               long l_var2 = 0L;

               for (int l_var4 = 0; l_var4 < param0; l_var4++) {
                  Climate.Parameter l_var5 = l_param1.parameterSpace[l_var4];
                  l_var2 += Math.abs((l_var5.min() + l_var5.max()) / 2L);
               }

               return l_var2;
            }));
            return new Climate.RTree.SubTree<>(param1);
         }

         long var2 = Long.MAX_VALUE;
         int var4 = -1;
         List<Climate.RTree.SubTree<T>> var5 = null;

         for (int var6 = 0; var6 < param0; var6++) {
            sort(param1, param0, var6, false);
            List<Climate.RTree.SubTree<T>> var7 = bucketize(param1);
            long var8 = 0L;

            for (Climate.RTree.SubTree<T> var11 : var7) {
               var8 += cost(var11.parameterSpace);
            }

            if (var2 > var8) {
               var2 = var8;
               var4 = var6;
               var5 = var7;
            }
         }

         sort(var5, param0, var4, true);
         return new Climate.RTree.SubTree<>(var5.stream().map(l_param1 -> build(param0, Arrays.asList(l_param1.children))).collect(Collectors.toList()));
      }

      private static <T> void sort(List<? extends Climate.RTree.Node<T>> param0, int param1, int param2, boolean param3) {
         Comparator<Climate.RTree.Node<T>> var4 = comparator(param2, param3);

         for (int var5 = 1; var5 < param1; var5++) {
            var4 = var4.thenComparing(comparator((param2 + var5) % param1, param3));
         }

         param0.sort(var4);
      }

      private static <T> Comparator<Climate.RTree.Node<T>> comparator(int param0, boolean param1) {
         return Comparator.comparingLong(l_param2 -> {
            Climate.Parameter l_var3 = l_param2.parameterSpace[param0];
            long l_var4 = (l_var3.min() + l_var3.max()) / 2L;
            return param1 ? Math.abs(l_var4) : l_var4;
         });
      }

      private static <T> List<Climate.RTree.SubTree<T>> bucketize(List<? extends Climate.RTree.Node<T>> param0) {
         List<Climate.RTree.SubTree<T>> var1 = Lists.newArrayList();
         List<Climate.RTree.Node<T>> var2 = Lists.newArrayList();
         int var3 = (int)Math.pow(6.0, Math.floor(Math.log(param0.size() - 0.01) / Math.log(6.0)));

         for (Climate.RTree.Node<T> var5 : param0) {
            var2.add(var5);
            if (var2.size() >= var3) {
               var1.add(new Climate.RTree.SubTree<>(var2));
               var2 = Lists.newArrayList();
            }
         }

         if (!var2.isEmpty()) {
            var1.add(new Climate.RTree.SubTree<>(var2));
         }

         return var1;
      }

      private static long cost(Climate.Parameter[] param0) {
         long var1 = 0L;

         for (Climate.Parameter var6 : param0) {
            var1 += Math.abs(var6.max() - var6.min());
         }

         return var1;
      }

      static <T> List<Climate.Parameter> buildParameterSpace(List<? extends Climate.RTree.Node<T>> param0) {
         if (param0.isEmpty()) {
            throw new IllegalArgumentException("SubTree needs at least one child");
         }

         int var1 = 7;
         List<Climate.Parameter> var2 = Lists.newArrayList();

         for (int var3 = 0; var3 < 7; var3++) {
            var2.add(null);
         }

         for (Climate.RTree.Node<T> var4 : param0) {
            for (int var5 = 0; var5 < 7; var5++) {
               var2.set(var5, var4.parameterSpace[var5].span(var2.get(var5)));
            }
         }

         return var2;
      }

      public T search(Climate.TargetPoint param0, Climate.DistanceMetric<T> param1) {
         long[] var3 = param0.toParameterArray();
         Climate.RTree.Leaf<T> var4 = this.root.search(var3, this.lastResult.get(), param1);
         this.lastResult.set(var4);
         return var4.value;
      }

      static final class Leaf<T> extends Climate.RTree.Node<T> {
         final T value;

         Leaf(Climate.ParameterPoint param0, T param1) {
            super(param0.parameterSpace());
            this.value = param1;
         }

         @Override
         protected Climate.RTree.Leaf<T> search(long[] param0, @Nullable Climate.RTree.Leaf<T> param1, Climate.DistanceMetric<T> param2) {
            return this;
         }
      }

      abstract static class Node<T> {
         protected final Climate.Parameter[] parameterSpace;

         protected Node(List<Climate.Parameter> param0) {
            this.parameterSpace = param0.toArray(new Climate.Parameter[0]);
         }

         protected abstract Climate.RTree.Leaf<T> search(long[] var1, @Nullable Climate.RTree.Leaf<T> var2, Climate.DistanceMetric<T> var3);

         protected long distance(long[] param0) {
            long var2 = 0L;

            for (int var4 = 0; var4 < 7; var4++) {
               var2 += Mth.square(this.parameterSpace[var4].distance(param0[var4]));
            }

            return var2;
         }

         @Override
         public String toString() {
            return Arrays.toString(this.parameterSpace);
         }
      }

      static final class SubTree<T> extends Climate.RTree.Node<T> {
         final Climate.RTree.Node<T>[] children;

         protected SubTree(List<? extends Climate.RTree.Node<T>> param0) {
            this(Climate.RTree.buildParameterSpace(param0), param0);
         }

         protected SubTree(List<Climate.Parameter> param0, List<? extends Climate.RTree.Node<T>> param1) {
            super(param0);
            this.children = param1.toArray(new Climate.RTree.Node[0]);
         }

         @Override
         protected Climate.RTree.Leaf<T> search(long[] param0, @Nullable Climate.RTree.Leaf<T> param1, Climate.DistanceMetric<T> param2) {
            long var4 = param1 == null ? Long.MAX_VALUE : param2.distance(param1, param0);
            Climate.RTree.Leaf<T> var6 = param1;

            for (Climate.RTree.Node<T> var10 : this.children) {
               long var11 = param2.distance(var10, param0);
               if (var4 > var11) {
                  Climate.RTree.Leaf<T> var13 = var10.search(param0, var6, param2);
                  long var14 = var10 == var13 ? var11 : param2.distance(var13, param0);
                  if (var4 > var14) {
                     var4 = var14;
                     var6 = var13;
                  }
               }
            }

            return var6;
         }
      }
   }

   public record Sampler(
      DensityFunction temperature,
      DensityFunction humidity,
      DensityFunction continentalness,
      DensityFunction erosion,
      DensityFunction depth,
      DensityFunction weirdness,
      List<Climate.ParameterPoint> spawnTarget
   ) {
      public Climate.TargetPoint sample(int param0, int param1, int param2) {
         int var4 = QuartPos.toBlock(param0);
         int var5 = QuartPos.toBlock(param1);
         int var6 = QuartPos.toBlock(param2);
         DensityFunction.SinglePointContext var7 = new DensityFunction.SinglePointContext(var4, var5, var6);
         return Climate.target(
            (float)this.temperature.compute(var7),
            (float)this.humidity.compute(var7),
            (float)this.continentalness.compute(var7),
            (float)this.erosion.compute(var7),
            (float)this.depth.compute(var7),
            (float)this.weirdness.compute(var7)
         );
      }

      public BlockPos findSpawnPosition() {
         return this.spawnTarget.isEmpty() ? BlockPos.ZERO : Climate.findSpawnPosition(this.spawnTarget, this);
      }
   }

   static class SpawnFinder {
      Climate.SpawnFinder.Result result;

      SpawnFinder(List<Climate.ParameterPoint> param0, Climate.Sampler param1) {
         this.result = getSpawnPositionAndFitness(param0, param1, 0, 0);
         this.radialSearch(param0, param1, 2048.0F, 512.0F);
         this.radialSearch(param0, param1, 512.0F, 32.0F);
      }

      private void radialSearch(List<Climate.ParameterPoint> param0, Climate.Sampler param1, float param2, float param3) {
         float var5 = 0.0F;
         float var6 = param3;
         BlockPos var7 = this.result.location();

         while (var6 <= param2) {
            int var8 = var7.getX() + (int)(Math.sin(var5) * var6);
            int var9 = var7.getZ() + (int)(Math.cos(var5) * var6);
            Climate.SpawnFinder.Result var10 = getSpawnPositionAndFitness(param0, param1, var8, var9);
            if (var10.fitness() < this.result.fitness()) {
               this.result = var10;
            }

            var5 += param3 / var6;
            if (var5 > Math.PI * 2) {
               var5 = 0.0F;
               var6 += param3;
            }
         }
      }

      private static Climate.SpawnFinder.Result getSpawnPositionAndFitness(List<Climate.ParameterPoint> param0, Climate.Sampler param1, int param2, int param3) {
         double var4 = Mth.square(2500.0);
         int var6 = 2;
         long var7 = (long)(Mth.square(10000.0F) * Math.pow((Mth.square((long)param2) + Mth.square((long)param3)) / var4, 2.0));
         Climate.TargetPoint var9 = param1.sample(QuartPos.fromBlock(param2), 0, QuartPos.fromBlock(param3));
         Climate.TargetPoint var10 = new Climate.TargetPoint(var9.temperature(), var9.humidity(), var9.continentalness(), var9.erosion(), 0L, var9.weirdness());
         long var11 = Long.MAX_VALUE;

         for (Climate.ParameterPoint var14 : param0) {
            var11 = Math.min(var11, var14.fitness(var10));
         }

         return new Climate.SpawnFinder.Result(new BlockPos(param2, 0, param3), var7 + var11);
      }

      record Result(BlockPos location, long fitness) {
      }
   }

   public record TargetPoint(long temperature, long humidity, long continentalness, long erosion, long depth, long weirdness) {

      @VisibleForTesting
      protected long[] toParameterArray() {
         return new long[]{this.temperature, this.humidity, this.continentalness, this.erosion, this.depth, this.weirdness, 0L};
      }
   }
}

package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.Graph;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableInt;

public class FeatureSorter {
   public static <T> List<FeatureSorter.StepFeatureData> buildFeaturesPerStep(
      List<T> param0, Function<T, List<HolderSet<PlacedFeature>>> param1, boolean param2
   ) {
      Object2IntMap<PlacedFeature> var3 = new Object2IntOpenHashMap<>();
      MutableInt var4 = new MutableInt(0);

      record FeatureData(int featureIndex, int step, PlacedFeature feature) {
      }

      Comparator<FeatureData> var5 = Comparator.comparingInt(FeatureData::step).thenComparingInt(FeatureData::featureIndex);
      Map<FeatureData, Set<FeatureData>> var6 = new TreeMap<>(var5);
      int var7 = 0;

      for (T var9 : param0) {
         List<FeatureData> var10 = Lists.newArrayList();
         List<HolderSet<PlacedFeature>> var11 = param1.apply(var9);
         var7 = Math.max(var7, var11.size());

         for (int var12 = 0; var12 < var11.size(); var12++) {
            for (Holder<PlacedFeature> var14 : var11.get(var12)) {
               PlacedFeature var15 = var14.value();
               var10.add(new FeatureData(var3.computeIfAbsent(var15, l_param1 -> var4.getAndIncrement()), var12, var15));
            }
         }

         for (int var12 = 0; var12 < var10.size(); var12++) {
            Set<FeatureData> var13 = var6.computeIfAbsent(var10.get(var12), l_param1 -> new TreeSet<>(var5));
            if (var12 < var10.size() - 1) {
               var13.add(var10.get(var12 + 1));
            }
         }
      }

      Set<FeatureData> var8 = new TreeSet<>(var5);
      Set<FeatureData> var9 = new TreeSet<>(var5);
      List<FeatureData> var10 = Lists.newArrayList();

      for (FeatureData var12 : var6.keySet()) {
         if (!var9.isEmpty()) {
            throw new IllegalStateException("You somehow broke the universe; DFS bork (iteration finished with non-empty in-progress vertex set");
         }

         if (!var8.contains(var12) && Graph.depthFirstSearch(var6, var8, var9, var10::add, var12)) {
            if (!param2) {
               throw new IllegalStateException("Feature order cycle found");
            }

            List<T> var13 = new ArrayList<>(param0);

            int var14;
            do {
               var14 = var13.size();
               ListIterator<T> var15 = var13.listIterator();

               while (var15.hasNext()) {
                  T var16 = var15.next();
                  var15.remove();

                  try {
                     buildFeaturesPerStep(var13, param1, false);
                  } catch (IllegalStateException var17) {
                     continue;
                  }

                  var15.add(var16);
               }
            } while (var14 != var13.size());

            throw new IllegalStateException("Feature order cycle found, involved sources: " + var13);
         }
      }

      Collections.reverse(var10);
      Builder<FeatureSorter.StepFeatureData> var11 = ImmutableList.builder();

      for (int var12 = 0; var12 < var7; var12++) {
         int var13 = var12;
         List<PlacedFeature> var14 = var10.stream().filter(l_param1 -> l_param1.step() == var13).map(FeatureData::feature).collect(Collectors.toList());
         var11.add(new FeatureSorter.StepFeatureData(var14));
      }

      return var11.build();
   }

   public record StepFeatureData(List<PlacedFeature> features, ToIntFunction<PlacedFeature> indexMapping) {
      StepFeatureData(List<PlacedFeature> param0) {
         this(param0, Util.createIndexLookup(param0, l_param0 -> new Object2IntOpenCustomHashMap<>(l_param0, Util.identityStrategy())));
      }
   }
}

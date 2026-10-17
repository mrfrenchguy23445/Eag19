package net.minecraft.world.level.levelgen.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

public class FeatureCountTracker {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final LoadingCache<ServerLevel, FeatureCountTracker.LevelData> data = CacheBuilder.newBuilder()
      .weakKeys()
      .expireAfterAccess(5L, TimeUnit.MINUTES)
      .build(new CacheLoader<ServerLevel, FeatureCountTracker.LevelData>() {
         public FeatureCountTracker.LevelData load(ServerLevel param0) {
            return new FeatureCountTracker.LevelData(Object2IntMaps.synchronize(new Object2IntOpenHashMap<>()), new MutableInt(0));
         }
      });

   public static void chunkDecorated(ServerLevel param0) {
      try {
         data.get(param0).chunksWithFeatures().increment();
      } catch (Exception var1) {
         LOGGER.error("Failed to increment chunk count", var1);
      }
   }

   public static void featurePlaced(ServerLevel param0, ConfiguredFeature<?, ?> param1, Optional<PlacedFeature> param2) {
      try {
         data.get(param0)
            .featureData()
            .computeInt(new FeatureCountTracker.FeatureData(param1, param2), (l_param0, l_param1) -> l_param1 == null ? 1 : l_param1 + 1);
      } catch (Exception var3) {
         LOGGER.error("Failed to increment feature count", var3);
      }
   }

   public static void clearCounts() {
      data.invalidateAll();
      LOGGER.debug("Cleared feature counts");
   }

   public static void logCounts() {
      LOGGER.debug("Logging feature counts:");
      data.asMap()
         .forEach(
            (l_param0, l_param1) -> {
               String l_var2 = l_param0.dimension().location().toString();
               boolean l_var3 = l_param0.getServer().isRunning();
               Registry<PlacedFeature> l_var4 = l_param0.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
               String l_var5 = (l_var3 ? "running" : "dead") + " " + l_var2;
               Integer l_var6 = l_param1.chunksWithFeatures().getValue();
               LOGGER.debug(l_var5 + " total_chunks: " + l_var6);
               l_param1.featureData()
                  .forEach(
                     (l2_param3, l2_param4) -> LOGGER.debug(
                        l_var5
                           + " "
                           + String.format(Locale.ROOT, "%10d ", l2_param4)
                           + String.format(Locale.ROOT, "%10f ", (double)l2_param4.intValue() / l_var6.intValue())
                           + l2_param3.topFeature().flatMap(l_var4::getResourceKey).<ResourceLocation>map(ResourceKey::location)
                           + " "
                           + l2_param3.feature().feature()
                           + " "
                           + l2_param3.feature()
                     )
                  );
            }
         );
   }

   record FeatureData(ConfiguredFeature<?, ?> feature, Optional<PlacedFeature> topFeature) {
   }

   record LevelData(Object2IntMap<FeatureCountTracker.FeatureData> featureData, MutableInt chunksWithFeatures) {
   }
}

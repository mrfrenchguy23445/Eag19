package net.minecraft.util.worldupdate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMaps;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenCustomHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;

public class WorldUpgrader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setDaemon(true).build();
   private final Registry<LevelStem> dimensions;
   private final Set<ResourceKey<Level>> levels;
   private final boolean eraseCache;
   private final LevelStorageSource.LevelStorageAccess levelStorage;
   private final Thread thread;
   private final DataFixer dataFixer;
   private volatile boolean running = true;
   private volatile boolean finished;
   private volatile float progress;
   private volatile int totalChunks;
   private volatile int converted;
   private volatile int skipped;
   private final Object2FloatMap<ResourceKey<Level>> progressMap = Object2FloatMaps.synchronize(new Object2FloatOpenCustomHashMap<>(Util.identityStrategy()));
   private volatile Component status = Component.translatable("optimizeWorld.stage.counting");
   private static final Pattern REGEX = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");
   private final DimensionDataStorage overworldDataStorage;

   public WorldUpgrader(LevelStorageSource.LevelStorageAccess param0, DataFixer param1, Registry<LevelStem> param2, boolean param3) {
      this.dimensions = param2;
      this.levels = param2.registryKeySet().stream().map(Registries::levelStemToLevel).collect(Collectors.toUnmodifiableSet());
      this.eraseCache = param3;
      this.dataFixer = param1;
      this.levelStorage = param0;
      this.overworldDataStorage = new DimensionDataStorage(this.levelStorage.getDimensionPath(Level.OVERWORLD).resolve("data").toFile(), param1);
      this.thread = THREAD_FACTORY.newThread(this::work);
      this.thread.setUncaughtExceptionHandler((l_param0, l_param1) -> {
         LOGGER.error("Error upgrading world", l_param1);
         this.status = Component.translatable("optimizeWorld.stage.failed");
         this.finished = true;
      });
      this.thread.start();
   }

   public void cancel() {
      this.running = false;

      try {
         this.thread.join();
      } catch (InterruptedException var2) {
      }
   }

   private void work() {
      this.totalChunks = 0;
      Builder<ResourceKey<Level>, ListIterator<ChunkPos>> var1 = ImmutableMap.builder();

      for (ResourceKey<Level> var3 : this.levels) {
         List<ChunkPos> var4 = this.getAllChunkPos(var3);
         var1.put(var3, var4.listIterator());
         this.totalChunks = this.totalChunks + var4.size();
      }

      if (this.totalChunks == 0) {
         this.finished = true;
      } else {
         float var2 = this.totalChunks;
         ImmutableMap<ResourceKey<Level>, ListIterator<ChunkPos>> var3 = var1.build();
         Builder<ResourceKey<Level>, ChunkStorage> var4 = ImmutableMap.builder();

         for (ResourceKey<Level> var6 : this.levels) {
            Path var7 = this.levelStorage.getDimensionPath(var6);
            var4.put(var6, new ChunkStorage(var7.resolve("region"), this.dataFixer, true));
         }

         ImmutableMap<ResourceKey<Level>, ChunkStorage> var5 = var4.build();
         long var6 = Util.getMillis();
         this.status = Component.translatable("optimizeWorld.stage.upgrading");

         while (this.running) {
            boolean var8 = false;
            float var9 = 0.0F;

            for (ResourceKey<Level> var11 : this.levels) {
               ListIterator<ChunkPos> var12 = var3.get(var11);
               ChunkStorage var13 = var5.get(var11);
               if (var12.hasNext()) {
                  ChunkPos var14 = var12.next();
                  boolean var15 = false;

                  try {
                     CompoundTag var16 = var13.read(var14).join().orElse(null);
                     if (var16 != null) {
                        int var17 = ChunkStorage.getVersion(var16);
                        ChunkGenerator var18 = this.dimensions.getOrThrow(Registries.levelToLevelStem(var11)).generator();
                        CompoundTag var19 = var13.upgradeChunkTag(var11, () -> this.overworldDataStorage, var16, var18.getTypeNameForDataFixer());
                        ChunkPos var20 = new ChunkPos(var19.getInt("xPos"), var19.getInt("zPos"));
                        if (!var20.equals(var14)) {
                           LOGGER.warn("Chunk {} has invalid position {}", var14, var20);
                        }

                        boolean var21 = var17 < SharedConstants.getCurrentVersion().getDataVersion().getVersion();
                        if (this.eraseCache) {
                           var21 = var21 || var19.contains("Heightmaps");
                           var19.remove("Heightmaps");
                           var21 = var21 || var19.contains("isLightOn");
                           var19.remove("isLightOn");
                           ListTag var22 = var19.getList("sections", 10);

                           for (int var23 = 0; var23 < var22.size(); var23++) {
                              CompoundTag var24 = var22.getCompound(var23);
                              var21 = var21 || var24.contains("BlockLight");
                              var24.remove("BlockLight");
                              var21 = var21 || var24.contains("SkyLight");
                              var24.remove("SkyLight");
                           }
                        }

                        if (var21) {
                           var13.write(var14, var19);
                           var15 = true;
                        }
                     }
                  } catch (ReportedException | CompletionException var16) {
                     Throwable var17 = var16.getCause();
                     if (!(var17 instanceof IOException)) {
                        throw var16;
                     }

                     LOGGER.error("Error upgrading chunk {}", var14, var17);
                  }

                  if (var15) {
                     this.converted++;
                  } else {
                     this.skipped++;
                  }

                  var8 = true;
               }

               float var14 = var12.nextIndex() / var2;
               this.progressMap.put(var11, var14);
               var9 += var14;
            }

            this.progress = var9;
            if (!var8) {
               this.running = false;
            }
         }

         this.status = Component.translatable("optimizeWorld.stage.finished");

         for (ChunkStorage var9 : var5.values()) {
            try {
               var9.close();
            } catch (IOException var10) {
               LOGGER.error("Error upgrading chunk", var10);
            }
         }

         this.overworldDataStorage.save();
         var6 = Util.getMillis() - var6;
         LOGGER.info("World optimizaton finished after {} ms", var6);
         this.finished = true;
      }
   }

   private List<ChunkPos> getAllChunkPos(ResourceKey<Level> param0) {
      File var2 = this.levelStorage.getDimensionPath(param0).toFile();
      File var3 = new File(var2, "region");
      File[] var4 = var3.listFiles((l_param0, l_param1) -> l_param1.endsWith(".mca"));
      if (var4 == null) {
         return ImmutableList.of();
      }

      List<ChunkPos> var5 = Lists.newArrayList();

      for (File var9 : var4) {
         Matcher var10 = REGEX.matcher(var9.getName());
         if (var10.matches()) {
            int var11 = Integer.parseInt(var10.group(1)) << 5;
            int var12 = Integer.parseInt(var10.group(2)) << 5;

            try (RegionFile var13 = new RegionFile(var9.toPath(), var3.toPath(), true)) {
               for (int var14 = 0; var14 < 32; var14++) {
                  for (int var15 = 0; var15 < 32; var15++) {
                     ChunkPos var16 = new ChunkPos(var14 + var11, var15 + var12);
                     if (var13.doesChunkExist(var16)) {
                        var5.add(var16);
                     }
                  }
               }
            } catch (Throwable var19) {
            }
         }
      }

      return var5;
   }

   public boolean isFinished() {
      return this.finished;
   }

   public Set<ResourceKey<Level>> levels() {
      return this.levels;
   }

   public float dimensionProgress(ResourceKey<Level> param0) {
      return this.progressMap.getFloat(param0);
   }

   public float getProgress() {
      return this.progress;
   }

   public int getTotalChunks() {
      return this.totalChunks;
   }

   public int getConverted() {
      return this.converted;
   }

   public int getSkipped() {
      return this.skipped;
   }

   public Component getStatus() {
      return this.status;
   }
}

package net.minecraft.world.level.levelgen.structure;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

public class StructureCheck {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int NO_STRUCTURE = -1;
   private final ChunkScanAccess storageAccess;
   private final RegistryAccess registryAccess;
   private final Registry<Biome> biomes;
   private final Registry<Structure> structureConfigs;
   private final StructureTemplateManager structureTemplateManager;
   private final ResourceKey<Level> dimension;
   private final ChunkGenerator chunkGenerator;
   private final RandomState randomState;
   private final LevelHeightAccessor heightAccessor;
   private final BiomeSource biomeSource;
   private final long seed;
   private final DataFixer fixerUpper;
   private final Long2ObjectMap<Object2IntMap<Structure>> loadedChunks = new Long2ObjectOpenHashMap<>();
   private final Map<Structure, Long2BooleanMap> featureChecks = new HashMap<>();

   public StructureCheck(
      ChunkScanAccess param0,
      RegistryAccess param1,
      StructureTemplateManager param2,
      ResourceKey<Level> param3,
      ChunkGenerator param4,
      RandomState param5,
      LevelHeightAccessor param6,
      BiomeSource param7,
      long param8,
      DataFixer param10
   ) {
      this.storageAccess = param0;
      this.registryAccess = param1;
      this.structureTemplateManager = param2;
      this.dimension = param3;
      this.chunkGenerator = param4;
      this.randomState = param5;
      this.heightAccessor = param6;
      this.biomeSource = param7;
      this.seed = param8;
      this.fixerUpper = param10;
      this.biomes = param1.registryOrThrow(Registries.BIOME);
      this.structureConfigs = param1.registryOrThrow(Registries.STRUCTURE);
   }

   public StructureCheckResult checkStart(ChunkPos param0, Structure param1, boolean param2) {
      long var4 = param0.toLong();
      Object2IntMap<Structure> var6 = this.loadedChunks.get(var4);
      if (var6 != null) {
         return this.checkStructureInfo(var6, param1, param2);
      }

      StructureCheckResult var7 = this.tryLoadFromStorage(param0, param1, param2, var4);
      if (var7 != null) {
         return var7;
      }

      boolean var8 = this.featureChecks
         .computeIfAbsent(param1, l_param0 -> new Long2BooleanOpenHashMap())
         .computeIfAbsent(var4, l_param2 -> this.canCreateStructure(param0, param1));
      return !var8 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.CHUNK_LOAD_NEEDED;
   }

   private boolean canCreateStructure(ChunkPos param0, Structure param1) {
      return param1.findValidGenerationPoint(
            new Structure.GenerationContext(
               this.registryAccess,
               this.chunkGenerator,
               this.biomeSource,
               this.randomState,
               this.structureTemplateManager,
               this.seed,
               param0,
               this.heightAccessor,
               param1.biomes()::contains
            )
         )
         .isPresent();
   }

   @Nullable
   private StructureCheckResult tryLoadFromStorage(ChunkPos param0, Structure param1, boolean param2, long param3) {
      CollectFields var6 = new CollectFields(
         new FieldSelector(IntTag.TYPE, "DataVersion"),
         new FieldSelector("Level", "Structures", CompoundTag.TYPE, "Starts"),
         new FieldSelector("structures", CompoundTag.TYPE, "starts")
      );

      try {
         this.storageAccess.scanChunk(param0, var6).join();
      } catch (Exception var7) {
         LOGGER.warn("Failed to read chunk {}", param0, var7);
         return StructureCheckResult.CHUNK_LOAD_NEEDED;
      }

      if (!(var6.getResult() instanceof CompoundTag var8)) {
         return null;
      } else {
         int var9 = ChunkStorage.getVersion(var8);
         if (var9 <= 1493) {
            return StructureCheckResult.CHUNK_LOAD_NEEDED;
         }

         ChunkStorage.injectDatafixingContext(var8, this.dimension, this.chunkGenerator.getTypeNameForDataFixer());

         CompoundTag var10;
         try {
            var10 = DataFixTypes.CHUNK.updateToCurrentVersion(this.fixerUpper, var8, var9);
         } catch (Exception var11) {
            LOGGER.warn("Failed to partially datafix chunk {}", param0, var11);
            return StructureCheckResult.CHUNK_LOAD_NEEDED;
         }

         Object2IntMap<Structure> var11 = this.loadStructures(var10);
         if (var11 == null) {
            return null;
         }

         this.storeFullResults(param3, var11);
         return this.checkStructureInfo(var11, param1, param2);
      }
   }

   @Nullable
   private Object2IntMap<Structure> loadStructures(CompoundTag param0) {
      if (!param0.contains("structures", 10)) {
         return null;
      }

      CompoundTag var2 = param0.getCompound("structures");
      if (!var2.contains("starts", 10)) {
         return null;
      }

      CompoundTag var3 = var2.getCompound("starts");
      if (var3.isEmpty()) {
         return Object2IntMaps.emptyMap();
      }

      Object2IntMap<Structure> var4 = new Object2IntOpenHashMap<>();
      Registry<Structure> var5 = this.registryAccess.registryOrThrow(Registries.STRUCTURE);

      for (String var7 : var3.getAllKeys()) {
         ResourceLocation var8 = ResourceLocation.tryParse(var7);
         if (var8 != null) {
            Structure var9 = var5.get(var8);
            if (var9 != null) {
               CompoundTag var10 = var3.getCompound(var7);
               if (!var10.isEmpty()) {
                  String var11 = var10.getString("id");
                  if (!"INVALID".equals(var11)) {
                     int var12 = var10.getInt("references");
                     var4.put(var9, var12);
                  }
               }
            }
         }
      }

      return var4;
   }

   private static Object2IntMap<Structure> deduplicateEmptyMap(Object2IntMap<Structure> param0) {
      return param0.isEmpty() ? Object2IntMaps.emptyMap() : param0;
   }

   private StructureCheckResult checkStructureInfo(Object2IntMap<Structure> param0, Structure param1, boolean param2) {
      int var4 = param0.getOrDefault(param1, -1);
      return var4 == -1 || param2 && var4 != 0 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.START_PRESENT;
   }

   public void onStructureLoad(ChunkPos param0, Map<Structure, StructureStart> param1) {
      long var3 = param0.toLong();
      Object2IntMap<Structure> var5 = new Object2IntOpenHashMap<>();
      param1.forEach((l_param1, l_param2) -> {
         if (l_param2.isValid()) {
            var5.put(l_param1, l_param2.getReferences());
         }
      });
      this.storeFullResults(var3, var5);
   }

   private void storeFullResults(long param0, Object2IntMap<Structure> param2) {
      this.loadedChunks.put(param0, deduplicateEmptyMap(param2));
      this.featureChecks.values().forEach(l_param2 -> l_param2.remove(param0));
   }

   public void incrementReference(ChunkPos param0, Structure param1) {
      this.loadedChunks.compute(param0.toLong(), (l_param1, l_param2) -> {
         if (l_param2 == null || l_param2.isEmpty()) {
            l_param2 = new Object2IntOpenHashMap<>();
         }

         l_param2.computeInt(param1, (l2_param0, l2_param1) -> l2_param1 == null ? 1 : l2_param1 + 1);
         return l_param2;
      });
   }
}

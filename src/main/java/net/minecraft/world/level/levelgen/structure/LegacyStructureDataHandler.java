package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class LegacyStructureDataHandler {
   private static final Map<String, String> CURRENT_TO_LEGACY_MAP = Util.make(Maps.newHashMap(), param0 -> {
      param0.put("Village", "Village");
      param0.put("Mineshaft", "Mineshaft");
      param0.put("Mansion", "Mansion");
      param0.put("Igloo", "Temple");
      param0.put("Desert_Pyramid", "Temple");
      param0.put("Jungle_Pyramid", "Temple");
      param0.put("Swamp_Hut", "Temple");
      param0.put("Stronghold", "Stronghold");
      param0.put("Monument", "Monument");
      param0.put("Fortress", "Fortress");
      param0.put("EndCity", "EndCity");
   });
   private static final Map<String, String> LEGACY_TO_CURRENT_MAP = Util.make(Maps.newHashMap(), param0 -> {
      param0.put("Iglu", "Igloo");
      param0.put("TeDP", "Desert_Pyramid");
      param0.put("TeJP", "Jungle_Pyramid");
      param0.put("TeSH", "Swamp_Hut");
   });
   private static final Set<String> OLD_STRUCTURE_REGISTRY_KEYS = Set.of(
      "pillager_outpost",
      "mineshaft",
      "mansion",
      "jungle_pyramid",
      "desert_pyramid",
      "igloo",
      "ruined_portal",
      "shipwreck",
      "swamp_hut",
      "stronghold",
      "monument",
      "ocean_ruin",
      "fortress",
      "endcity",
      "buried_treasure",
      "village",
      "nether_fossil",
      "bastion_remnant"
   );
   private final boolean hasLegacyData;
   private final Map<String, Long2ObjectMap<CompoundTag>> dataMap = Maps.newHashMap();
   private final Map<String, StructureFeatureIndexSavedData> indexMap = Maps.newHashMap();
   private final List<String> legacyKeys;
   private final List<String> currentKeys;

   public LegacyStructureDataHandler(@Nullable DimensionDataStorage param0, List<String> param1, List<String> param2) {
      this.legacyKeys = param1;
      this.currentKeys = param2;
      this.populateCaches(param0);
      boolean var4 = false;

      for (String var6 : this.currentKeys) {
         var4 |= this.dataMap.get(var6) != null;
      }

      this.hasLegacyData = var4;
   }

   public void removeIndex(long param0) {
      for (String var4 : this.legacyKeys) {
         StructureFeatureIndexSavedData var5 = this.indexMap.get(var4);
         if (var5 != null && var5.hasUnhandledIndex(param0)) {
            var5.removeIndex(param0);
            var5.setDirty();
         }
      }
   }

   public CompoundTag updateFromLegacy(CompoundTag param0) {
      CompoundTag var2 = param0.getCompound("Level");
      ChunkPos var3 = new ChunkPos(var2.getInt("xPos"), var2.getInt("zPos"));
      if (this.isUnhandledStructureStart(var3.x, var3.z)) {
         param0 = this.updateStructureStart(param0, var3);
      }

      CompoundTag var4 = var2.getCompound("Structures");
      CompoundTag var5 = var4.getCompound("References");

      for (String var7 : this.currentKeys) {
         boolean var8 = OLD_STRUCTURE_REGISTRY_KEYS.contains(var7.toLowerCase(Locale.ROOT));
         if (!var5.contains(var7, 12) && var8) {
            int var9 = 8;
            LongList var10 = new LongArrayList();

            for (int var11 = var3.x - 8; var11 <= var3.x + 8; var11++) {
               for (int var12 = var3.z - 8; var12 <= var3.z + 8; var12++) {
                  if (this.hasLegacyStart(var11, var12, var7)) {
                     var10.add(ChunkPos.asLong(var11, var12));
                  }
               }
            }

            var5.putLongArray(var7, var10);
         }
      }

      var4.put("References", var5);
      var2.put("Structures", var4);
      param0.put("Level", var2);
      return param0;
   }

   private boolean hasLegacyStart(int param0, int param1, String param2) {
      return !this.hasLegacyData
         ? false
         : this.dataMap.get(param2) != null && this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(param2)).hasStartIndex(ChunkPos.asLong(param0, param1));
   }

   private boolean isUnhandledStructureStart(int param0, int param1) {
      if (!this.hasLegacyData) {
         return false;
      }

      for (String var4 : this.currentKeys) {
         if (this.dataMap.get(var4) != null && this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(var4)).hasUnhandledIndex(ChunkPos.asLong(param0, param1))) {
            return true;
         }
      }

      return false;
   }

   private CompoundTag updateStructureStart(CompoundTag param0, ChunkPos param1) {
      CompoundTag var3 = param0.getCompound("Level");
      CompoundTag var4 = var3.getCompound("Structures");
      CompoundTag var5 = var4.getCompound("Starts");

      for (String var7 : this.currentKeys) {
         Long2ObjectMap<CompoundTag> var8 = this.dataMap.get(var7);
         if (var8 != null) {
            long var9 = param1.toLong();
            if (this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(var7)).hasUnhandledIndex(var9)) {
               CompoundTag var11 = var8.get(var9);
               if (var11 != null) {
                  var5.put(var7, var11);
               }
            }
         }
      }

      var4.put("Starts", var5);
      var3.put("Structures", var4);
      param0.put("Level", var3);
      return param0;
   }

   private void populateCaches(@Nullable DimensionDataStorage param0) {
      if (param0 != null) {
         for (String var3 : this.legacyKeys) {
            CompoundTag var4 = new CompoundTag();

            try {
               var4 = param0.readTagFromDisk(var3, 1493).getCompound("data").getCompound("Features");
               if (var4.isEmpty()) {
                  continue;
               }
            } catch (IOException var13) {
            }

            for (String var6 : var4.getAllKeys()) {
               CompoundTag var7 = var4.getCompound(var6);
               long var8 = ChunkPos.asLong(var7.getInt("ChunkX"), var7.getInt("ChunkZ"));
               ListTag var10 = var7.getList("Children", 10);
               if (!var10.isEmpty()) {
                  String var11 = var10.getCompound(0).getString("id");
                  String var12 = LEGACY_TO_CURRENT_MAP.get(var11);
                  if (var12 != null) {
                     var7.putString("id", var12);
                  }
               }

               String var11 = var7.getString("id");
               this.dataMap.computeIfAbsent(var11, l_param0 -> new Long2ObjectOpenHashMap<>()).put(var8, var7);
            }

            String var5 = var3 + "_index";
            StructureFeatureIndexSavedData var6 = param0.computeIfAbsent(StructureFeatureIndexSavedData::load, StructureFeatureIndexSavedData::new, var5);
            if (!var6.getAll().isEmpty()) {
               this.indexMap.put(var3, var6);
            } else {
               StructureFeatureIndexSavedData var7 = new StructureFeatureIndexSavedData();
               this.indexMap.put(var3, var7);

               for (String var9 : var4.getAllKeys()) {
                  CompoundTag var10 = var4.getCompound(var9);
                  var7.addIndex(ChunkPos.asLong(var10.getInt("ChunkX"), var10.getInt("ChunkZ")));
               }

               var7.setDirty();
            }
         }
      }
   }

   public static LegacyStructureDataHandler getLegacyStructureHandler(ResourceKey<Level> param0, @Nullable DimensionDataStorage param1) {
      if (param0 == Level.OVERWORLD) {
         return new LegacyStructureDataHandler(
            param1,
            ImmutableList.of("Monument", "Stronghold", "Village", "Mineshaft", "Temple", "Mansion"),
            ImmutableList.of("Village", "Mineshaft", "Mansion", "Igloo", "Desert_Pyramid", "Jungle_Pyramid", "Swamp_Hut", "Stronghold", "Monument")
         );
      } else if (param0 == Level.NETHER) {
         List<String> var2 = ImmutableList.of("Fortress");
         return new LegacyStructureDataHandler(param1, var2, var2);
      } else if (param0 == Level.END) {
         List<String> var2 = ImmutableList.of("EndCity");
         return new LegacyStructureDataHandler(param1, var2, var2);
      } else {
         throw new RuntimeException(String.format(Locale.ROOT, "Unknown dimension type : %s", param0));
      }
   }
}

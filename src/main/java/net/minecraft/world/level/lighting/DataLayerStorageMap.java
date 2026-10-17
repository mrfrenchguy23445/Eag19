package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.level.chunk.DataLayer;

public abstract class DataLayerStorageMap<M extends DataLayerStorageMap<M>> {
   private static final int CACHE_SIZE = 2;
   private final long[] lastSectionKeys = new long[2];
   private final DataLayer[] lastSections = new DataLayer[2];
   private boolean cacheEnabled;
   protected final Long2ObjectOpenHashMap<DataLayer> map;

   protected DataLayerStorageMap(Long2ObjectOpenHashMap<DataLayer> param0) {
      this.map = param0;
      this.clearCache();
      this.cacheEnabled = true;
   }

   public abstract M copy();

   public void copyDataLayer(long param0) {
      this.map.put(param0, this.map.get(param0).copy());
      this.clearCache();
   }

   public boolean hasLayer(long param0) {
      return this.map.containsKey(param0);
   }

   @Nullable
   public DataLayer getLayer(long param0) {
      if (this.cacheEnabled) {
         for (int var3 = 0; var3 < 2; var3++) {
            if (param0 == this.lastSectionKeys[var3]) {
               return this.lastSections[var3];
            }
         }
      }

      DataLayer var3 = this.map.get(param0);
      if (var3 == null) {
         return null;
      }

      if (this.cacheEnabled) {
         for (int var4 = 1; var4 > 0; var4--) {
            this.lastSectionKeys[var4] = this.lastSectionKeys[var4 - 1];
            this.lastSections[var4] = this.lastSections[var4 - 1];
         }

         this.lastSectionKeys[0] = param0;
         this.lastSections[0] = var3;
      }

      return var3;
   }

   @Nullable
   public DataLayer removeLayer(long param0) {
      return this.map.remove(param0);
   }

   public void setLayer(long param0, DataLayer param2) {
      this.map.put(param0, param2);
   }

   public void clearCache() {
      for (int var1 = 0; var1 < 2; var1++) {
         this.lastSectionKeys[var1] = Long.MAX_VALUE;
         this.lastSections[var1] = null;
      }
   }

   public void disableCache() {
      this.cacheEnabled = false;
   }
}

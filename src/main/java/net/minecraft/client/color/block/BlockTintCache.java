package net.minecraft.client.color.block;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

public class BlockTintCache {
   private static final int MAX_CACHE_ENTRIES = 256;
   private final ThreadLocal<BlockTintCache.LatestCacheInfo> latestChunkOnThread = ThreadLocal.withInitial(BlockTintCache.LatestCacheInfo::new);
   private final Long2ObjectLinkedOpenHashMap<BlockTintCache.CacheData> cache = new Long2ObjectLinkedOpenHashMap<>(256, 0.25F);
   private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
   private final ToIntFunction<BlockPos> source;

   public BlockTintCache(ToIntFunction<BlockPos> param0) {
      this.source = param0;
   }

   public int getColor(BlockPos param0) {
      int var2 = SectionPos.blockToSectionCoord(param0.getX());
      int var3 = SectionPos.blockToSectionCoord(param0.getZ());
      BlockTintCache.LatestCacheInfo var4 = this.latestChunkOnThread.get();
      if (var4.x != var2 || var4.z != var3 || var4.cache == null || var4.cache.isInvalidated()) {
         var4.x = var2;
         var4.z = var3;
         var4.cache = this.findOrCreateChunkCache(var2, var3);
      }

      int[] var5 = var4.cache.getLayer(param0.getY());
      int var6 = param0.getX() & 15;
      int var7 = param0.getZ() & 15;
      int var8 = var7 << 4 | var6;
      int var9 = var5[var8];
      if (var9 != -1) {
         return var9;
      }

      int var10 = this.source.applyAsInt(param0);
      var5[var8] = var10;
      return var10;
   }

   public void invalidateForChunk(int param0, int param1) {
      try {
         this.lock.writeLock().lock();

         for (int var3 = -1; var3 <= 1; var3++) {
            for (int var4 = -1; var4 <= 1; var4++) {
               long var5 = ChunkPos.asLong(param0 + var3, param1 + var4);
               BlockTintCache.CacheData var7 = this.cache.remove(var5);
               if (var7 != null) {
                  var7.invalidate();
               }
            }
         }
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   public void invalidateAll() {
      try {
         this.lock.writeLock().lock();
         this.cache.clear();
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   private BlockTintCache.CacheData findOrCreateChunkCache(int param0, int param1) {
      long var3 = ChunkPos.asLong(param0, param1);
      this.lock.readLock().lock();

      try {
         BlockTintCache.CacheData var5 = this.cache.get(var3);
         if (var5 != null) {
            return var5;
         }
      } finally {
         this.lock.readLock().unlock();
      }

      this.lock.writeLock().lock();

      try {
         BlockTintCache.CacheData var5 = this.cache.get(var3);
         if (var5 != null) {
            return var5;
         }

         BlockTintCache.CacheData var6 = new BlockTintCache.CacheData();
         if (this.cache.size() >= 256) {
            this.cache.removeFirst();
         }

         this.cache.put(var3, var6);
         return var6;
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   static class CacheData {
      private final Int2ObjectArrayMap<int[]> cache = new Int2ObjectArrayMap<>(16);
      private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
      private static final int BLOCKS_PER_LAYER = Mth.square(16);
      private volatile boolean invalidated;

      public int[] getLayer(int param0) {
         this.lock.readLock().lock();

         try {
            int[] var2 = this.cache.get(param0);
            if (var2 != null) {
               return var2;
            }
         } finally {
            this.lock.readLock().unlock();
         }

         this.lock.writeLock().lock();

         try {
            return this.cache.computeIfAbsent(param0, l_param0 -> this.allocateLayer());
         } finally {
            this.lock.writeLock().unlock();
         }
      }

      private int[] allocateLayer() {
         int[] var1 = new int[BLOCKS_PER_LAYER];
         Arrays.fill(var1, -1);
         return var1;
      }

      public boolean isInvalidated() {
         return this.invalidated;
      }

      public void invalidate() {
         this.invalidated = true;
      }
   }

   static class LatestCacheInfo {
      public int x = Integer.MIN_VALUE;
      public int z = Integer.MIN_VALUE;
      @Nullable
      BlockTintCache.CacheData cache;

      private LatestCacheInfo() {
      }
   }
}

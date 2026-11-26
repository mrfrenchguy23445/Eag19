package net.minecraft.world.level.chunk.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.util.ExceptionCollector;
import net.minecraft.world.level.ChunkPos;

public final class RegionFileStorage implements AutoCloseable {
   public static final String ANVIL_EXTENSION = ".mca";
   private static final int MAX_CACHE_SIZE = 256;
   private final Long2ObjectLinkedOpenHashMap<RegionFile> regionCache = new Long2ObjectLinkedOpenHashMap<>();
   private final Path folder;
   private final boolean sync;

   RegionFileStorage(Path param0, boolean param1) {
      this.folder = param0;
      this.sync = param1;
   }

   private RegionFile getRegionFile(ChunkPos param0) throws IOException {
      long var2 = ChunkPos.asLong(param0.getRegionX(), param0.getRegionZ());
      RegionFile var4 = this.regionCache.getAndMoveToFirst(var2);
      if (var4 != null) {
         return var4;
      }

      if (this.regionCache.size() >= 256) {
         this.regionCache.removeLast().close();
      }

      FileUtil.createDirectoriesSafe(this.folder);
      Path var5 = this.folder.resolve("r." + param0.getRegionX() + "." + param0.getRegionZ() + ".mca");
      RegionFile var6 = new RegionFile(var5, this.folder, this.sync);
      this.regionCache.putAndMoveToFirst(var2, var6);
      return var6;
   }

   @Nullable
   public CompoundTag read(ChunkPos param0) throws IOException {
      RegionFile var2 = this.getRegionFile(param0);

      try (DataInputStream var3 = var2.getChunkDataInputStream(param0)) {
         return var3 == null ? null : NbtIo.read(var3);
      }
   }

   public void scanChunk(ChunkPos param0, StreamTagVisitor param1) throws IOException {
      RegionFile var3 = this.getRegionFile(param0);

      try (DataInputStream var4 = var3.getChunkDataInputStream(param0)) {
         if (var4 != null) {
            NbtIo.parse(var4, param1);
         }
      }
   }

   protected void write(ChunkPos param0, @Nullable CompoundTag param1) throws IOException {
      RegionFile var3 = this.getRegionFile(param0);
      if (param1 == null) {
         var3.clear(param0);
      } else {
         try (DataOutputStream var4 = var3.getChunkDataOutputStream(param0)) {
            NbtIo.write(param1, var4);
         }
      }
   }

   @Override
   public void close() throws IOException {
      ExceptionCollector<IOException> var1 = new ExceptionCollector<>();

      for (RegionFile var3 : this.regionCache.values()) {
         try {
            var3.close();
         } catch (IOException var4) {
            var1.add(var4);
         }
      }

      var1.throwIfPresent();
   }

   public void flush() throws IOException {
      for (RegionFile var2 : this.regionCache.values()) {
         var2.flush();
      }
   }
}

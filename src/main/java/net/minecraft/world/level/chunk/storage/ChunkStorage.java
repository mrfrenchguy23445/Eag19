package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.LegacyStructureDataHandler;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class ChunkStorage implements AutoCloseable {
   public static final int LAST_MONOLYTH_STRUCTURE_DATA_VERSION = 1493;
   private final IOWorker worker;
   protected final DataFixer fixerUpper;
   @Nullable
   private volatile LegacyStructureDataHandler legacyStructureHandler;

   public ChunkStorage(Path param0, DataFixer param1, boolean param2) {
      this.fixerUpper = param1;
      this.worker = new IOWorker(param0, param2, "chunk");
   }

   public boolean isOldChunkAround(ChunkPos param0, int param1) {
      return this.worker.isOldChunkAround(param0, param1);
   }

   public CompoundTag upgradeChunkTag(
      ResourceKey<Level> param0, Supplier<DimensionDataStorage> param1, CompoundTag param2, Optional<ResourceKey<Codec<? extends ChunkGenerator>>> param3
   ) {
      int var5 = getVersion(param2);
      if (var5 < 1493) {
         param2 = DataFixTypes.CHUNK.update(this.fixerUpper, param2, var5, 1493);
         if (param2.getCompound("Level").getBoolean("hasLegacyStructureData")) {
            LegacyStructureDataHandler var6 = this.getLegacyStructureHandler(param0, param1);
            param2 = var6.updateFromLegacy(param2);
         }
      }

      injectDatafixingContext(param2, param0, param3);
      param2 = DataFixTypes.CHUNK.updateToCurrentVersion(this.fixerUpper, param2, Math.max(1493, var5));
      if (var5 < SharedConstants.getCurrentVersion().getDataVersion().getVersion()) {
         NbtUtils.addCurrentDataVersion(param2);
      }

      param2.remove("__context");
      return param2;
   }

   private LegacyStructureDataHandler getLegacyStructureHandler(ResourceKey<Level> param0, Supplier<DimensionDataStorage> param1) {
      LegacyStructureDataHandler var3 = this.legacyStructureHandler;
      if (var3 == null) {
         synchronized (this) {
            var3 = this.legacyStructureHandler;
            if (var3 == null) {
               this.legacyStructureHandler = var3 = LegacyStructureDataHandler.getLegacyStructureHandler(param0, param1.get());
            }
         }
      }

      return var3;
   }

   public static void injectDatafixingContext(CompoundTag param0, ResourceKey<Level> param1, Optional<ResourceKey<Codec<? extends ChunkGenerator>>> param2) {
      CompoundTag var3 = new CompoundTag();
      var3.putString("dimension", param1.location().toString());
      param2.ifPresent(l_param1 -> var3.putString("generator", l_param1.location().toString()));
      param0.put("__context", var3);
   }

   public static int getVersion(CompoundTag param0) {
      return NbtUtils.getDataVersion(param0, -1);
   }

   public CompletableFuture<Optional<CompoundTag>> read(ChunkPos param0) {
      return this.worker.loadAsync(param0);
   }

   public void write(ChunkPos param0, CompoundTag param1) {
      this.worker.store(param0, param1);
      if (this.legacyStructureHandler != null) {
         this.legacyStructureHandler.removeIndex(param0.toLong());
      }
   }

   public void flushWorker() {
      this.worker.synchronize(true).join();
   }

   @Override
   public void close() throws IOException {
      this.worker.close();
   }

   public ChunkScanAccess chunkScanner() {
      return this.worker;
   }
}

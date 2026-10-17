package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.slf4j.Logger;

public class ClientChunkCache extends ChunkSource {
   static final Logger LOGGER = LogUtils.getLogger();
   private final LevelChunk emptyChunk;
   private final LevelLightEngine lightEngine;
   volatile ClientChunkCache.Storage storage;
   final ClientLevel level;

   public ClientChunkCache(ClientLevel param0, int param1) {
      this.level = param0;
      this.emptyChunk = new EmptyLevelChunk(
         param0, new ChunkPos(0, 0), param0.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS)
      );
      this.lightEngine = new LevelLightEngine(this, true, param0.dimensionType().hasSkyLight());
      this.storage = new ClientChunkCache.Storage(calculateStorageRange(param1));
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   private static boolean isValidChunk(@Nullable LevelChunk param0, int param1, int param2) {
      if (param0 == null) {
         return false;
      }

      ChunkPos var3 = param0.getPos();
      return var3.x == param1 && var3.z == param2;
   }

   public void drop(int param0, int param1) {
      if (this.storage.inRange(param0, param1)) {
         int var3 = this.storage.getIndex(param0, param1);
         LevelChunk var4 = this.storage.getChunk(var3);
         if (isValidChunk(var4, param0, param1)) {
            this.storage.replace(var3, var4, null);
         }
      }
   }

   @Nullable
   public LevelChunk getChunk(int param0, int param1, ChunkStatus param2, boolean param3) {
      if (this.storage.inRange(param0, param1)) {
         LevelChunk var5 = this.storage.getChunk(this.storage.getIndex(param0, param1));
         if (isValidChunk(var5, param0, param1)) {
            return var5;
         }
      }

      return param3 ? this.emptyChunk : null;
   }

   @Override
   public BlockGetter getLevel() {
      return this.level;
   }

   public void replaceBiomes(int param0, int param1, FriendlyByteBuf param2) {
      if (!this.storage.inRange(param0, param1)) {
         LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", param0, param1);
      } else {
         int var4 = this.storage.getIndex(param0, param1);
         LevelChunk var5 = this.storage.chunks.get(var4);
         if (!isValidChunk(var5, param0, param1)) {
            LOGGER.warn("Ignoring chunk since it's not present: {}, {}", param0, param1);
         } else {
            var5.replaceBiomes(param2);
         }
      }
   }

   @Nullable
   public LevelChunk replaceWithPacketData(
      int param0, int param1, FriendlyByteBuf param2, CompoundTag param3, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> param4
   ) {
      if (!this.storage.inRange(param0, param1)) {
         LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", param0, param1);
         return null;
      }

      int var6 = this.storage.getIndex(param0, param1);
      LevelChunk var7 = this.storage.chunks.get(var6);
      ChunkPos var8 = new ChunkPos(param0, param1);
      if (!isValidChunk(var7, param0, param1)) {
         var7 = new LevelChunk(this.level, var8);
         var7.replaceWithPacketData(param2, param3, param4);
         this.storage.replace(var6, var7);
      } else {
         var7.replaceWithPacketData(param2, param3, param4);
      }

      this.level.onChunkLoaded(var8);
      return var7;
   }

   @Override
   public void tick(BooleanSupplier param0, boolean param1) {
   }

   public void updateViewCenter(int param0, int param1) {
      this.storage.viewCenterX = param0;
      this.storage.viewCenterZ = param1;
   }

   public void updateViewRadius(int param0) {
      int var2 = this.storage.chunkRadius;
      int var3 = calculateStorageRange(param0);
      if (var2 != var3) {
         ClientChunkCache.Storage var4 = new ClientChunkCache.Storage(var3);
         var4.viewCenterX = this.storage.viewCenterX;
         var4.viewCenterZ = this.storage.viewCenterZ;

         for (int var5 = 0; var5 < this.storage.chunks.length(); var5++) {
            LevelChunk var6 = this.storage.chunks.get(var5);
            if (var6 != null) {
               ChunkPos var7 = var6.getPos();
               if (var4.inRange(var7.x, var7.z)) {
                  var4.replace(var4.getIndex(var7.x, var7.z), var6);
               }
            }
         }

         this.storage = var4;
      }
   }

   private static int calculateStorageRange(int param0) {
      return Math.max(2, param0) + 3;
   }

   @Override
   public String gatherStats() {
      return this.storage.chunks.length() + ", " + this.getLoadedChunksCount();
   }

   @Override
   public int getLoadedChunksCount() {
      return this.storage.chunkCount;
   }

   @Override
   public void onLightUpdate(LightLayer param0, SectionPos param1) {
      Minecraft.getInstance().levelRenderer.setSectionDirty(param1.x(), param1.y(), param1.z());
   }

   final class Storage {
      final AtomicReferenceArray<LevelChunk> chunks;
      final int chunkRadius;
      private final int viewRange;
      volatile int viewCenterX;
      volatile int viewCenterZ;
      int chunkCount;

      Storage(int param1) {
         this.chunkRadius = param1;
         this.viewRange = param1 * 2 + 1;
         this.chunks = new AtomicReferenceArray<>(this.viewRange * this.viewRange);
      }

      int getIndex(int param0, int param1) {
         return Math.floorMod(param1, this.viewRange) * this.viewRange + Math.floorMod(param0, this.viewRange);
      }

      protected void replace(int param0, @Nullable LevelChunk param1) {
         LevelChunk var3 = this.chunks.getAndSet(param0, param1);
         if (var3 != null) {
            this.chunkCount--;
            ClientChunkCache.this.level.unload(var3);
         }

         if (param1 != null) {
            this.chunkCount++;
         }
      }

      protected LevelChunk replace(int param0, LevelChunk param1, @Nullable LevelChunk param2) {
         if (this.chunks.compareAndSet(param0, param1, param2) && param2 == null) {
            this.chunkCount--;
         }

         ClientChunkCache.this.level.unload(param1);
         return param1;
      }

      boolean inRange(int param0, int param1) {
         return Math.abs(param0 - this.viewCenterX) <= this.chunkRadius && Math.abs(param1 - this.viewCenterZ) <= this.chunkRadius;
      }

      @Nullable
      protected LevelChunk getChunk(int param0) {
         return this.chunks.get(param0);
      }

      private void dumpChunks(String param0) {
         try (FileOutputStream var2 = new FileOutputStream(param0)) {
            int var3 = ClientChunkCache.this.storage.chunkRadius;

            for (int var4 = this.viewCenterZ - var3; var4 <= this.viewCenterZ + var3; var4++) {
               for (int var5 = this.viewCenterX - var3; var5 <= this.viewCenterX + var3; var5++) {
                  LevelChunk var6 = ClientChunkCache.this.storage.chunks.get(ClientChunkCache.this.storage.getIndex(var5, var4));
                  if (var6 != null) {
                     ChunkPos var7 = var6.getPos();
                     var2.write((var7.x + "\t" + var7.z + "\t" + var6.isEmpty() + "\n").getBytes(StandardCharsets.UTF_8));
                  }
               }
            }
         } catch (IOException var2) {
            ClientChunkCache.LOGGER.error("Failed to dump chunks to file {}", param0, var2);
         }
      }
   }
}

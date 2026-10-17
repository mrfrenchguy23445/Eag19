package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import org.slf4j.Logger;

public class EntityStorage implements EntityPersistentStorage<Entity> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String ENTITIES_TAG = "Entities";
   private static final String POSITION_TAG = "Position";
   private final ServerLevel level;
   private final IOWorker worker;
   private final LongSet emptyChunks = new LongOpenHashSet();
   private final ProcessorMailbox<Runnable> entityDeserializerQueue;
   protected final DataFixer fixerUpper;

   public EntityStorage(ServerLevel param0, Path param1, DataFixer param2, boolean param3, Executor param4) {
      this.level = param0;
      this.fixerUpper = param2;
      this.entityDeserializerQueue = ProcessorMailbox.create(param4, "entity-deserializer");
      this.worker = new IOWorker(param1, param3, "entities");
   }

   @Override
   public CompletableFuture<ChunkEntities<Entity>> loadEntities(ChunkPos param0) {
      return this.emptyChunks.contains(param0.toLong())
         ? CompletableFuture.completedFuture(emptyChunk(param0))
         : this.worker.loadAsync(param0).thenApplyAsync(l_param1 -> {
            if (l_param1.isEmpty()) {
               this.emptyChunks.add(param0.toLong());
               return emptyChunk(param0);
            }

            try {
               ChunkPos l_var3 = readChunkPos(l_param1.get());
               if (!Objects.equals(param0, l_var3)) {
                  LOGGER.error("Chunk file at {} is in the wrong location. (Expected {}, got {})", param0, param0, l_var3);
               }
            } catch (Exception l_var3) {
               LOGGER.warn("Failed to parse chunk {} position info", param0, l_var3);
            }

            CompoundTag l_var3 = this.upgradeChunkTag(l_param1.get());
            ListTag l_var4 = l_var3.getList("Entities", 10);
            List<Entity> l_var5 = EntityType.loadEntitiesRecursive(l_var4, this.level).collect(ImmutableList.toImmutableList());
            return new ChunkEntities<>(param0, l_var5);
         }, this.entityDeserializerQueue::tell);
   }

   private static ChunkPos readChunkPos(CompoundTag param0) {
      int[] var1 = param0.getIntArray("Position");
      return new ChunkPos(var1[0], var1[1]);
   }

   private static void writeChunkPos(CompoundTag param0, ChunkPos param1) {
      param0.put("Position", new IntArrayTag(new int[]{param1.x, param1.z}));
   }

   private static ChunkEntities<Entity> emptyChunk(ChunkPos param0) {
      return new ChunkEntities<>(param0, ImmutableList.of());
   }

   @Override
   public void storeEntities(ChunkEntities<Entity> param0) {
      ChunkPos var2 = param0.getPos();
      if (param0.isEmpty()) {
         if (this.emptyChunks.add(var2.toLong())) {
            this.worker.store(var2, null);
         }
      } else {
         ListTag var3 = new ListTag();
         param0.getEntities().forEach(l_param1 -> {
            CompoundTag l_var2 = new CompoundTag();
            if (l_param1.save(l_var2)) {
               var3.add(l_var2);
            }
         });
         CompoundTag var4 = NbtUtils.addCurrentDataVersion(new CompoundTag());
         var4.put("Entities", var3);
         writeChunkPos(var4, var2);
         this.worker.store(var2, var4).exceptionally(l_param1 -> {
            LOGGER.error("Failed to store chunk {}", var2, l_param1);
            return null;
         });
         this.emptyChunks.remove(var2.toLong());
      }
   }

   @Override
   public void flush(boolean param0) {
      this.worker.synchronize(param0).join();
      this.entityDeserializerQueue.runAll();
   }

   private CompoundTag upgradeChunkTag(CompoundTag param0) {
      int var2 = NbtUtils.getDataVersion(param0, -1);
      return DataFixTypes.ENTITY_CHUNK.updateToCurrentVersion(this.fixerUpper, param0, var2);
   }

   @Override
   public void close() throws IOException {
      this.worker.close();
   }
}

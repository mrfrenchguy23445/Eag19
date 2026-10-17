package net.minecraft.world.level.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class PersistentEntitySectionManager<T extends EntityAccess> implements AutoCloseable {
   static final Logger LOGGER = LogUtils.getLogger();
   final Set<UUID> knownUuids = Sets.newHashSet();
   final LevelCallback<T> callbacks;
   private final EntityPersistentStorage<T> permanentStorage;
   private final EntityLookup<T> visibleEntityStorage;
   final EntitySectionStorage<T> sectionStorage;
   private final LevelEntityGetter<T> entityGetter;
   private final Long2ObjectMap<Visibility> chunkVisibility = new Long2ObjectOpenHashMap<>();
   private final Long2ObjectMap<PersistentEntitySectionManager.ChunkLoadStatus> chunkLoadStatuses = new Long2ObjectOpenHashMap<>();
   private final LongSet chunksToUnload = new LongOpenHashSet();
   private final Queue<ChunkEntities<T>> loadingInbox = Queues.newConcurrentLinkedQueue();

   public PersistentEntitySectionManager(Class<T> param0, LevelCallback<T> param1, EntityPersistentStorage<T> param2) {
      this.visibleEntityStorage = new EntityLookup<>();
      this.sectionStorage = new EntitySectionStorage<>(param0, this.chunkVisibility);
      this.chunkVisibility.defaultReturnValue(Visibility.HIDDEN);
      this.chunkLoadStatuses.defaultReturnValue(PersistentEntitySectionManager.ChunkLoadStatus.FRESH);
      this.callbacks = param1;
      this.permanentStorage = param2;
      this.entityGetter = new LevelEntityGetterAdapter<>(this.visibleEntityStorage, this.sectionStorage);
   }

   void removeSectionIfEmpty(long param0, EntitySection<T> param2) {
      if (param2.isEmpty()) {
         this.sectionStorage.remove(param0);
      }
   }

   private boolean addEntityUuid(T param0) {
      if (!this.knownUuids.add(param0.getUUID())) {
         LOGGER.warn("UUID of added entity already exists: {}", param0);
         return false;
      } else {
         return true;
      }
   }

   public boolean addNewEntity(T param0) {
      return this.addEntity(param0, false);
   }

   private boolean addEntity(T param0, boolean param1) {
      if (!this.addEntityUuid(param0)) {
         return false;
      }

      long var3 = SectionPos.asLong(param0.blockPosition());
      EntitySection<T> var5 = this.sectionStorage.getOrCreateSection(var3);
      var5.add(param0);
      param0.setLevelCallback(new PersistentEntitySectionManager.Callback(param0, var3, var5));
      if (!param1) {
         this.callbacks.onCreated(param0);
      }

      Visibility var6 = getEffectiveStatus(param0, var5.getStatus());
      if (var6.isAccessible()) {
         this.startTracking(param0);
      }

      if (var6.isTicking()) {
         this.startTicking(param0);
      }

      return true;
   }

   static <T extends EntityAccess> Visibility getEffectiveStatus(T param0, Visibility param1) {
      return param0.isAlwaysTicking() ? Visibility.TICKING : param1;
   }

   public void addLegacyChunkEntities(Stream<T> param0) {
      param0.forEach(l_param0 -> this.addEntity((T)l_param0, true));
   }

   public void addWorldGenChunkEntities(Stream<T> param0) {
      param0.forEach(l_param0 -> this.addEntity((T)l_param0, false));
   }

   void startTicking(T param0) {
      this.callbacks.onTickingStart(param0);
   }

   void stopTicking(T param0) {
      this.callbacks.onTickingEnd(param0);
   }

   void startTracking(T param0) {
      this.visibleEntityStorage.add(param0);
      this.callbacks.onTrackingStart(param0);
   }

   void stopTracking(T param0) {
      this.callbacks.onTrackingEnd(param0);
      this.visibleEntityStorage.remove(param0);
   }

   public void updateChunkStatus(ChunkPos param0, ChunkHolder.FullChunkStatus param1) {
      Visibility var3 = Visibility.fromFullChunkStatus(param1);
      this.updateChunkStatus(param0, var3);
   }

   public void updateChunkStatus(ChunkPos param0, Visibility param1) {
      long var3 = param0.toLong();
      if (param1 == Visibility.HIDDEN) {
         this.chunkVisibility.remove(var3);
         this.chunksToUnload.add(var3);
      } else {
         this.chunkVisibility.put(var3, param1);
         this.chunksToUnload.remove(var3);
         this.ensureChunkQueuedForLoad(var3);
      }

      this.sectionStorage.getExistingSectionsInChunk(var3).forEach(l_param1 -> {
         Visibility l_var3 = l_param1.updateChunkStatus(param1);
         boolean l_var4 = l_var3.isAccessible();
         boolean l_var5 = param1.isAccessible();
         boolean l_var6 = l_var3.isTicking();
         boolean l_var7 = param1.isTicking();
         if (l_var6 && !l_var7) {
            l_param1.getEntities().filter(l2_param0 -> !l2_param0.isAlwaysTicking()).forEach(this::stopTicking);
         }

         if (l_var4 && !l_var5) {
            l_param1.getEntities().filter(l2_param0 -> !l2_param0.isAlwaysTicking()).forEach(this::stopTracking);
         } else if (!l_var4 && l_var5) {
            l_param1.getEntities().filter(l2_param0 -> !l2_param0.isAlwaysTicking()).forEach(this::startTracking);
         }

         if (!l_var6 && l_var7) {
            l_param1.getEntities().filter(l2_param0 -> !l2_param0.isAlwaysTicking()).forEach(this::startTicking);
         }
      });
   }

   private void ensureChunkQueuedForLoad(long param0) {
      PersistentEntitySectionManager.ChunkLoadStatus var3 = this.chunkLoadStatuses.get(param0);
      if (var3 == PersistentEntitySectionManager.ChunkLoadStatus.FRESH) {
         this.requestChunkLoad(param0);
      }
   }

   private boolean storeChunkSections(long param0, Consumer<T> param2) {
      PersistentEntitySectionManager.ChunkLoadStatus var4 = this.chunkLoadStatuses.get(param0);
      if (var4 == PersistentEntitySectionManager.ChunkLoadStatus.PENDING) {
         return false;
      }

      List<T> var5 = this.sectionStorage
         .getExistingSectionsInChunk(param0)
         .flatMap(l_param0 -> l_param0.getEntities().filter(EntityAccess::shouldBeSaved))
         .collect(Collectors.toList());
      if (var5.isEmpty()) {
         if (var4 == PersistentEntitySectionManager.ChunkLoadStatus.LOADED) {
            this.permanentStorage.storeEntities(new ChunkEntities<>(new ChunkPos(param0), ImmutableList.of()));
         }

         return true;
      } else if (var4 == PersistentEntitySectionManager.ChunkLoadStatus.FRESH) {
         this.requestChunkLoad(param0);
         return false;
      } else {
         this.permanentStorage.storeEntities(new ChunkEntities<>(new ChunkPos(param0), var5));
         var5.forEach(param2);
         return true;
      }
   }

   private void requestChunkLoad(long param0) {
      this.chunkLoadStatuses.put(param0, PersistentEntitySectionManager.ChunkLoadStatus.PENDING);
      ChunkPos var3 = new ChunkPos(param0);
      this.permanentStorage.loadEntities(var3).thenAccept(this.loadingInbox::add).exceptionally(l_param1 -> {
         LOGGER.error("Failed to read chunk {}", var3, l_param1);
         return null;
      });
   }

   private boolean processChunkUnload(long param0) {
      boolean var3 = this.storeChunkSections(param0, l_param0 -> l_param0.getPassengersAndSelf().forEach(this::unloadEntity));
      if (!var3) {
         return false;
      }

      this.chunkLoadStatuses.remove(param0);
      return true;
   }

   private void unloadEntity(EntityAccess param0) {
      param0.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
      param0.setLevelCallback(EntityInLevelCallback.NULL);
   }

   private void processUnloads() {
      this.chunksToUnload.removeIf(l_param0 -> this.chunkVisibility.get(l_param0) != Visibility.HIDDEN ? true : this.processChunkUnload(l_param0));
   }

   private void processPendingLoads() {
      ChunkEntities<T> var1;
      while ((var1 = this.loadingInbox.poll()) != null) {
         var1.getEntities().forEach(l_param0 -> this.addEntity((T)l_param0, true));
         this.chunkLoadStatuses.put(var1.getPos().toLong(), PersistentEntitySectionManager.ChunkLoadStatus.LOADED);
      }
   }

   public void tick() {
      this.processPendingLoads();
      this.processUnloads();
   }

   private LongSet getAllChunksToSave() {
      LongSet var1 = this.sectionStorage.getAllChunksWithExistingSections();

      for (Entry<PersistentEntitySectionManager.ChunkLoadStatus> var3 : Long2ObjectMaps.fastIterable(this.chunkLoadStatuses)) {
         if (var3.getValue() == PersistentEntitySectionManager.ChunkLoadStatus.LOADED) {
            var1.add(var3.getLongKey());
         }
      }

      return var1;
   }

   public void autoSave() {
      this.getAllChunksToSave().forEach(l_param0 -> {
         boolean l_var3 = this.chunkVisibility.get(l_param0) == Visibility.HIDDEN;
         if (l_var3) {
            this.processChunkUnload(l_param0);
         } else {
            this.storeChunkSections(l_param0, l2_param0 -> {});
         }
      });
   }

   public void saveAll() {
      LongSet var1 = this.getAllChunksToSave();

      while (!var1.isEmpty()) {
         this.permanentStorage.flush(false);
         this.processPendingLoads();
         var1.removeIf(l_param0 -> {
            boolean l_var3 = this.chunkVisibility.get(l_param0) == Visibility.HIDDEN;
            return l_var3 ? this.processChunkUnload(l_param0) : this.storeChunkSections(l_param0, l2_param0 -> {});
         });
      }

      this.permanentStorage.flush(true);
   }

   @Override
   public void close() throws IOException {
      this.saveAll();
      this.permanentStorage.close();
   }

   public boolean isLoaded(UUID param0) {
      return this.knownUuids.contains(param0);
   }

   public LevelEntityGetter<T> getEntityGetter() {
      return this.entityGetter;
   }

   public boolean canPositionTick(BlockPos param0) {
      return this.chunkVisibility.get(ChunkPos.asLong(param0)).isTicking();
   }

   public boolean canPositionTick(ChunkPos param0) {
      return this.chunkVisibility.get(param0.toLong()).isTicking();
   }

   public boolean areEntitiesLoaded(long param0) {
      return this.chunkLoadStatuses.get(param0) == PersistentEntitySectionManager.ChunkLoadStatus.LOADED;
   }

   public void dumpSections(Writer param0) throws IOException {
      CsvOutput var2 = CsvOutput.builder()
         .addColumn("x")
         .addColumn("y")
         .addColumn("z")
         .addColumn("visibility")
         .addColumn("load_status")
         .addColumn("entity_count")
         .build(param0);
      this.sectionStorage.getAllChunksWithExistingSections().forEach(l_param1 -> {
         PersistentEntitySectionManager.ChunkLoadStatus l_var4 = this.chunkLoadStatuses.get(l_param1);
         this.sectionStorage.getExistingSectionPositionsInChunk(l_param1).forEach(l2_param2 -> {
            EntitySection<T> l2_var5 = this.sectionStorage.getSection(l2_param2);
            if (l2_var5 != null) {
               try {
                  var2.writeRow(SectionPos.x(l2_param2), SectionPos.y(l2_param2), SectionPos.z(l2_param2), l2_var5.getStatus(), l_var4, l2_var5.size());
               } catch (IOException l2_var6) {
                  throw new UncheckedIOException(l2_var6);
               }
            }
         });
      });
   }

   @VisibleForDebug
   public String gatherStats() {
      return this.knownUuids.size()
         + ","
         + this.visibleEntityStorage.count()
         + ","
         + this.sectionStorage.count()
         + ","
         + this.chunkLoadStatuses.size()
         + ","
         + this.chunkVisibility.size()
         + ","
         + this.loadingInbox.size()
         + ","
         + this.chunksToUnload.size();
   }

   class Callback implements EntityInLevelCallback {
      private final T entity;
      private long currentSectionKey;
      private EntitySection<T> currentSection;

      Callback(T param1, long param2, EntitySection<T> param4) {
         this.entity = param1;
         this.currentSectionKey = param2;
         this.currentSection = param4;
      }

      @Override
      public void onMove() {
         BlockPos var1 = this.entity.blockPosition();
         long var2 = SectionPos.asLong(var1);
         if (var2 != this.currentSectionKey) {
            Visibility var4 = this.currentSection.getStatus();
            if (!this.currentSection.remove(this.entity)) {
               PersistentEntitySectionManager.LOGGER
                  .warn("Entity {} wasn't found in section {} (moving to {})", this.entity, SectionPos.of(this.currentSectionKey), var2);
            }

            PersistentEntitySectionManager.this.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
            EntitySection<T> var5 = PersistentEntitySectionManager.this.sectionStorage.getOrCreateSection(var2);
            var5.add(this.entity);
            this.currentSection = var5;
            this.currentSectionKey = var2;
            this.updateStatus(var4, var5.getStatus());
         }
      }

      private void updateStatus(Visibility param0, Visibility param1) {
         Visibility var3 = PersistentEntitySectionManager.getEffectiveStatus(this.entity, param0);
         Visibility var4 = PersistentEntitySectionManager.getEffectiveStatus(this.entity, param1);
         if (var3 == var4) {
            if (var4.isAccessible()) {
               PersistentEntitySectionManager.this.callbacks.onSectionChange(this.entity);
            }
         } else {
            boolean var5 = var3.isAccessible();
            boolean var6 = var4.isAccessible();
            if (var5 && !var6) {
               PersistentEntitySectionManager.this.stopTracking(this.entity);
            } else if (!var5 && var6) {
               PersistentEntitySectionManager.this.startTracking(this.entity);
            }

            boolean var7 = var3.isTicking();
            boolean var8 = var4.isTicking();
            if (var7 && !var8) {
               PersistentEntitySectionManager.this.stopTicking(this.entity);
            } else if (!var7 && var8) {
               PersistentEntitySectionManager.this.startTicking(this.entity);
            }

            if (var6) {
               PersistentEntitySectionManager.this.callbacks.onSectionChange(this.entity);
            }
         }
      }

      @Override
      public void onRemove(Entity.RemovalReason param0) {
         if (!this.currentSection.remove(this.entity)) {
            PersistentEntitySectionManager.LOGGER
               .warn("Entity {} wasn't found in section {} (destroying due to {})", this.entity, SectionPos.of(this.currentSectionKey), param0);
         }

         Visibility var2 = PersistentEntitySectionManager.getEffectiveStatus(this.entity, this.currentSection.getStatus());
         if (var2.isTicking()) {
            PersistentEntitySectionManager.this.stopTicking(this.entity);
         }

         if (var2.isAccessible()) {
            PersistentEntitySectionManager.this.stopTracking(this.entity);
         }

         if (param0.shouldDestroy()) {
            PersistentEntitySectionManager.this.callbacks.onDestroyed(this.entity);
         }

         PersistentEntitySectionManager.this.knownUuids.remove(this.entity.getUUID());
         this.entity.setLevelCallback(NULL);
         PersistentEntitySectionManager.this.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
      }
   }

   enum ChunkLoadStatus {
      FRESH,
      PENDING,
      LOADED;
   }
}

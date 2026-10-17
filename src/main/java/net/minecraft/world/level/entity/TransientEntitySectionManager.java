package net.minecraft.world.level.entity;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class TransientEntitySectionManager<T extends EntityAccess> {
   static final Logger LOGGER = LogUtils.getLogger();
   final LevelCallback<T> callbacks;
   final EntityLookup<T> entityStorage;
   final EntitySectionStorage<T> sectionStorage;
   private final LongSet tickingChunks = new LongOpenHashSet();
   private final LevelEntityGetter<T> entityGetter;

   public TransientEntitySectionManager(Class<T> param0, LevelCallback<T> param1) {
      this.entityStorage = new EntityLookup<>();
      this.sectionStorage = new EntitySectionStorage<>(param0, l_param0 -> this.tickingChunks.contains(l_param0) ? Visibility.TICKING : Visibility.TRACKED);
      this.callbacks = param1;
      this.entityGetter = new LevelEntityGetterAdapter<>(this.entityStorage, this.sectionStorage);
   }

   public void startTicking(ChunkPos param0) {
      long var2 = param0.toLong();
      this.tickingChunks.add(var2);
      this.sectionStorage.getExistingSectionsInChunk(var2).forEach(l_param0 -> {
         Visibility l_var2 = l_param0.updateChunkStatus(Visibility.TICKING);
         if (!l_var2.isTicking()) {
            l_param0.getEntities().filter(l2_param0 -> !l2_param0.isAlwaysTicking()).forEach(this.callbacks::onTickingStart);
         }
      });
   }

   public void stopTicking(ChunkPos param0) {
      long var2 = param0.toLong();
      this.tickingChunks.remove(var2);
      this.sectionStorage.getExistingSectionsInChunk(var2).forEach(l_param0 -> {
         Visibility l_var2 = l_param0.updateChunkStatus(Visibility.TRACKED);
         if (l_var2.isTicking()) {
            l_param0.getEntities().filter(l2_param0 -> !l2_param0.isAlwaysTicking()).forEach(this.callbacks::onTickingEnd);
         }
      });
   }

   public LevelEntityGetter<T> getEntityGetter() {
      return this.entityGetter;
   }

   public void addEntity(T param0) {
      this.entityStorage.add(param0);
      long var2 = SectionPos.asLong(param0.blockPosition());
      EntitySection<T> var4 = this.sectionStorage.getOrCreateSection(var2);
      var4.add(param0);
      param0.setLevelCallback(new TransientEntitySectionManager.Callback(param0, var2, var4));
      this.callbacks.onCreated(param0);
      this.callbacks.onTrackingStart(param0);
      if (param0.isAlwaysTicking() || var4.getStatus().isTicking()) {
         this.callbacks.onTickingStart(param0);
      }
   }

   @VisibleForDebug
   public int count() {
      return this.entityStorage.count();
   }

   void removeSectionIfEmpty(long param0, EntitySection<T> param2) {
      if (param2.isEmpty()) {
         this.sectionStorage.remove(param0);
      }
   }

   @VisibleForDebug
   public String gatherStats() {
      return this.entityStorage.count() + "," + this.sectionStorage.count() + "," + this.tickingChunks.size();
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
               TransientEntitySectionManager.LOGGER
                  .warn("Entity {} wasn't found in section {} (moving to {})", this.entity, SectionPos.of(this.currentSectionKey), var2);
            }

            TransientEntitySectionManager.this.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
            EntitySection<T> var5 = TransientEntitySectionManager.this.sectionStorage.getOrCreateSection(var2);
            var5.add(this.entity);
            this.currentSection = var5;
            this.currentSectionKey = var2;
            TransientEntitySectionManager.this.callbacks.onSectionChange(this.entity);
            if (!this.entity.isAlwaysTicking()) {
               boolean var6 = var4.isTicking();
               boolean var7 = var5.getStatus().isTicking();
               if (var6 && !var7) {
                  TransientEntitySectionManager.this.callbacks.onTickingEnd(this.entity);
               } else if (!var6 && var7) {
                  TransientEntitySectionManager.this.callbacks.onTickingStart(this.entity);
               }
            }
         }
      }

      @Override
      public void onRemove(Entity.RemovalReason param0) {
         if (!this.currentSection.remove(this.entity)) {
            TransientEntitySectionManager.LOGGER
               .warn("Entity {} wasn't found in section {} (destroying due to {})", this.entity, SectionPos.of(this.currentSectionKey), param0);
         }

         Visibility var2 = this.currentSection.getStatus();
         if (var2.isTicking() || this.entity.isAlwaysTicking()) {
            TransientEntitySectionManager.this.callbacks.onTickingEnd(this.entity);
         }

         TransientEntitySectionManager.this.callbacks.onTrackingEnd(this.entity);
         TransientEntitySectionManager.this.callbacks.onDestroyed(this.entity);
         TransientEntitySectionManager.this.entityStorage.remove(this.entity);
         this.entity.setLevelCallback(NULL);
         TransientEntitySectionManager.this.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
      }
   }
}

package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.SectionTracker;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public abstract class LayerLightSectionStorage<M extends DataLayerStorageMap<M>> extends SectionTracker {
   protected static final int LIGHT_AND_DATA = 0;
   protected static final int LIGHT_ONLY = 1;
   protected static final int EMPTY = 2;
   protected static final DataLayer EMPTY_DATA = new DataLayer();
   private static final Direction[] DIRECTIONS = Direction.values();
   private final LightLayer layer;
   private final LightChunkGetter chunkSource;
   protected final LongSet dataSectionSet = new LongOpenHashSet();
   protected final LongSet toMarkNoData = new LongOpenHashSet();
   protected final LongSet toMarkData = new LongOpenHashSet();
   protected volatile M visibleSectionData;
   protected final M updatingSectionData;
   protected final LongSet changedSections = new LongOpenHashSet();
   protected final LongSet sectionsAffectedByLightUpdates = new LongOpenHashSet();
   protected final Long2ObjectMap<DataLayer> queuedSections = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
   private final LongSet untrustedSections = new LongOpenHashSet();
   private final LongSet columnsToRetainQueuedDataFor = new LongOpenHashSet();
   private final LongSet toRemove = new LongOpenHashSet();
   protected volatile boolean hasToRemove;

   protected LayerLightSectionStorage(LightLayer param0, LightChunkGetter param1, M param2) {
      super(3, 16, 256);
      this.layer = param0;
      this.chunkSource = param1;
      this.updatingSectionData = param2;
      this.visibleSectionData = param2.copy();
      this.visibleSectionData.disableCache();
   }

   protected boolean storingLightForSection(long param0) {
      return this.getDataLayer(param0, true) != null;
   }

   @Nullable
   protected DataLayer getDataLayer(long param0, boolean param2) {
      return this.getDataLayer(param2 ? this.updatingSectionData : this.visibleSectionData, param0);
   }

   @Nullable
   protected DataLayer getDataLayer(M param0, long param1) {
      return param0.getLayer(param1);
   }

   @Nullable
   public DataLayer getDataLayerData(long param0) {
      DataLayer var3 = this.queuedSections.get(param0);
      return var3 != null ? var3 : this.getDataLayer(param0, false);
   }

   protected abstract int getLightValue(long var1);

   protected int getStoredLevel(long param0) {
      long var3 = SectionPos.blockToSection(param0);
      DataLayer var5 = this.getDataLayer(var3, true);
      return var5.get(
         SectionPos.sectionRelative(BlockPos.getX(param0)),
         SectionPos.sectionRelative(BlockPos.getY(param0)),
         SectionPos.sectionRelative(BlockPos.getZ(param0))
      );
   }

   protected void setStoredLevel(long param0, int param2) {
      long var4 = SectionPos.blockToSection(param0);
      if (this.changedSections.add(var4)) {
         this.updatingSectionData.copyDataLayer(var4);
      }

      DataLayer var6 = this.getDataLayer(var4, true);
      var6.set(
         SectionPos.sectionRelative(BlockPos.getX(param0)),
         SectionPos.sectionRelative(BlockPos.getY(param0)),
         SectionPos.sectionRelative(BlockPos.getZ(param0)),
         param2
      );
      SectionPos.aroundAndAtBlockPos(param0, this.sectionsAffectedByLightUpdates::add);
   }

   @Override
   protected int getLevel(long param0) {
      if (param0 == Long.MAX_VALUE) {
         return 2;
      } else if (this.dataSectionSet.contains(param0)) {
         return 0;
      } else {
         return !this.toRemove.contains(param0) && this.updatingSectionData.hasLayer(param0) ? 1 : 2;
      }
   }

   @Override
   protected int getLevelFromSource(long param0) {
      if (this.toMarkNoData.contains(param0)) {
         return 2;
      } else {
         return !this.dataSectionSet.contains(param0) && !this.toMarkData.contains(param0) ? 2 : 0;
      }
   }

   @Override
   protected void setLevel(long param0, int param2) {
      int var4 = this.getLevel(param0);
      if (var4 != 0 && param2 == 0) {
         this.dataSectionSet.add(param0);
         this.toMarkData.remove(param0);
      }

      if (var4 == 0 && param2 != 0) {
         this.dataSectionSet.remove(param0);
         this.toMarkNoData.remove(param0);
      }

      if (var4 >= 2 && param2 != 2) {
         if (this.toRemove.contains(param0)) {
            this.toRemove.remove(param0);
         } else {
            this.updatingSectionData.setLayer(param0, this.createDataLayer(param0));
            this.changedSections.add(param0);
            this.onNodeAdded(param0);
            int var5 = SectionPos.x(param0);
            int var6 = SectionPos.y(param0);
            int var7 = SectionPos.z(param0);

            for (int var8 = -1; var8 <= 1; var8++) {
               for (int var9 = -1; var9 <= 1; var9++) {
                  for (int var10 = -1; var10 <= 1; var10++) {
                     this.sectionsAffectedByLightUpdates.add(SectionPos.asLong(var5 + var9, var6 + var10, var7 + var8));
                  }
               }
            }
         }
      }

      if (var4 != 2 && param2 >= 2) {
         this.toRemove.add(param0);
      }

      this.hasToRemove = !this.toRemove.isEmpty();
   }

   protected DataLayer createDataLayer(long param0) {
      DataLayer var3 = this.queuedSections.get(param0);
      return var3 != null ? var3 : new DataLayer();
   }

   protected void clearQueuedSectionBlocks(LayerLightEngine<?, ?> param0, long param1) {
      if (param0.getQueueSize() != 0) {
         if (param0.getQueueSize() < 8192) {
            param0.removeIf(l_param2 -> SectionPos.blockToSection(l_param2) == param1);
         } else {
            int var4 = SectionPos.sectionToBlockCoord(SectionPos.x(param1));
            int var5 = SectionPos.sectionToBlockCoord(SectionPos.y(param1));
            int var6 = SectionPos.sectionToBlockCoord(SectionPos.z(param1));

            for (int var7 = 0; var7 < 16; var7++) {
               for (int var8 = 0; var8 < 16; var8++) {
                  for (int var9 = 0; var9 < 16; var9++) {
                     long var10 = BlockPos.asLong(var4 + var7, var5 + var8, var6 + var9);
                     param0.removeFromQueue(var10);
                  }
               }
            }
         }
      }
   }

   protected boolean hasInconsistencies() {
      return this.hasToRemove;
   }

   protected void markNewInconsistencies(LayerLightEngine<M, ?> param0, boolean param1, boolean param2) {
      if (this.hasInconsistencies() || !this.queuedSections.isEmpty()) {
         for (long var5 : this.toRemove) {
            this.clearQueuedSectionBlocks(param0, var5);
            DataLayer var7 = this.queuedSections.remove(var5);
            DataLayer var8 = this.updatingSectionData.removeLayer(var5);
            if (this.columnsToRetainQueuedDataFor.contains(SectionPos.getZeroNode(var5))) {
               if (var7 != null) {
                  this.queuedSections.put(var5, var7);
               } else if (var8 != null) {
                  this.queuedSections.put(var5, var8);
               }
            }
         }

         this.updatingSectionData.clearCache();

         for (long var5 : this.toRemove) {
            this.onNodeRemoved(var5);
         }

         this.toRemove.clear();
         this.hasToRemove = false;

         for (Entry<DataLayer> var5 : this.queuedSections.long2ObjectEntrySet()) {
            long var6 = var5.getLongKey();
            if (this.storingLightForSection(var6)) {
               DataLayer var8 = var5.getValue();
               if (this.updatingSectionData.getLayer(var6) != var8) {
                  this.clearQueuedSectionBlocks(param0, var6);
                  this.updatingSectionData.setLayer(var6, var8);
                  this.changedSections.add(var6);
               }
            }
         }

         this.updatingSectionData.clearCache();
         if (!param2) {
            for (long var5 : this.queuedSections.keySet()) {
               this.checkEdgesForSection(param0, var5);
            }
         } else {
            for (long var5 : this.untrustedSections) {
               this.checkEdgesForSection(param0, var5);
            }
         }

         this.untrustedSections.clear();
         ObjectIterator<Entry<DataLayer>> var4 = this.queuedSections.long2ObjectEntrySet().iterator();

         while (var4.hasNext()) {
            Entry<DataLayer> var5 = var4.next();
            long var6 = var5.getLongKey();
            if (this.storingLightForSection(var6)) {
               var4.remove();
            }
         }
      }
   }

   private void checkEdgesForSection(LayerLightEngine<M, ?> param0, long param1) {
      if (this.storingLightForSection(param1)) {
         int var4 = SectionPos.sectionToBlockCoord(SectionPos.x(param1));
         int var5 = SectionPos.sectionToBlockCoord(SectionPos.y(param1));
         int var6 = SectionPos.sectionToBlockCoord(SectionPos.z(param1));

         for (Direction var10 : DIRECTIONS) {
            long var11 = SectionPos.offset(param1, var10);
            if (!this.queuedSections.containsKey(var11) && this.storingLightForSection(var11)) {
               for (int var13 = 0; var13 < 16; var13++) {
                  for (int var14 = 0; var14 < 16; var14++) {
                     long var15;
                     long var17;
                     switch (var10) {
                        case DOWN:
                           var15 = BlockPos.asLong(var4 + var14, var5, var6 + var13);
                           var17 = BlockPos.asLong(var4 + var14, var5 - 1, var6 + var13);
                           break;
                        case UP:
                           var15 = BlockPos.asLong(var4 + var14, var5 + 16 - 1, var6 + var13);
                           var17 = BlockPos.asLong(var4 + var14, var5 + 16, var6 + var13);
                           break;
                        case NORTH:
                           var15 = BlockPos.asLong(var4 + var13, var5 + var14, var6);
                           var17 = BlockPos.asLong(var4 + var13, var5 + var14, var6 - 1);
                           break;
                        case SOUTH:
                           var15 = BlockPos.asLong(var4 + var13, var5 + var14, var6 + 16 - 1);
                           var17 = BlockPos.asLong(var4 + var13, var5 + var14, var6 + 16);
                           break;
                        case WEST:
                           var15 = BlockPos.asLong(var4, var5 + var13, var6 + var14);
                           var17 = BlockPos.asLong(var4 - 1, var5 + var13, var6 + var14);
                           break;
                        default:
                           var15 = BlockPos.asLong(var4 + 16 - 1, var5 + var13, var6 + var14);
                           var17 = BlockPos.asLong(var4 + 16, var5 + var13, var6 + var14);
                     }

                     param0.checkEdge(var15, var17, param0.computeLevelFromNeighbor(var15, var17, param0.getLevel(var15)), false);
                     param0.checkEdge(var17, var15, param0.computeLevelFromNeighbor(var17, var15, param0.getLevel(var17)), false);
                  }
               }
            }
         }
      }
   }

   protected void onNodeAdded(long param0) {
   }

   protected void onNodeRemoved(long param0) {
   }

   protected void enableLightSources(long param0, boolean param2) {
   }

   public void retainData(long param0, boolean param2) {
      if (param2) {
         this.columnsToRetainQueuedDataFor.add(param0);
      } else {
         this.columnsToRetainQueuedDataFor.remove(param0);
      }
   }

   protected void queueSectionData(long param0, @Nullable DataLayer param2, boolean param3) {
      if (param2 != null) {
         this.queuedSections.put(param0, param2);
         if (!param3) {
            this.untrustedSections.add(param0);
         }
      } else {
         this.queuedSections.remove(param0);
      }
   }

   protected void updateSectionStatus(long param0, boolean param2) {
      boolean var4 = this.dataSectionSet.contains(param0);
      if (!var4 && !param2) {
         this.toMarkData.add(param0);
         this.checkEdge(Long.MAX_VALUE, param0, 0, true);
      }

      if (var4 && param2) {
         this.toMarkNoData.add(param0);
         this.checkEdge(Long.MAX_VALUE, param0, 2, false);
      }
   }

   protected void runAllUpdates() {
      if (this.hasWork()) {
         this.runUpdates(Integer.MAX_VALUE);
      }
   }

   protected void swapSectionMap() {
      if (!this.changedSections.isEmpty()) {
         M var1 = this.updatingSectionData.copy();
         var1.disableCache();
         this.visibleSectionData = var1;
         this.changedSections.clear();
      }

      if (!this.sectionsAffectedByLightUpdates.isEmpty()) {
         LongIterator var1 = this.sectionsAffectedByLightUpdates.iterator();

         while (var1.hasNext()) {
            long var2 = var1.nextLong();
            this.chunkSource.onLightUpdate(this.layer, SectionPos.of(var2));
         }

         this.sectionsAffectedByLightUpdates.clear();
      }
   }
}

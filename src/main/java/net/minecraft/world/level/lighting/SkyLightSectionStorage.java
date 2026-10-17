package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public class SkyLightSectionStorage extends LayerLightSectionStorage<SkyLightSectionStorage.SkyDataLayerStorageMap> {
   private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
   private final LongSet sectionsWithSources = new LongOpenHashSet();
   private final LongSet sectionsToAddSourcesTo = new LongOpenHashSet();
   private final LongSet sectionsToRemoveSourcesFrom = new LongOpenHashSet();
   private final LongSet columnsWithSkySources = new LongOpenHashSet();
   private volatile boolean hasSourceInconsistencies;

   protected SkyLightSectionStorage(LightChunkGetter param0) {
      super(
         LightLayer.SKY,
         param0,
         new SkyLightSectionStorage.SkyDataLayerStorageMap(new Long2ObjectOpenHashMap<>(), new Long2IntOpenHashMap(), Integer.MAX_VALUE)
      );
   }

   @Override
   protected int getLightValue(long param0) {
      return this.getLightValue(param0, false);
   }

   protected int getLightValue(long param0, boolean param2) {
      long var4 = SectionPos.blockToSection(param0);
      int var6 = SectionPos.y(var4);
      SkyLightSectionStorage.SkyDataLayerStorageMap var7 = param2 ? this.updatingSectionData : this.visibleSectionData;
      int var8 = var7.topSections.get(SectionPos.getZeroNode(var4));
      if (var8 != var7.currentLowestY && var6 < var8) {
         DataLayer var9 = this.getDataLayer(var7, var4);
         if (var9 == null) {
            param0 = BlockPos.getFlatIndex(param0);

            while (var9 == null) {
               if (++var6 >= var8) {
                  return 15;
               }

               param0 = BlockPos.offset(param0, 0, 16, 0);
               var4 = SectionPos.offset(var4, Direction.UP);
               var9 = this.getDataLayer(var7, var4);
            }
         }

         return var9.get(
            SectionPos.sectionRelative(BlockPos.getX(param0)),
            SectionPos.sectionRelative(BlockPos.getY(param0)),
            SectionPos.sectionRelative(BlockPos.getZ(param0))
         );
      } else {
         return param2 && !this.lightOnInSection(var4) ? 0 : 15;
      }
   }

   @Override
   protected void onNodeAdded(long param0) {
      int var3 = SectionPos.y(param0);
      if (this.updatingSectionData.currentLowestY > var3) {
         this.updatingSectionData.currentLowestY = var3;
         this.updatingSectionData.topSections.defaultReturnValue(this.updatingSectionData.currentLowestY);
      }

      long var4 = SectionPos.getZeroNode(param0);
      int var6 = this.updatingSectionData.topSections.get(var4);
      if (var6 < var3 + 1) {
         this.updatingSectionData.topSections.put(var4, var3 + 1);
         if (this.columnsWithSkySources.contains(var4)) {
            this.queueAddSource(param0);
            if (var6 > this.updatingSectionData.currentLowestY) {
               long var7 = SectionPos.asLong(SectionPos.x(param0), var6 - 1, SectionPos.z(param0));
               this.queueRemoveSource(var7);
            }

            this.recheckInconsistencyFlag();
         }
      }
   }

   private void queueRemoveSource(long param0) {
      this.sectionsToRemoveSourcesFrom.add(param0);
      this.sectionsToAddSourcesTo.remove(param0);
   }

   private void queueAddSource(long param0) {
      this.sectionsToAddSourcesTo.add(param0);
      this.sectionsToRemoveSourcesFrom.remove(param0);
   }

   private void recheckInconsistencyFlag() {
      this.hasSourceInconsistencies = !this.sectionsToAddSourcesTo.isEmpty() || !this.sectionsToRemoveSourcesFrom.isEmpty();
   }

   @Override
   protected void onNodeRemoved(long param0) {
      long var3 = SectionPos.getZeroNode(param0);
      boolean var5 = this.columnsWithSkySources.contains(var3);
      if (var5) {
         this.queueRemoveSource(param0);
      }

      int var6 = SectionPos.y(param0);
      if (this.updatingSectionData.topSections.get(var3) == var6 + 1) {
         long var7;
         for (var7 = param0; !this.storingLightForSection(var7) && this.hasSectionsBelow(var6); var7 = SectionPos.offset(var7, Direction.DOWN)) {
            var6--;
         }

         if (this.storingLightForSection(var7)) {
            this.updatingSectionData.topSections.put(var3, var6 + 1);
            if (var5) {
               this.queueAddSource(var7);
            }
         } else {
            this.updatingSectionData.topSections.remove(var3);
         }
      }

      if (var5) {
         this.recheckInconsistencyFlag();
      }
   }

   @Override
   protected void enableLightSources(long param0, boolean param2) {
      this.runAllUpdates();
      if (param2 && this.columnsWithSkySources.add(param0)) {
         int var4 = this.updatingSectionData.topSections.get(param0);
         if (var4 != this.updatingSectionData.currentLowestY) {
            long var5 = SectionPos.asLong(SectionPos.x(param0), var4 - 1, SectionPos.z(param0));
            this.queueAddSource(var5);
            this.recheckInconsistencyFlag();
         }
      } else if (!param2) {
         this.columnsWithSkySources.remove(param0);
      }
   }

   @Override
   protected boolean hasInconsistencies() {
      return super.hasInconsistencies() || this.hasSourceInconsistencies;
   }

   @Override
   protected DataLayer createDataLayer(long param0) {
      DataLayer var3 = this.queuedSections.get(param0);
      if (var3 != null) {
         return var3;
      }

      long var4 = SectionPos.offset(param0, Direction.UP);
      int var6 = this.updatingSectionData.topSections.get(SectionPos.getZeroNode(param0));
      if (var6 != this.updatingSectionData.currentLowestY && SectionPos.y(var4) < var6) {
         DataLayer var7;
         while ((var7 = this.getDataLayer(var4, true)) == null) {
            var4 = SectionPos.offset(var4, Direction.UP);
         }

         return repeatFirstLayer(var7);
      } else {
         return new DataLayer();
      }
   }

   private static DataLayer repeatFirstLayer(DataLayer param0) {
      if (param0.isEmpty()) {
         return new DataLayer();
      }

      byte[] var1 = param0.getData();
      byte[] var2 = new byte[2048];

      for (int var3 = 0; var3 < 16; var3++) {
         System.arraycopy(var1, 0, var2, var3 * 128, 128);
      }

      return new DataLayer(var2);
   }

   @Override
   protected void markNewInconsistencies(LayerLightEngine<SkyLightSectionStorage.SkyDataLayerStorageMap, ?> param0, boolean param1, boolean param2) {
      super.markNewInconsistencies(param0, param1, param2);
      if (param1) {
         if (!this.sectionsToAddSourcesTo.isEmpty()) {
            for (long var5 : this.sectionsToAddSourcesTo) {
               int var7 = this.getLevel(var5);
               if (var7 != 2 && !this.sectionsToRemoveSourcesFrom.contains(var5) && this.sectionsWithSources.add(var5)) {
                  if (var7 == 1) {
                     this.clearQueuedSectionBlocks(param0, var5);
                     if (this.changedSections.add(var5)) {
                        this.updatingSectionData.copyDataLayer(var5);
                     }

                     Arrays.fill(this.getDataLayer(var5, true).getData(), (byte)-1);
                     int var8 = SectionPos.sectionToBlockCoord(SectionPos.x(var5));
                     int var9 = SectionPos.sectionToBlockCoord(SectionPos.y(var5));
                     int var10 = SectionPos.sectionToBlockCoord(SectionPos.z(var5));

                     for (Direction var14 : HORIZONTALS) {
                        long var15 = SectionPos.offset(var5, var14);
                        if ((
                              this.sectionsToRemoveSourcesFrom.contains(var15)
                                 || !this.sectionsWithSources.contains(var15) && !this.sectionsToAddSourcesTo.contains(var15)
                           )
                           && this.storingLightForSection(var15)) {
                           for (int var17 = 0; var17 < 16; var17++) {
                              for (int var18 = 0; var18 < 16; var18++) {
                                 long var19;
                                 long var21;
                                 switch (var14) {
                                    case NORTH:
                                       var19 = BlockPos.asLong(var8 + var17, var9 + var18, var10);
                                       var21 = BlockPos.asLong(var8 + var17, var9 + var18, var10 - 1);
                                       break;
                                    case SOUTH:
                                       var19 = BlockPos.asLong(var8 + var17, var9 + var18, var10 + 16 - 1);
                                       var21 = BlockPos.asLong(var8 + var17, var9 + var18, var10 + 16);
                                       break;
                                    case WEST:
                                       var19 = BlockPos.asLong(var8, var9 + var17, var10 + var18);
                                       var21 = BlockPos.asLong(var8 - 1, var9 + var17, var10 + var18);
                                       break;
                                    default:
                                       var19 = BlockPos.asLong(var8 + 16 - 1, var9 + var17, var10 + var18);
                                       var21 = BlockPos.asLong(var8 + 16, var9 + var17, var10 + var18);
                                 }

                                 param0.checkEdge(var19, var21, param0.computeLevelFromNeighbor(var19, var21, 0), true);
                              }
                           }
                        }
                     }

                     for (int var11 = 0; var11 < 16; var11++) {
                        for (int var12 = 0; var12 < 16; var12++) {
                           long var13 = BlockPos.asLong(
                              SectionPos.sectionToBlockCoord(SectionPos.x(var5), var11),
                              SectionPos.sectionToBlockCoord(SectionPos.y(var5)),
                              SectionPos.sectionToBlockCoord(SectionPos.z(var5), var12)
                           );
                           long var15 = BlockPos.asLong(
                              SectionPos.sectionToBlockCoord(SectionPos.x(var5), var11),
                              SectionPos.sectionToBlockCoord(SectionPos.y(var5)) - 1,
                              SectionPos.sectionToBlockCoord(SectionPos.z(var5), var12)
                           );
                           param0.checkEdge(var13, var15, param0.computeLevelFromNeighbor(var13, var15, 0), true);
                        }
                     }
                  } else {
                     for (int var8 = 0; var8 < 16; var8++) {
                        for (int var9 = 0; var9 < 16; var9++) {
                           long var10 = BlockPos.asLong(
                              SectionPos.sectionToBlockCoord(SectionPos.x(var5), var8),
                              SectionPos.sectionToBlockCoord(SectionPos.y(var5), 15),
                              SectionPos.sectionToBlockCoord(SectionPos.z(var5), var9)
                           );
                           param0.checkEdge(Long.MAX_VALUE, var10, 0, true);
                        }
                     }
                  }
               }
            }
         }

         this.sectionsToAddSourcesTo.clear();
         if (!this.sectionsToRemoveSourcesFrom.isEmpty()) {
            for (long var5 : this.sectionsToRemoveSourcesFrom) {
               if (this.sectionsWithSources.remove(var5) && this.storingLightForSection(var5)) {
                  for (int var7 = 0; var7 < 16; var7++) {
                     for (int var8 = 0; var8 < 16; var8++) {
                        long var9 = BlockPos.asLong(
                           SectionPos.sectionToBlockCoord(SectionPos.x(var5), var7),
                           SectionPos.sectionToBlockCoord(SectionPos.y(var5), 15),
                           SectionPos.sectionToBlockCoord(SectionPos.z(var5), var8)
                        );
                        param0.checkEdge(Long.MAX_VALUE, var9, 15, false);
                     }
                  }
               }
            }
         }

         this.sectionsToRemoveSourcesFrom.clear();
         this.hasSourceInconsistencies = false;
      }
   }

   protected boolean hasSectionsBelow(int param0) {
      return param0 >= this.updatingSectionData.currentLowestY;
   }

   protected boolean isAboveData(long param0) {
      long var3 = SectionPos.getZeroNode(param0);
      int var5 = this.updatingSectionData.topSections.get(var3);
      return var5 == this.updatingSectionData.currentLowestY || SectionPos.y(param0) >= var5;
   }

   protected boolean lightOnInSection(long param0) {
      long var3 = SectionPos.getZeroNode(param0);
      return this.columnsWithSkySources.contains(var3);
   }

   protected static final class SkyDataLayerStorageMap extends DataLayerStorageMap<SkyLightSectionStorage.SkyDataLayerStorageMap> {
      int currentLowestY;
      final Long2IntOpenHashMap topSections;

      public SkyDataLayerStorageMap(Long2ObjectOpenHashMap<DataLayer> param0, Long2IntOpenHashMap param1, int param2) {
         super(param0);
         this.topSections = param1;
         param1.defaultReturnValue(param2);
         this.currentLowestY = param2;
      }

      public SkyLightSectionStorage.SkyDataLayerStorageMap copy() {
         return new SkyLightSectionStorage.SkyDataLayerStorageMap(this.map.clone(), this.topSections.clone(), this.currentLowestY);
      }
   }
}

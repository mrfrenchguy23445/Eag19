package net.minecraft.world.level.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import java.util.Objects;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

public class EntitySectionStorage<T extends EntityAccess> {
   private final Class<T> entityClass;
   private final Long2ObjectFunction<Visibility> intialSectionVisibility;
   private final Long2ObjectMap<EntitySection<T>> sections = new Long2ObjectOpenHashMap<>();
   private final LongSortedSet sectionIds = new LongAVLTreeSet();

   public EntitySectionStorage(Class<T> param0, Long2ObjectFunction<Visibility> param1) {
      this.entityClass = param0;
      this.intialSectionVisibility = param1;
   }

   public void forEachAccessibleNonEmptySection(AABB param0, AbortableIterationConsumer<EntitySection<T>> param1) {
      int var3 = 2;
      int var4 = SectionPos.posToSectionCoord(param0.minX - 2.0);
      int var5 = SectionPos.posToSectionCoord(param0.minY - 4.0);
      int var6 = SectionPos.posToSectionCoord(param0.minZ - 2.0);
      int var7 = SectionPos.posToSectionCoord(param0.maxX + 2.0);
      int var8 = SectionPos.posToSectionCoord(param0.maxY + 0.0);
      int var9 = SectionPos.posToSectionCoord(param0.maxZ + 2.0);

      for (int var10 = var4; var10 <= var7; var10++) {
         long var11 = SectionPos.asLong(var10, 0, 0);
         long var13 = SectionPos.asLong(var10, -1, -1);
         LongIterator var15 = this.sectionIds.subSet(var11, var13 + 1L).iterator();

         while (var15.hasNext()) {
            long var16 = var15.nextLong();
            int var18 = SectionPos.y(var16);
            int var19 = SectionPos.z(var16);
            if (var18 >= var5 && var18 <= var8 && var19 >= var6 && var19 <= var9) {
               EntitySection<T> var20 = this.sections.get(var16);
               if (var20 != null && !var20.isEmpty() && var20.getStatus().isAccessible() && param1.accept(var20).shouldAbort()) {
                  return;
               }
            }
         }
      }
   }

   public LongStream getExistingSectionPositionsInChunk(long param0) {
      int var3 = ChunkPos.getX(param0);
      int var4 = ChunkPos.getZ(param0);
      LongSortedSet var5 = this.getChunkSections(var3, var4);
      if (var5.isEmpty()) {
         return LongStream.empty();
      }

      // Port note (wasm19): StreamSupport.longStream is not implemented in this classlib; the set is
      // materialised and streamed through LongStream.of(long...). Callers only iterate the result.
      return LongStream.of(var5.toLongArray());
   }

   private LongSortedSet getChunkSections(int param0, int param1) {
      long var3 = SectionPos.asLong(param0, 0, param1);
      long var5 = SectionPos.asLong(param0, -1, param1);
      return this.sectionIds.subSet(var3, var5 + 1L);
   }

   public Stream<EntitySection<T>> getExistingSectionsInChunk(long param0) {
      return this.getExistingSectionPositionsInChunk(param0).mapToObj(this.sections::get).filter(Objects::nonNull);
   }

   private static long getChunkKeyFromSectionKey(long param0) {
      return ChunkPos.asLong(SectionPos.x(param0), SectionPos.z(param0));
   }

   public EntitySection<T> getOrCreateSection(long param0) {
      return this.sections.computeIfAbsent(param0, this::createSection);
   }

   @Nullable
   public EntitySection<T> getSection(long param0) {
      return this.sections.get(param0);
   }

   private EntitySection<T> createSection(long param0) {
      long var3 = getChunkKeyFromSectionKey(param0);
      Visibility var5 = this.intialSectionVisibility.get(var3);
      this.sectionIds.add(param0);
      return new EntitySection<>(this.entityClass, var5);
   }

   public LongSet getAllChunksWithExistingSections() {
      LongSet var1 = new LongOpenHashSet();
      this.sections.keySet().forEach(l_param1 -> var1.add(getChunkKeyFromSectionKey(l_param1)));
      return var1;
   }

   public void getEntities(AABB param0, AbortableIterationConsumer<T> param1) {
      this.forEachAccessibleNonEmptySection(param0, l_param2 -> l_param2.getEntities(param0, param1));
   }

   public <U extends T> void getEntities(EntityTypeTest<T, U> param0, AABB param1, AbortableIterationConsumer<U> param2) {
      this.forEachAccessibleNonEmptySection(param1, l_param3 -> l_param3.getEntities(param0, param1, param2));
   }

   public void remove(long param0) {
      this.sections.remove(param0);
      this.sectionIds.remove(param0);
   }

   @VisibleForDebug
   public int count() {
      return this.sectionIds.size();
   }
}

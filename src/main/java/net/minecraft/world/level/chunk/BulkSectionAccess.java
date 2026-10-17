package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BulkSectionAccess implements AutoCloseable {
   private final LevelAccessor level;
   private final Long2ObjectMap<LevelChunkSection> acquiredSections = new Long2ObjectOpenHashMap<>();
   @Nullable
   private LevelChunkSection lastSection;
   private long lastSectionKey;

   public BulkSectionAccess(LevelAccessor param0) {
      this.level = param0;
   }

   @Nullable
   public LevelChunkSection getSection(BlockPos param0) {
      int var2 = this.level.getSectionIndex(param0.getY());
      if (var2 >= 0 && var2 < this.level.getSectionsCount()) {
         long var3 = SectionPos.asLong(param0);
         if (this.lastSection == null || this.lastSectionKey != var3) {
            this.lastSection = this.acquiredSections.computeIfAbsent(var3, l_param2 -> {
               ChunkAccess l_var5 = this.level.getChunk(SectionPos.blockToSectionCoord(param0.getX()), SectionPos.blockToSectionCoord(param0.getZ()));
               LevelChunkSection l_var6 = l_var5.getSection(var2);
               l_var6.acquire();
               return l_var6;
            });
            this.lastSectionKey = var3;
         }

         return this.lastSection;
      } else {
         return null;
      }
   }

   public BlockState getBlockState(BlockPos param0) {
      LevelChunkSection var2 = this.getSection(param0);
      if (var2 == null) {
         return Blocks.AIR.defaultBlockState();
      }

      int var3 = SectionPos.sectionRelative(param0.getX());
      int var4 = SectionPos.sectionRelative(param0.getY());
      int var5 = SectionPos.sectionRelative(param0.getZ());
      return var2.getBlockState(var3, var4, var5);
   }

   @Override
   public void close() {
      for (LevelChunkSection var2 : this.acquiredSections.values()) {
         var2.release();
      }
   }
}

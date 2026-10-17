package net.minecraft.client.renderer.chunk;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.DebugLevelSource;

class RenderChunk {
   private final Map<BlockPos, BlockEntity> blockEntities;
   @Nullable
   private final List<PalettedContainer<BlockState>> sections;
   private final boolean debug;
   private final LevelChunk wrapped;

   RenderChunk(LevelChunk param0) {
      this.wrapped = param0;
      this.debug = param0.getLevel().isDebug();
      this.blockEntities = ImmutableMap.copyOf(param0.getBlockEntities());
      if (param0 instanceof EmptyLevelChunk) {
         this.sections = null;
      } else {
         LevelChunkSection[] var2 = param0.getSections();
         this.sections = new ArrayList<>(var2.length);

         for (LevelChunkSection var6 : var2) {
            this.sections.add(var6.hasOnlyAir() ? null : var6.getStates().copy());
         }
      }
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos param0) {
      return this.blockEntities.get(param0);
   }

   public BlockState getBlockState(BlockPos param0) {
      int var2 = param0.getX();
      int var3 = param0.getY();
      int var4 = param0.getZ();
      if (this.debug) {
         BlockState var5 = null;
         if (var3 == 60) {
            var5 = Blocks.BARRIER.defaultBlockState();
         }

         if (var3 == 70) {
            var5 = DebugLevelSource.getBlockStateFor(var2, var4);
         }

         return var5 == null ? Blocks.AIR.defaultBlockState() : var5;
      } else {
         if (this.sections == null) {
            return Blocks.AIR.defaultBlockState();
         }

         try {
            int var5 = this.wrapped.getSectionIndex(var3);
            if (var5 >= 0 && var5 < this.sections.size()) {
               PalettedContainer<BlockState> var6 = this.sections.get(var5);
               if (var6 != null) {
                  return var6.get(var2 & 15, var3 & 15, var4 & 15);
               }
            }

            return Blocks.AIR.defaultBlockState();
         } catch (Throwable var5) {
            CrashReport var6 = CrashReport.forThrowable(var5, "Getting block state");
            CrashReportCategory var7 = var6.addCategory("Block being got");
            var7.setDetail("Location", () -> CrashReportCategory.formatLocation(this.wrapped, var2, var3, var4));
            throw new ReportedException(var6);
         }
      }
   }
}

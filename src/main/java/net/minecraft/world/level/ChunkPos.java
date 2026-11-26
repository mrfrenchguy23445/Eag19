package net.minecraft.world.level;

import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public class ChunkPos {
   private static final int SAFETY_MARGIN = 1056;
   public static final long INVALID_CHUNK_POS = asLong(1875066, 1875066);
   public static final ChunkPos ZERO = new ChunkPos(0, 0);
   private static final long COORD_BITS = 32L;
   private static final long COORD_MASK = 4294967295L;
   private static final int REGION_BITS = 5;
   public static final int REGION_SIZE = 32;
   private static final int REGION_MASK = 31;
   public static final int REGION_MAX_INDEX = 31;
   public final int x;
   public final int z;
   private static final int HASH_A = 1664525;
   private static final int HASH_C = 1013904223;
   private static final int HASH_Z_XOR = -559038737;

   public ChunkPos(int param0, int param1) {
      this.x = param0;
      this.z = param1;
   }

   public ChunkPos(BlockPos param0) {
      this.x = SectionPos.blockToSectionCoord(param0.getX());
      this.z = SectionPos.blockToSectionCoord(param0.getZ());
   }

   public ChunkPos(long param0) {
      this.x = (int)param0;
      this.z = (int)(param0 >> 32);
   }

   public static ChunkPos minFromRegion(int param0, int param1) {
      return new ChunkPos(param0 << 5, param1 << 5);
   }

   public static ChunkPos maxFromRegion(int param0, int param1) {
      return new ChunkPos((param0 << 5) + 31, (param1 << 5) + 31);
   }

   public long toLong() {
      return asLong(this.x, this.z);
   }

   public static long asLong(int param0, int param1) {
      return param0 & 4294967295L | (param1 & 4294967295L) << 32;
   }

   public static long asLong(BlockPos param0) {
      return asLong(SectionPos.blockToSectionCoord(param0.getX()), SectionPos.blockToSectionCoord(param0.getZ()));
   }

   public static int getX(long param0) {
      return (int)(param0 & 4294967295L);
   }

   public static int getZ(long param0) {
      return (int)(param0 >>> 32 & 4294967295L);
   }

   @Override
   public int hashCode() {
      return hash(this.x, this.z);
   }

   public static int hash(int param0, int param1) {
      int var2 = 1664525 * param0 + 1013904223;
      int var3 = 1664525 * (param1 ^ -559038737) + 1013904223;
      return var2 ^ var3;
   }

   @Override
   public boolean equals(Object param0) {
      if (this == param0) {
         return true;
      } else {
         return !(param0 instanceof ChunkPos var2) ? false : this.x == var2.x && this.z == var2.z;
      }
   }

   public int getMiddleBlockX() {
      return this.getBlockX(8);
   }

   public int getMiddleBlockZ() {
      return this.getBlockZ(8);
   }

   public int getMinBlockX() {
      return SectionPos.sectionToBlockCoord(this.x);
   }

   public int getMinBlockZ() {
      return SectionPos.sectionToBlockCoord(this.z);
   }

   public int getMaxBlockX() {
      return this.getBlockX(15);
   }

   public int getMaxBlockZ() {
      return this.getBlockZ(15);
   }

   public int getRegionX() {
      return this.x >> 5;
   }

   public int getRegionZ() {
      return this.z >> 5;
   }

   public int getRegionLocalX() {
      return this.x & 31;
   }

   public int getRegionLocalZ() {
      return this.z & 31;
   }

   public BlockPos getBlockAt(int param0, int param1, int param2) {
      return new BlockPos(this.getBlockX(param0), param1, this.getBlockZ(param2));
   }

   public int getBlockX(int param0) {
      return SectionPos.sectionToBlockCoord(this.x, param0);
   }

   public int getBlockZ(int param0) {
      return SectionPos.sectionToBlockCoord(this.z, param0);
   }

   public BlockPos getMiddleBlockPosition(int param0) {
      return new BlockPos(this.getMiddleBlockX(), param0, this.getMiddleBlockZ());
   }

   @Override
   public String toString() {
      return "[" + this.x + ", " + this.z + "]";
   }

   public BlockPos getWorldPosition() {
      return new BlockPos(this.getMinBlockX(), 0, this.getMinBlockZ());
   }

   public int getChessboardDistance(ChunkPos param0) {
      return Math.max(Math.abs(this.x - param0.x), Math.abs(this.z - param0.z));
   }

   public static Stream<ChunkPos> rangeClosed(ChunkPos param0, int param1) {
      return rangeClosed(new ChunkPos(param0.x - param1, param0.z - param1), new ChunkPos(param0.x + param1, param0.z + param1));
   }

   public static Stream<ChunkPos> rangeClosed(final ChunkPos param0, final ChunkPos param1) {
      int var2 = Math.abs(param0.x - param1.x) + 1;
      int var3 = Math.abs(param0.z - param1.z) + 1;
      final int var4 = param0.x < param1.x ? 1 : -1;
      final int var5 = param0.z < param1.z ? 1 : -1;
      return StreamSupport.stream(new AbstractSpliterator<ChunkPos>(var2 * var3, 64) {
         @Nullable
         private ChunkPos pos;

         @Override
         public boolean tryAdvance(Consumer<? super ChunkPos> l_param0) {
            if (this.pos == null) {
               this.pos = param0;
            } else {
               int l_var2 = this.pos.x;
               int l_var3 = this.pos.z;
               if (l_var2 == param1.x) {
                  if (l_var3 == param1.z) {
                     return false;
                  }

                  this.pos = new ChunkPos(param0.x, l_var3 + var5);
               } else {
                  this.pos = new ChunkPos(l_var2 + var4, l_var3);
               }
            }

            l_param0.accept(this.pos);
            return true;
         }
      }, false);
   }
}

package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class RegionFile implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SECTOR_BYTES = 4096;
   @VisibleForTesting
   protected static final int SECTOR_INTS = 1024;
   private static final int CHUNK_HEADER_SIZE = 5;
   private static final int HEADER_OFFSET = 0;
   private static final ByteBuffer PADDING_BUFFER = ByteBuffer.allocateDirect(1);
   private static final String EXTERNAL_FILE_EXTENSION = ".mcc";
   private static final int EXTERNAL_STREAM_FLAG = 128;
   private static final int EXTERNAL_CHUNK_THRESHOLD = 256;
   private static final int CHUNK_NOT_PRESENT = 0;
   private final FileChannel file;
   private final Path externalFileDir;
   final RegionFileVersion version;
   private final ByteBuffer header = zeroedHeader();
   private final IntBuffer offsets;
   private final IntBuffer timestamps;

   /**
    * The region header, zeroed explicitly rather than trusting allocateDirect.
    *
    * <p>The JDK guarantees that every element of an allocateDirect buffer is initialized to zero.
    * The wasm-gc classlib does not honour that: TByteBuffer.allocateDirect resolves to
    * Heap.alloc, which hands back a block of reused WASM linear memory that is never cleared. That
    * is invisible everywhere else in the game because every other direct buffer is written before
    * it is read -- but this header is the exception. It is filled by a read from disk, and a
    * brand-new region file has nothing to read: TFileChannel reports end-of-file as -1, leaving the
    * buffer untouched. Without this, offsets then reads the previous occupant's bytes and every
    * chunk in the region looks allocated at a sector that does not exist, so a freshly created
    * world fails to load with "header is truncated" and "Negative position" for every chunk.
    */
   private static ByteBuffer zeroedHeader() {
      ByteBuffer var0 = ByteBuffer.allocateDirect(8192);
      for (int var1 = 0; var1 < 8192; ++var1) {
         var0.put(var1, (byte)0);
      }

      return var0;
   }
   @VisibleForTesting
   protected final RegionBitmap usedSectors = new RegionBitmap();

   public RegionFile(Path param0, Path param1, boolean param2) throws IOException {
      this(param0, param1, RegionFileVersion.VERSION_DEFLATE, param2);
   }

   public RegionFile(Path param0, Path param1, RegionFileVersion param2, boolean param3) throws IOException {
      this.version = param2;
      if (!Files.isDirectory(param1)) {
         throw new IllegalArgumentException("Expected directory, got " + param1.toAbsolutePath());
      }

      this.externalFileDir = param1;
      this.offsets = this.header.asIntBuffer();
      this.offsets.limit(1024);
      this.header.position(4096);
      this.timestamps = this.header.asIntBuffer();
      if (param3) {
         this.file = FileChannel.open(param0, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DSYNC);
      } else {
         this.file = FileChannel.open(param0, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
      }

      this.usedSectors.force(0, 2);
      this.header.position(0);
      int var5 = this.file.read(this.header, 0L);
      if (var5 != -1) {
         if (var5 != 8192) {
            LOGGER.warn("Region file {} has truncated header: {}", param0, var5);
         }

         long var6 = Files.size(param0);

         for (int var8 = 0; var8 < 1024; var8++) {
            int var9 = this.offsets.get(var8);
            if (var9 != 0) {
               int var10 = getSectorNumber(var9);
               int var11 = getNumSectors(var9);
               if (var10 < 2) {
                  LOGGER.warn("Region file {} has invalid sector at index: {}; sector {} overlaps with header", param0, var8, var10);
                  this.offsets.put(var8, 0);
               } else if (var11 == 0) {
                  LOGGER.warn("Region file {} has an invalid sector at index: {}; size has to be > 0", param0, var8);
                  this.offsets.put(var8, 0);
               } else if (var10 * 4096L > var6) {
                  LOGGER.warn("Region file {} has an invalid sector at index: {}; sector {} is out of bounds", param0, var8, var10);
                  this.offsets.put(var8, 0);
               } else {
                  this.usedSectors.force(var10, var11);
               }
            }
         }
      }
   }

   private Path getExternalChunkPath(ChunkPos param0) {
      String var2 = "c." + param0.x + "." + param0.z + ".mcc";
      return this.externalFileDir.resolve(var2);
   }

   @Nullable
   public synchronized DataInputStream getChunkDataInputStream(ChunkPos param0) throws IOException {
      int var2 = this.getOffset(param0);
      if (var2 == 0) {
         return null;
      }

      int var3 = getSectorNumber(var2);
      int var4 = getNumSectors(var2);
      int var5 = var4 * 4096;
      ByteBuffer var6 = ByteBuffer.allocate(var5);
      this.file.read(var6, var3 * 4096);
      var6.flip();
      if (var6.remaining() < 5) {
         LOGGER.error("Chunk {} header is truncated: expected {} but read {}", param0, var5, var6.remaining());
         return null;
      }

      int var7 = var6.getInt();
      byte var8 = var6.get();
      if (var7 == 0) {
         LOGGER.warn("Chunk {} is allocated, but stream is missing", param0);
         return null;
      }

      int var9 = var7 - 1;
      if (isExternalStreamChunk(var8)) {
         if (var9 != 0) {
            LOGGER.warn("Chunk has both internal and external streams");
         }

         return this.createExternalChunkInputStream(param0, getExternalChunkVersion(var8));
      } else if (var9 > var6.remaining()) {
         LOGGER.error("Chunk {} stream is truncated: expected {} but read {}", param0, var9, var6.remaining());
         return null;
      } else if (var9 < 0) {
         LOGGER.error("Declared size {} of chunk {} is negative", var7, param0);
         return null;
      } else {
         return this.createChunkInputStream(param0, var8, createStream(var6, var9));
      }
   }

   private static int getTimestamp() {
      return (int)(Util.getEpochMillis() / 1000L);
   }

   private static boolean isExternalStreamChunk(byte param0) {
      return (param0 & 128) != 0;
   }

   private static byte getExternalChunkVersion(byte param0) {
      return (byte)(param0 & -129);
   }

   @Nullable
   private DataInputStream createChunkInputStream(ChunkPos param0, byte param1, InputStream param2) throws IOException {
      RegionFileVersion var4 = RegionFileVersion.fromId(param1);
      if (var4 == null) {
         LOGGER.error("Chunk {} has invalid chunk stream version {}", param0, param1);
         return null;
      } else {
         return new DataInputStream(var4.wrap(param2));
      }
   }

   @Nullable
   private DataInputStream createExternalChunkInputStream(ChunkPos param0, byte param1) throws IOException {
      Path var3 = this.getExternalChunkPath(param0);
      if (!Files.isRegularFile(var3)) {
         LOGGER.error("External chunk path {} is not file", var3);
         return null;
      } else {
         return this.createChunkInputStream(param0, param1, Files.newInputStream(var3));
      }
   }

   private static ByteArrayInputStream createStream(ByteBuffer param0, int param1) {
      return new ByteArrayInputStream(param0.array(), param0.position(), param1);
   }

   private int packSectorOffset(int param0, int param1) {
      return param0 << 8 | param1;
   }

   private static int getNumSectors(int param0) {
      return param0 & 0xFF;
   }

   private static int getSectorNumber(int param0) {
      return param0 >> 8 & 16777215;
   }

   private static int sizeToSectors(int param0) {
      return (param0 + 4096 - 1) / 4096;
   }

   public boolean doesChunkExist(ChunkPos param0) {
      int var2 = this.getOffset(param0);
      if (var2 == 0) {
         return false;
      }

      int var3 = getSectorNumber(var2);
      int var4 = getNumSectors(var2);
      ByteBuffer var5 = ByteBuffer.allocate(5);

      try {
         this.file.read(var5, var3 * 4096);
         var5.flip();
         if (var5.remaining() != 5) {
            return false;
         }

         int var6 = var5.getInt();
         byte var7 = var5.get();
         if (isExternalStreamChunk(var7)) {
            if (!RegionFileVersion.isValidVersion(getExternalChunkVersion(var7))) {
               return false;
            }

            if (!Files.isRegularFile(this.getExternalChunkPath(param0))) {
               return false;
            }
         } else {
            if (!RegionFileVersion.isValidVersion(var7)) {
               return false;
            }

            if (var6 == 0) {
               return false;
            }

            int var8 = var6 - 1;
            if (var8 < 0 || var8 > 4096 * var4) {
               return false;
            }
         }

         return true;
      } catch (IOException var6) {
         return false;
      }
   }

   public DataOutputStream getChunkDataOutputStream(ChunkPos param0) throws IOException {
      return new DataOutputStream(this.version.wrap(new RegionFile.ChunkBuffer(param0)));
   }

   public void flush() throws IOException {
      this.file.force(true);
   }

   public void clear(ChunkPos param0) throws IOException {
      int var2 = getOffsetIndex(param0);
      int var3 = this.offsets.get(var2);
      if (var3 != 0) {
         this.offsets.put(var2, 0);
         this.timestamps.put(var2, getTimestamp());
         this.writeHeader();
         Files.deleteIfExists(this.getExternalChunkPath(param0));
         this.usedSectors.free(getSectorNumber(var3), getNumSectors(var3));
      }
   }

   protected synchronized void write(ChunkPos param0, ByteBuffer param1) throws IOException {
      int var3 = getOffsetIndex(param0);
      int var4 = this.offsets.get(var3);
      int var5 = getSectorNumber(var4);
      int var6 = getNumSectors(var4);
      int var7 = param1.remaining();
      int var8 = sizeToSectors(var7);
      int var9;
      RegionFile.CommitOp var10;
      if (var8 >= 256) {
         Path var11 = this.getExternalChunkPath(param0);
         LOGGER.warn("Saving oversized chunk {} ({} bytes} to external file {}", param0, var7, var11);
         var8 = 1;
         var9 = this.usedSectors.allocate(var8);
         var10 = this.writeToExternalFile(var11, param1);
         ByteBuffer var12 = this.createExternalStub();
         this.file.write(var12, var9 * 4096);
      } else {
         var9 = this.usedSectors.allocate(var8);
         var10 = () -> Files.deleteIfExists(this.getExternalChunkPath(param0));
         this.file.write(param1, var9 * 4096);
      }

      this.offsets.put(var3, this.packSectorOffset(var9, var8));
      this.timestamps.put(var3, getTimestamp());
      this.writeHeader();
      var10.run();
      if (var5 != 0) {
         this.usedSectors.free(var5, var6);
      }
   }

   private ByteBuffer createExternalStub() {
      ByteBuffer var1 = ByteBuffer.allocate(5);
      var1.putInt(1);
      var1.put((byte)(this.version.getId() | 128));
      var1.flip();
      return var1;
   }

   private RegionFile.CommitOp writeToExternalFile(Path param0, ByteBuffer param1) throws IOException {
      Path var3 = Files.createTempFile(this.externalFileDir, "tmp", null);

      try (FileChannel var4 = FileChannel.open(var3, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
         param1.position(5);
         var4.write(param1);
      }

      return () -> Files.move(var3, param0, StandardCopyOption.REPLACE_EXISTING);
   }

   private void writeHeader() throws IOException {
      this.header.position(0);
      this.file.write(this.header, 0L);
   }

   private int getOffset(ChunkPos param0) {
      return this.offsets.get(getOffsetIndex(param0));
   }

   public boolean hasChunk(ChunkPos param0) {
      return this.getOffset(param0) != 0;
   }

   private static int getOffsetIndex(ChunkPos param0) {
      return param0.getRegionLocalX() + param0.getRegionLocalZ() * 32;
   }

   @Override
   public void close() throws IOException {
      try {
         this.padToFullSector();
      } finally {
         try {
            this.file.force(true);
         } finally {
            this.file.close();
         }
      }
   }

   private void padToFullSector() throws IOException {
      int var1 = (int)this.file.size();
      int var2 = sizeToSectors(var1) * 4096;
      if (var1 != var2) {
         ByteBuffer var3 = PADDING_BUFFER.duplicate();
         var3.position(0);
         this.file.write(var3, var2 - 1);
      }
   }

   class ChunkBuffer extends ByteArrayOutputStream {
      private final ChunkPos pos;

      public ChunkBuffer(ChunkPos param1) {
         super(8096);
         super.write(0);
         super.write(0);
         super.write(0);
         super.write(0);
         super.write(RegionFile.this.version.getId());
         this.pos = param1;
      }

      @Override
      public void close() throws IOException {
         ByteBuffer var1 = ByteBuffer.wrap(this.buf, 0, this.count);
         var1.putInt(0, this.count - 5 + 1);
         RegionFile.this.write(this.pos, var1);
      }
   }

   interface CommitOp {
      void run() throws IOException;
   }
}

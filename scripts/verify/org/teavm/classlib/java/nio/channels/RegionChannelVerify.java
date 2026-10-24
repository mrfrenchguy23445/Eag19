package org.teavm.classlib.java.nio.channels;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/** Exercises the same positional writes and padding used by region storage. */
public final class RegionChannelVerify {
   private static void check(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }
   public static void main(String[] args) throws Exception {
      Path path = Files.createTempFile("region-channel-", ".mca");
      try {
         try (TFileChannel channel = TFileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            channel.position(17);
            channel.write(ByteBuffer.wrap(new byte[] {1, 2, 3}), 8192);
            check(channel.position() == 17, "positional write moved cursor");
            check(channel.size() == 8195, "sparse write length");
            ByteBuffer gap = ByteBuffer.allocate(8192);
            check(channel.read(gap, 0) == 8192, "sparse read");
            check(Arrays.equals(gap.array(), new byte[8192]), "gap must be zero-filled");
            channel.write(ByteBuffer.wrap(new byte[] {9}), 0);
            channel.write(ByteBuffer.wrap(new byte[] {4}), 8195);
            check(channel.size() == 8196, "growth must not double the logical file size");
            ByteBuffer data = ByteBuffer.allocate(4);
            channel.read(data, 8192);
            check(Arrays.equals(data.array(), new byte[] {1, 2, 3, 4}), "sector content");
            check(channel.position() == 17, "positional read moved cursor");
            channel.force(true);
         }
         try (TFileChannel channel = TFileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            ByteBuffer data = ByteBuffer.allocate(4);
            channel.read(data, 8192);
            check(Arrays.equals(data.array(), new byte[] {1, 2, 3, 4}), "reopen lost chunk data");
            channel.position(100);
            channel.truncate(10);
            check(channel.position() == 10 && channel.size() == 10, "truncate cursor and length");
         }
         check(Files.size(path) == 10, "truncate persisted");
         System.out.println("Region channel verification passed");
      } finally {
         Files.deleteIfExists(path);
      }
   }
}

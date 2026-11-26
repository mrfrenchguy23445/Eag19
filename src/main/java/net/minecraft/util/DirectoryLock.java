package net.minecraft.util;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.FileUtil;

public class DirectoryLock implements AutoCloseable {
   public static final String LOCK_FILE = "session.lock";
   private final FileChannel lockFile;
   private final FileLock lock;
   private static final ByteBuffer DUMMY;

   public static DirectoryLock create(Path param0) throws IOException {
      Path var1 = param0.resolve("session.lock");
      FileUtil.createDirectoriesSafe(param0);
      FileChannel var2 = FileChannel.open(var1, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

      try {
         var2.write(DUMMY.duplicate());
         var2.force(true);
         FileLock var3 = var2.tryLock();
         if (var3 == null) {
            throw DirectoryLock.LockException.alreadyLocked(var1);
         } else {
            return new DirectoryLock(var2, var3);
         }
      } catch (IOException var3) {
         try {
            var2.close();
         } catch (IOException var4) {
            var3.addSuppressed(var4);
         }

         throw var3;
      }
   }

   private DirectoryLock(FileChannel param0, FileLock param1) {
      this.lockFile = param0;
      this.lock = param1;
   }

   @Override
   public void close() throws IOException {
      try {
         if (this.lock.isValid()) {
            this.lock.release();
         }
      } finally {
         if (this.lockFile.isOpen()) {
            this.lockFile.close();
         }
      }
   }

   public boolean isValid() {
      return this.lock.isValid();
   }

   public static boolean isLocked(Path param0) throws IOException {
      Path var1 = param0.resolve("session.lock");

      try (
         FileChannel var2 = FileChannel.open(var1, StandardOpenOption.WRITE);
         FileLock var3 = var2.tryLock();
      ) {
         return var3 == null;
      } catch (AccessDeniedException var2) {
         return true;
      } catch (NoSuchFileException var2) {
         return false;
      }
   }

   static {
      byte[] var0 = "☃".getBytes(Charsets.UTF_8);
      DUMMY = ByteBuffer.allocateDirect(var0.length);
      DUMMY.put(var0);
      DUMMY.flip();
   }

   public static class LockException extends IOException {
      private LockException(Path param0, String param1) {
         super(param0.toAbsolutePath() + ": " + param1);
      }

      public static DirectoryLock.LockException alreadyLocked(Path param0) {
         return new DirectoryLock.LockException(param0, "already locked (possibly by other Minecraft instance?)");
      }
   }
}

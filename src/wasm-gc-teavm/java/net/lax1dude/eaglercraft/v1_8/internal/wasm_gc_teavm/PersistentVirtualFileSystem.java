package net.lax1dude.eaglercraft.v1_8.internal.wasm_gc_teavm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import net.lax1dude.eaglercraft.v1_8.internal.vfs2.VFile2;
import net.lax1dude.eaglercraft.v1_8.EagRuntime;
import net.lax1dude.eaglercraft.v1_8.Filesystem;
import net.lax1dude.eaglercraft.v1_8.internal.IEaglerFilesystem;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;
import org.teavm.runtime.fs.VirtualFileSystem;

/** Routes java.io and java.nio saves through the runtime's persistent IndexedDB store. */
public final class PersistentVirtualFileSystem implements VirtualFileSystem {
   private static final String FILES = "eag19-vfs/files/";
   private static final String DIRS = "eag19-vfs/directories/";
   private static final String TIMES = "eag19-vfs/timestamps/";

   interface Store {
      byte[] read(String key);
      void write(String key, byte[] bytes);
      boolean delete(String key);
      boolean exists(String key);
      List<String> list(String prefix);
   }

   private final Store store;

   public PersistentVirtualFileSystem() {
      this(Filesystem.getHandleFor(EagRuntime.getConfiguration().getWorldsDB()));
   }

   private PersistentVirtualFileSystem(IEaglerFilesystem filesystem) {
      this(new Store() {
         public byte[] read(String key) { return VFile2.create(filesystem, key).getAllBytes(); }
         public void write(String key, byte[] bytes) { VFile2.create(filesystem, key).setAllBytes(bytes); }
         public boolean delete(String key) { return VFile2.create(filesystem, key).delete(); }
         public boolean exists(String key) { return VFile2.create(filesystem, key).exists(); }
         public List<String> list(String prefix) { return VFile2.create(filesystem, prefix).listFilenames(true); }
      });
   }

   PersistentVirtualFileSystem(Store store) { this.store = store; }

   public String getUserDir() { return "/"; }
   public boolean isWindows() { return false; }
   public String canonicalize(String path) {
      List<String> parts = new ArrayList<>();
      for (String part : path.replace('\\', '/').split("/")) {
         if (part.isEmpty() || part.equals(".")) continue;
         if (part.equals("..")) {
            if (!parts.isEmpty()) parts.remove(parts.size() - 1);
         } else {
            parts.add(part);
         }
      }
      return "/" + String.join("/", parts);
   }
   public VirtualFile getFile(String path) { return new Entry(canonicalize(path).substring(1)); }

   private byte[] timestamp(long value) { return Long.toString(value).getBytes(StandardCharsets.UTF_8); }

   private final class Entry implements VirtualFile {
      private final String path;
      Entry(String path) { this.path = path; }
      private String child(String name) { return path.isEmpty() ? name : path + "/" + name; }
      private boolean validName(String name) {
         return !name.isEmpty() && !name.equals(".") && !name.equals("..") && name.indexOf('/') < 0 && name.indexOf('\\') < 0;
      }
      public String getName() { return path.substring(path.lastIndexOf('/') + 1); }
      public boolean isDirectory() { return path.isEmpty() || store.exists(DIRS + path); }
      public boolean isFile() { return !path.isEmpty() && store.exists(FILES + path); }
      public boolean canRead() { return isFile() || isDirectory(); }
      public boolean canWrite() { return canRead(); }
      public boolean setReadOnly(boolean readOnly) { return !readOnly; }
      public int length() {
         byte[] bytes = isFile() ? store.read(FILES + path) : null;
         return bytes == null ? 0 : bytes.length;
      }
      public long lastModified() {
         byte[] bytes = store.read(isDirectory() ? DIRS + path : TIMES + path);
         if (bytes == null) return 0;
         try { return Long.parseLong(new String(bytes, StandardCharsets.UTF_8)); }
         catch (NumberFormatException ignored) { return 0; }
      }
      public boolean setLastModified(long time) {
         if (!canRead()) return false;
         store.write(isDirectory() ? DIRS + path : TIMES + path, timestamp(time));
         return true;
      }
      public String[] listFiles() {
         if (!isDirectory()) return null;
         TreeSet<String> children = new TreeSet<>();
         for (String root : new String[] { FILES, DIRS }) {
            String prefix = root + (path.isEmpty() ? "" : path + "/");
            for (String key : store.list(prefix)) {
               if (!key.startsWith(prefix)) continue;
               String name = key.substring(prefix.length());
               if (name.isEmpty()) continue;
               int slash = name.indexOf('/');
               children.add(slash < 0 ? name : name.substring(0, slash));
            }
         }
         return children.toArray(new String[0]);
      }
      public boolean createFile(String name) throws IOException {
         if (!isDirectory() || !validName(name)) throw new IOException("Invalid parent or file name");
         Entry target = new Entry(child(name));
         if (target.canRead()) return false;
         store.write(FILES + target.path, new byte[0]);
         target.setLastModified(System.currentTimeMillis());
         return true;
      }
      public boolean createDirectory(String name) {
         if (!isDirectory() || !validName(name)) return false;
         Entry target = new Entry(child(name));
         if (target.canRead()) return false;
         store.write(DIRS + target.path, timestamp(System.currentTimeMillis()));
         return true;
      }
      public boolean delete() {
         if (path.isEmpty()) return false;
         if (isDirectory()) {
            if (listFiles().length != 0) return false;
            return store.delete(DIRS + path);
         }
         if (!isFile()) return false;
         boolean deleted = store.delete(FILES + path);
         if (deleted) store.delete(TIMES + path);
         return deleted;
      }
      public boolean adopt(VirtualFile source, String name) {
         if (!(source instanceof PersistentVirtualFileSystem.Entry) || !isDirectory() || !validName(name)) return false;
         Entry from = (Entry)source;
         String target = child(name);
         if (from.path.isEmpty() || target.equals(from.path) || target.startsWith(from.path + "/")) return false;
         Entry destination = new Entry(target);
         if (!from.canRead() || destination.canRead()) return false;
         for (String root : new String[] { FILES, DIRS, TIMES }) {
            String oldKey = root + from.path;
            List<String> keys = new ArrayList<>(store.list(oldKey + "/"));
            if (store.exists(oldKey)) keys.add(oldKey);
            for (String key : keys) {
               if (!key.equals(oldKey) && !key.startsWith(oldKey + "/")) continue;
               store.write(root + target + key.substring(oldKey.length()), store.read(key));
               store.delete(key);
            }
         }
         return true;
      }
      public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
         if (!isFile()) return null;
         return new Accessor(this, readable, writable, append);
      }
   }

   private final class Accessor implements VirtualFileAccessor {
      private final Entry file;
      private final boolean readable;
      private final boolean writable;
      private byte[] bytes;
      private int position;
      private boolean dirty;
      private boolean closed;
      Accessor(Entry file, boolean readable, boolean writable, boolean append) {
         this.file = file;
         this.readable = readable;
         this.writable = writable;
         this.bytes = store.read(FILES + file.path);
         if (writable && !append) { bytes = new byte[0]; dirty = true; }
         if (append) position = bytes.length;
      }
      private void check() throws IOException { if (closed) throw new IOException("File is closed"); }
      public int read(byte[] buffer, int offset, int count) throws IOException {
         check();
         if (!readable) throw new IOException("File is not readable");
         if (count == 0) return 0;
         int length = Math.min(count, bytes.length - position);
         if (length <= 0) return -1;
         System.arraycopy(bytes, position, buffer, offset, length);
         position += length;
         return length;
      }
      public void write(byte[] buffer, int offset, int count) throws IOException {
         check();
         if (!writable) throw new IOException("File is not writable");
         if (position > Integer.MAX_VALUE - count) throw new IOException("File too large");
         if (position + count > bytes.length) bytes = Arrays.copyOf(bytes, position + count);
         System.arraycopy(buffer, offset, bytes, position, count);
         position += count;
         dirty = true;
      }
      public int tell() throws IOException { check(); return position; }
      public void seek(int target) throws IOException {
         check();
         if (target < 0) throw new IOException("Negative seek");
         position = target;
      }
      public void skip(int amount) throws IOException { seek(Math.addExact(position, amount)); }
      public int size() throws IOException { check(); return bytes.length; }
      public void resize(int size) throws IOException {
         check();
         if (!writable || size < 0) throw new IOException("Invalid resize");
         bytes = Arrays.copyOf(bytes, size);
         dirty = true;
      }
      public void flush() throws IOException {
         check();
         if (dirty) {
            store.write(FILES + file.path, bytes);
            file.setLastModified(System.currentTimeMillis());
            dirty = false;
         }
      }
      public void close() throws IOException { if (!closed) { flush(); closed = true; } }
   }
}

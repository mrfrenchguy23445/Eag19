package net.lax1dude.eaglercraft.v1_8.internal.wasm_gc_teavm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;

public final class PersistentFilesystemVerify {
   private static void check(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }
   public static void main(String[] args) throws Exception {
      Map<String, byte[]> database = new HashMap<>();
      PersistentVirtualFileSystem.Store store = new PersistentVirtualFileSystem.Store() {
         public byte[] read(String key) { byte[] bytes = database.get(key); return bytes == null ? null : bytes.clone(); }
         public void write(String key, byte[] bytes) { database.put(key, bytes.clone()); }
         public boolean exists(String key) { return database.containsKey(key); }
         public boolean delete(String key) { return database.remove(key) != null; }
         public List<String> list(String prefix) { return database.keySet().stream().filter(k -> k.startsWith(prefix)).collect(Collectors.toList()); }
      };
      PersistentVirtualFileSystem fs = new PersistentVirtualFileSystem(store);
      check(fs.getFile("/").createDirectory("saves"), "create saves");
      check(fs.getFile("/saves").createDirectory("World"), "create world");
      VirtualFile world = fs.getFile("/saves/World");
      check(world.createDirectory("empty"), "create empty directory");
      check(world.createFile("level.dat"), "create level data");
      VirtualFile level = fs.getFile("/saves/World/level.dat");
      VirtualFileAccessor output = level.createAccessor(false, true, false);
      output.write(new byte[] {1, 2, 3}, 0, 3);
      output.close();
      level.setLastModified(12345);
      // A new provider instance models a page reload; all state must come from the store.
      fs = new PersistentVirtualFileSystem(store);
      check(fs.getFile("/saves/World/empty").isDirectory(), "empty directory survives reload");
      level = fs.getFile("/saves/World/level.dat");
      check(level.lastModified() == 12345 && level.length() == 3, "saved metadata");
      byte[] data = new byte[3];
      VirtualFileAccessor input = level.createAccessor(true, false, false);
      check(input.read(data, 0, 3) == 3 && Arrays.equals(data, new byte[] {1, 2, 3}), "saved content");
      input.close();
      output = level.createAccessor(false, true, true);
      output.seek(0);
      output.write(new byte[] {9}, 0, 1);
      output.close();
      input = level.createAccessor(true, false, false);
      input.read(data, 0, 3);
      input.close();
      check(Arrays.equals(data, new byte[] {9, 2, 3}), "append accessor plus rewind preserves existing bytes");
      output = level.createAccessor(false, true, true);
      output.write(new byte[] {4}, 0, 1);
      output.close();
      check(level.length() == 4, "append");
      output = level.createAccessor(false, true, false);
      check(output.size() == 0, "non-append output stream must truncate");
      output.write(new byte[] {5, 6, 7}, 0, 3);
      output.close();
      check(!fs.getFile("/saves/World").delete(), "cannot delete nonempty directory");
      check(fs.getFile("/saves").adopt(fs.getFile("/saves/World"), "Renamed"), "move world tree");
      check(!fs.getFile("/saves/World").isDirectory(), "old world removed");
      check(fs.getFile("/saves/Renamed/level.dat").length() == 3, "moved content");
      check(Arrays.equals(fs.getFile("/saves").listFiles(), new String[] {"Renamed"}), "directory listing");
      check(fs.canonicalize("/saves/./Renamed/../Renamed").equals("/saves/Renamed"), "path normalization");
      System.out.println("Persistent filesystem verification passed");
   }
}

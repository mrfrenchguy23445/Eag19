package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import net.minecraft.Util;
import org.slf4j.Logger;

/**
 * Collects files and strings into a zip archive.
 *
 * <p>Upstream builds the archive through {@code java.nio.file.FileSystemProvider} and the JDK's
 * {@code jar:} filesystem. {@code FileSystemProvider} does not exist in the WASM-GC TeaVM
 * classlib, and the NIO filesystem model is not available there at all, so this class writes the
 * archive format directly instead.
 *
 * <p>Entries are buffered in memory and the archive is emitted by {@link #close()}. The ZIP local
 * file header records each entry's compressed size up front, so a streaming writer would have to
 * use the data descriptor extension; buffering is simpler and the only caller
 * ({@code Minecraft.saveProfilingResult}) writes a small set of diagnostic text files.
 *
 * <p>Entries are stored uncompressed (method 0). Profiling dumps are mostly text that a modern
 * decompressor handles at comparable speed, and {@code Deflater} is not guaranteed to be present
 * in every TeaVM classlib.
 */
public class FileZipper implements Closeable {
   private static final Logger LOGGER = LogUtils.getLogger();

   /** Fixed DOS timestamp: 1980-01-01 00:00:00, the earliest the ZIP format can express. */
   private static final int DOS_TIME = 0;
   private static final int DOS_DATE = 33;

   private static final int METHOD_STORED = 0;
   private static final int VERSION_NEEDED = 20;
   private static final int VERSION_MADE_BY = 20;
   /** Bit 11 of the general purpose flags: file name and comment are UTF-8. */
   private static final int FLAG_UTF8 = 1 << 11;
   /** MS-DOS directory attribute in the low byte of the external file attributes. */
   private static final int EXTERNAL_ATTR_DIRECTORY = 0x10;

   private final Path outputFile;
   private final List<Entry> entries = new ArrayList<>();

   public FileZipper(Path param0) {
      this.outputFile = param0;
   }

   public void add(Path param0, String param1) {
      this.addEntry(param0.toString(), param1.getBytes(StandardCharsets.UTF_8));
   }

   public void add(Path param0, File param1) {
      try {
         this.addEntry(param0.toString(), Files.readAllBytes(Util.toPath(param1)));
      } catch (IOException var2) {
         throw new UncheckedIOException(var2);
      }
   }

   public void add(Path param0) {
      if (Files.isRegularFile(param0)) {
         try {
            this.addEntry(param0.toString(), Files.readAllBytes(param0));
         } catch (IOException var2) {
            throw new UncheckedIOException(var2);
         }
      } else {
         try (Stream<Path> var3 = Files.find(param0, Integer.MAX_VALUE, (l_param0, l_param1) -> l_param1.isRegularFile())) {
            for (Path var5 : var3.collect(Collectors.toList())) {
               this.addEntry(param0.relativize(var5).toString(), Files.readAllBytes(var5));
            }
         } catch (IOException var2) {
            throw new UncheckedIOException(var2);
         }
      }
   }

   @Override
   public void close() {
      try {
         writeArchive(this.outputFile.toFile(), this.entries);
         LOGGER.info("Compressed to {}", this.outputFile);
      } catch (IOException var1) {
         throw new UncheckedIOException(var1);
      }
   }

   /**
    * Records one file entry, first emitting a directory entry for each parent that has not been
    * recorded yet so that the archive is well formed even for readers that do not synthesise
    * intermediate directories.
    */
   private void addEntry(String param0, byte[] param1) {
      String var1 = normaliseName(param0);
      if (var1.isEmpty()) {
         return;
      }

      int var2 = var1.lastIndexOf('/');
      if (var2 > 0) {
         String var3 = var1.substring(0, var2);
         for (int var4 = var3.indexOf('/'); var4 >= 0; var4 = var3.indexOf('/', var4 + 1)) {
            this.entries.add(Entry.directory(var3.substring(0, var4)));
         }

         this.entries.add(Entry.directory(var3));
      }

      this.entries.add(Entry.file(var1, param1));
   }

   /**
    * Normalises an entry name into a relative, forward-slash separated path with no leading
    * separator, as required by the ZIP specification.
    */
   private static String normaliseName(String param0) {
      String var1 = param0.replace('\\', '/');
      while (var1.startsWith("/")) {
         var1 = var1.substring(1);
      }

      return var1;
   }

   private static void writeArchive(File param0, List<Entry> param1) throws IOException {
      File var1 = param0.getParentFile();
      if (var1 != null) {
         Files.createDirectories(Util.toPath(var1));
      }

      ByteArrayOutputStream var2 = new ByteArrayOutputStream();
      DataOutputStream var3 = new DataOutputStream(var2);
      int var4 = 0;
      for (Entry var5 : param1) {
         var5.localHeaderOffset = var4;
         var5.writeLocalHeader(var3);
         var3.write(var5.data);
         var4 += var5.localHeaderSize();
      }

      int var6 = var4;
      for (Entry var7 : param1) {
         var7.writeCentralDirectoryRecord(var3);
         var6 += 46 + var7.nameBytes().length;
      }

      var3.writeInt(0x06054B50);
      var3.writeShort(0);
      var3.writeShort(0);
      var3.writeShort(param1.size());
      var3.writeShort(param1.size());
      var6 += 22;
      var3.writeInt(var6);
      var3.writeInt(var2.size());
      var3.flush();

      try (OutputStream var8 = Files.newOutputStream(Util.toPath(param0))) {
         var8.write(var2.toByteArray());
      }
   }

   /** One record in the archive: a local file header, its bytes, and a central directory entry. */
   private static final class Entry {
      final String name;
      final byte[] data;
      final boolean directory;
      final int crc;
      int localHeaderOffset;

      private Entry(String name, byte[] data, boolean directory) {
         this.name = name;
         this.data = data;
         this.directory = directory;
         CRC32 var1 = new CRC32();
         var1.update(data);
         this.crc = (int)var1.getValue();
      }

      static Entry file(String name, byte[] data) {
         return new Entry(name, data, false);
      }

      static Entry directory(String name) {
         return new Entry(name.endsWith("/") ? name : name + "/", new byte[0], true);
      }

      byte[] nameBytes() {
         return this.name.getBytes(StandardCharsets.UTF_8);
      }

      /** Size of this entry's local file header plus its data, i.e. the distance to the next entry. */
      int localHeaderSize() {
         return 30 + nameBytes().length + this.data.length;
      }

      void writeLocalHeader(DataOutputStream param0) throws IOException {
         param0.writeInt(0x04034B50);
         param0.writeShort(VERSION_NEEDED);
         param0.writeShort(FLAG_UTF8);
         param0.writeShort(METHOD_STORED);
         param0.writeShort(DOS_TIME);
         param0.writeShort(DOS_DATE);
         param0.writeInt(this.crc);
         param0.writeInt(this.data.length);
         param0.writeInt(this.data.length);
         byte[] var1 = nameBytes();
         param0.writeShort(var1.length);
         param0.writeShort(0);
         param0.write(var1);
      }

      void writeCentralDirectoryRecord(DataOutputStream param0) throws IOException {
         param0.writeInt(0x02014B50);
         param0.writeShort(VERSION_MADE_BY);
         param0.writeShort(VERSION_NEEDED);
         param0.writeShort(FLAG_UTF8);
         param0.writeShort(METHOD_STORED);
         param0.writeShort(DOS_TIME);
         param0.writeShort(DOS_DATE);
         param0.writeInt(this.crc);
         param0.writeInt(this.data.length);
         param0.writeInt(this.data.length);
         byte[] var1 = nameBytes();
         param0.writeShort(var1.length);
         param0.writeShort(0);
         param0.writeShort(0);
         param0.writeShort(0);
         param0.writeShort(0);
         param0.writeInt(this.directory ? EXTERNAL_ATTR_DIRECTORY : 0);
         param0.writeInt(this.localHeaderOffset);
         param0.write(var1);
      }
   }
}

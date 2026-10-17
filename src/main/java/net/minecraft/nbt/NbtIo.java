package net.minecraft.nbt;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.util.FastBufferedInputStream;

public class NbtIo {
   public static CompoundTag readCompressed(File param0) throws IOException {
      try (InputStream var1 = new FileInputStream(param0)) {
         return readCompressed(var1);
      }
   }

   private static DataInputStream createDecompressorStream(InputStream param0) throws IOException {
      return new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(param0)));
   }

   public static CompoundTag readCompressed(InputStream param0) throws IOException {
      try (DataInputStream var1 = createDecompressorStream(param0)) {
         return read(var1, NbtAccounter.UNLIMITED);
      }
   }

   public static void parseCompressed(File param0, StreamTagVisitor param1) throws IOException {
      try (InputStream var2 = new FileInputStream(param0)) {
         parseCompressed(var2, param1);
      }
   }

   public static void parseCompressed(InputStream param0, StreamTagVisitor param1) throws IOException {
      try (DataInputStream var2 = createDecompressorStream(param0)) {
         parse(var2, param1);
      }
   }

   public static void writeCompressed(CompoundTag param0, File param1) throws IOException {
      try (OutputStream var2 = new FileOutputStream(param1)) {
         writeCompressed(param0, var2);
      }
   }

   public static void writeCompressed(CompoundTag param0, OutputStream param1) throws IOException {
      try (DataOutputStream var2 = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(param1)))) {
         write(param0, var2);
      }
   }

   public static void write(CompoundTag param0, File param1) throws IOException {
      try (
         FileOutputStream var2 = new FileOutputStream(param1);
         DataOutputStream var3 = new DataOutputStream(var2);
      ) {
         write(param0, var3);
      }
   }

   @Nullable
   public static CompoundTag read(File param0) throws IOException {
      if (!param0.exists()) {
         return null;
      }

      try (
         FileInputStream var1 = new FileInputStream(param0);
         DataInputStream var2 = new DataInputStream(var1);
      ) {
         return read(var2, NbtAccounter.UNLIMITED);
      }
   }

   public static CompoundTag read(DataInput param0) throws IOException {
      return read(param0, NbtAccounter.UNLIMITED);
   }

   public static CompoundTag read(DataInput param0, NbtAccounter param1) throws IOException {
      Tag var2 = readUnnamedTag(param0, 0, param1);
      if (var2 instanceof CompoundTag) {
         return (CompoundTag)var2;
      } else {
         throw new IOException("Root tag must be a named compound tag");
      }
   }

   public static void write(CompoundTag param0, DataOutput param1) throws IOException {
      writeUnnamedTag(param0, param1);
   }

   public static void parse(DataInput param0, StreamTagVisitor param1) throws IOException {
      TagType<?> var2 = TagTypes.getType(param0.readByte());
      if (var2 == EndTag.TYPE) {
         if (param1.visitRootEntry(EndTag.TYPE) == StreamTagVisitor.ValueResult.CONTINUE) {
            param1.visitEnd();
         }
      } else {
         switch (param1.visitRootEntry(var2)) {
            case HALT:
            default:
               break;
            case BREAK:
               StringTag.skipString(param0);
               var2.skip(param0);
               break;
            case CONTINUE:
               StringTag.skipString(param0);
               var2.parse(param0, param1);
         }
      }
   }

   public static void writeUnnamedTag(Tag param0, DataOutput param1) throws IOException {
      param1.writeByte(param0.getId());
      if (param0.getId() != 0) {
         param1.writeUTF("");
         param0.write(param1);
      }
   }

   private static Tag readUnnamedTag(DataInput param0, int param1, NbtAccounter param2) throws IOException {
      byte var3 = param0.readByte();
      if (var3 == 0) {
         return EndTag.INSTANCE;
      }

      StringTag.skipString(param0);

      try {
         return TagTypes.getType(var3).load(param0, param1, param2);
      } catch (IOException var4) {
         CrashReport var5 = CrashReport.forThrowable(var4, "Loading NBT data");
         CrashReportCategory var6 = var5.addCategory("NBT Tag");
         var6.setDetail("Tag type", var3);
         throw new ReportedException(var5);
      }
   }
}

package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;

public class CompoundTag implements Tag {
   public static final Codec<CompoundTag> CODEC = Codec.PASSTHROUGH.comapFlatMap(param0 -> {
      Tag var1 = param0.convert(NbtOps.INSTANCE).getValue();
      return var1 instanceof CompoundTag ? DataResult.success((CompoundTag)var1) : DataResult.error(() -> "Not a compound tag: " + var1);
   }, param0 -> new Dynamic<>(NbtOps.INSTANCE, param0));
   private static final int SELF_SIZE_IN_BYTES = 48;
   private static final int MAP_ENTRY_SIZE_IN_BYTES = 32;
   public static final TagType<CompoundTag> TYPE = new TagType.VariableSize<CompoundTag>() {
      public CompoundTag load(DataInput param0, int param1, NbtAccounter param2) throws IOException {
         param2.accountBytes(48L);
         if (param1 > 512) {
            throw new RuntimeException("Tried to read NBT tag with too high complexity, depth > 512");
         }

         Map<String, Tag> var4 = Maps.newHashMap();

         byte var5;
         while ((var5 = CompoundTag.readNamedTagType(param0, param2)) != 0) {
            String var6 = CompoundTag.readNamedTagName(param0, param2);
            param2.accountBytes(28 + 2 * var6.length());
            Tag var7 = CompoundTag.readNamedTagData(TagTypes.getType(var5), var6, param0, param1 + 1, param2);
            if (var4.put(var6, var7) == null) {
               param2.accountBytes(36L);
            }
         }

         return new CompoundTag(var4);
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput param0, StreamTagVisitor param1) throws IOException {
         byte var3;
         label33:
         while ((var3 = param0.readByte()) != 0) {
            TagType<?> var4 = TagTypes.getType(var3);
            switch (param1.visitEntry(var4)) {
               case HALT:
                  return StreamTagVisitor.ValueResult.HALT;
               case BREAK:
                  StringTag.skipString(param0);
                  var4.skip(param0);
                  break label33;
               case SKIP:
                  StringTag.skipString(param0);
                  var4.skip(param0);
                  break;
               default:
                  String var5 = param0.readUTF();
                  switch (param1.visitEntry(var4, var5)) {
                     case HALT:
                        return StreamTagVisitor.ValueResult.HALT;
                     case BREAK:
                        var4.skip(param0);
                        break label33;
                     case SKIP:
                        var4.skip(param0);
                        break;
                     default:
                        switch (var4.parse(param0, param1)) {
                           case HALT:
                              return StreamTagVisitor.ValueResult.HALT;
                           case BREAK:
                        }
                  }
            }
         }

         if (var3 != 0) {
            while ((var3 = param0.readByte()) != 0) {
               StringTag.skipString(param0);
               TagTypes.getType(var3).skip(param0);
            }
         }

         return param1.visitContainerEnd();
      }

      @Override
      public void skip(DataInput param0) throws IOException {
         byte var2;
         while ((var2 = param0.readByte()) != 0) {
            StringTag.skipString(param0);
            TagTypes.getType(var2).skip(param0);
         }
      }

      @Override
      public String getName() {
         return "COMPOUND";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Compound";
      }
   };
   private final Map<String, Tag> tags;

   protected CompoundTag(Map<String, Tag> param0) {
      this.tags = param0;
   }

   public CompoundTag() {
      this(Maps.newHashMap());
   }

   @Override
   public void write(DataOutput param0) throws IOException {
      for (String var3 : this.tags.keySet()) {
         Tag var4 = this.tags.get(var3);
         writeNamedTag(var3, var4, param0);
      }

      param0.writeByte(0);
   }

   @Override
   public int sizeInBytes() {
      int var1 = 48;

      for (Entry<String, Tag> var3 : this.tags.entrySet()) {
         var1 += 28 + 2 * var3.getKey().length();
         var1 += 36;
         var1 += var3.getValue().sizeInBytes();
      }

      return var1;
   }

   public Set<String> getAllKeys() {
      return this.tags.keySet();
   }

   @Override
   public byte getId() {
      return 10;
   }

   @Override
   public TagType<CompoundTag> getType() {
      return TYPE;
   }

   public int size() {
      return this.tags.size();
   }

   @Nullable
   public Tag put(String param0, Tag param1) {
      return this.tags.put(param0, param1);
   }

   public void putByte(String param0, byte param1) {
      this.tags.put(param0, ByteTag.valueOf(param1));
   }

   public void putShort(String param0, short param1) {
      this.tags.put(param0, ShortTag.valueOf(param1));
   }

   public void putInt(String param0, int param1) {
      this.tags.put(param0, IntTag.valueOf(param1));
   }

   public void putLong(String param0, long param1) {
      this.tags.put(param0, LongTag.valueOf(param1));
   }

   public void putUUID(String param0, UUID param1) {
      this.tags.put(param0, NbtUtils.createUUID(param1));
   }

   public UUID getUUID(String param0) {
      return NbtUtils.loadUUID(this.get(param0));
   }

   public boolean hasUUID(String param0) {
      Tag var2 = this.get(param0);
      return var2 != null && var2.getType() == IntArrayTag.TYPE && ((IntArrayTag)var2).getAsIntArray().length == 4;
   }

   public void putFloat(String param0, float param1) {
      this.tags.put(param0, FloatTag.valueOf(param1));
   }

   public void putDouble(String param0, double param1) {
      this.tags.put(param0, DoubleTag.valueOf(param1));
   }

   public void putString(String param0, String param1) {
      this.tags.put(param0, StringTag.valueOf(param1));
   }

   public void putByteArray(String param0, byte[] param1) {
      this.tags.put(param0, new ByteArrayTag(param1));
   }

   public void putByteArray(String param0, List<Byte> param1) {
      this.tags.put(param0, new ByteArrayTag(param1));
   }

   public void putIntArray(String param0, int[] param1) {
      this.tags.put(param0, new IntArrayTag(param1));
   }

   public void putIntArray(String param0, List<Integer> param1) {
      this.tags.put(param0, new IntArrayTag(param1));
   }

   public void putLongArray(String param0, long[] param1) {
      this.tags.put(param0, new LongArrayTag(param1));
   }

   public void putLongArray(String param0, List<Long> param1) {
      this.tags.put(param0, new LongArrayTag(param1));
   }

   public void putBoolean(String param0, boolean param1) {
      this.tags.put(param0, ByteTag.valueOf(param1));
   }

   @Nullable
   public Tag get(String param0) {
      return this.tags.get(param0);
   }

   public byte getTagType(String param0) {
      Tag var2 = this.tags.get(param0);
      return var2 == null ? 0 : var2.getId();
   }

   public boolean contains(String param0) {
      return this.tags.containsKey(param0);
   }

   public boolean contains(String param0, int param1) {
      int var3 = this.getTagType(param0);
      if (var3 == param1) {
         return true;
      } else {
         return param1 != 99 ? false : var3 == 1 || var3 == 2 || var3 == 3 || var3 == 4 || var3 == 5 || var3 == 6;
      }
   }

   public byte getByte(String param0) {
      try {
         if (this.contains(param0, 99)) {
            return ((NumericTag)this.tags.get(param0)).getAsByte();
         }
      } catch (ClassCastException var3) {
      }

      return 0;
   }

   public short getShort(String param0) {
      try {
         if (this.contains(param0, 99)) {
            return ((NumericTag)this.tags.get(param0)).getAsShort();
         }
      } catch (ClassCastException var3) {
      }

      return 0;
   }

   public int getInt(String param0) {
      try {
         if (this.contains(param0, 99)) {
            return ((NumericTag)this.tags.get(param0)).getAsInt();
         }
      } catch (ClassCastException var3) {
      }

      return 0;
   }

   public long getLong(String param0) {
      try {
         if (this.contains(param0, 99)) {
            return ((NumericTag)this.tags.get(param0)).getAsLong();
         }
      } catch (ClassCastException var3) {
      }

      return 0L;
   }

   public float getFloat(String param0) {
      try {
         if (this.contains(param0, 99)) {
            return ((NumericTag)this.tags.get(param0)).getAsFloat();
         }
      } catch (ClassCastException var3) {
      }

      return 0.0F;
   }

   public double getDouble(String param0) {
      try {
         if (this.contains(param0, 99)) {
            return ((NumericTag)this.tags.get(param0)).getAsDouble();
         }
      } catch (ClassCastException var3) {
      }

      return 0.0;
   }

   public String getString(String param0) {
      try {
         if (this.contains(param0, 8)) {
            return this.tags.get(param0).getAsString();
         }
      } catch (ClassCastException var3) {
      }

      return "";
   }

   public byte[] getByteArray(String param0) {
      try {
         if (this.contains(param0, 7)) {
            return ((ByteArrayTag)this.tags.get(param0)).getAsByteArray();
         }
      } catch (ClassCastException var2) {
         throw new ReportedException(this.createReport(param0, ByteArrayTag.TYPE, var2));
      }

      return new byte[0];
   }

   public int[] getIntArray(String param0) {
      try {
         if (this.contains(param0, 11)) {
            return ((IntArrayTag)this.tags.get(param0)).getAsIntArray();
         }
      } catch (ClassCastException var2) {
         throw new ReportedException(this.createReport(param0, IntArrayTag.TYPE, var2));
      }

      return new int[0];
   }

   public long[] getLongArray(String param0) {
      try {
         if (this.contains(param0, 12)) {
            return ((LongArrayTag)this.tags.get(param0)).getAsLongArray();
         }
      } catch (ClassCastException var2) {
         throw new ReportedException(this.createReport(param0, LongArrayTag.TYPE, var2));
      }

      return new long[0];
   }

   public CompoundTag getCompound(String param0) {
      try {
         if (this.contains(param0, 10)) {
            return (CompoundTag)this.tags.get(param0);
         }
      } catch (ClassCastException var2) {
         throw new ReportedException(this.createReport(param0, TYPE, var2));
      }

      return new CompoundTag();
   }

   public ListTag getList(String param0, int param1) {
      try {
         if (this.getTagType(param0) == 9) {
            ListTag var3 = (ListTag)this.tags.get(param0);
            if (!var3.isEmpty() && var3.getElementType() != param1) {
               return new ListTag();
            }

            return var3;
         }
      } catch (ClassCastException var3) {
         throw new ReportedException(this.createReport(param0, ListTag.TYPE, var3));
      }

      return new ListTag();
   }

   public boolean getBoolean(String param0) {
      return this.getByte(param0) != 0;
   }

   public void remove(String param0) {
      this.tags.remove(param0);
   }

   @Override
   public String toString() {
      return this.getAsString();
   }

   public boolean isEmpty() {
      return this.tags.isEmpty();
   }

   private CrashReport createReport(String param0, TagType<?> param1, ClassCastException param2) {
      CrashReport var4 = CrashReport.forThrowable(param2, "Reading NBT data");
      CrashReportCategory var5 = var4.addCategory("Corrupt NBT tag", 1);
      var5.setDetail("Tag type found", () -> this.tags.get(param0).getType().getName());
      var5.setDetail("Tag type expected", param1::getName);
      var5.setDetail("Tag name", param0);
      return var4;
   }

   public CompoundTag copy() {
      Map<String, Tag> var1 = Maps.newHashMap(Maps.transformValues(this.tags, Tag::copy));
      return new CompoundTag(var1);
   }

   @Override
   public boolean equals(Object param0) {
      return this == param0 ? true : param0 instanceof CompoundTag && Objects.equals(this.tags, ((CompoundTag)param0).tags);
   }

   @Override
   public int hashCode() {
      return this.tags.hashCode();
   }

   private static void writeNamedTag(String param0, Tag param1, DataOutput param2) throws IOException {
      param2.writeByte(param1.getId());
      if (param1.getId() != 0) {
         param2.writeUTF(param0);
         param1.write(param2);
      }
   }

   static byte readNamedTagType(DataInput param0, NbtAccounter param1) throws IOException {
      return param0.readByte();
   }

   static String readNamedTagName(DataInput param0, NbtAccounter param1) throws IOException {
      return param0.readUTF();
   }

   static Tag readNamedTagData(TagType<?> param0, String param1, DataInput param2, int param3, NbtAccounter param4) {
      try {
         return param0.load(param2, param3, param4);
      } catch (IOException var5) {
         CrashReport var6 = CrashReport.forThrowable(var5, "Loading NBT data");
         CrashReportCategory var7 = var6.addCategory("NBT Tag");
         var7.setDetail("Tag name", param1);
         var7.setDetail("Tag type", param0.getName());
         throw new ReportedException(var6);
      }
   }

   public CompoundTag merge(CompoundTag param0) {
      for (String var3 : param0.tags.keySet()) {
         Tag var4 = param0.tags.get(var3);
         if (var4.getId() == 10) {
            if (this.contains(var3, 10)) {
               CompoundTag var5 = this.getCompound(var3);
               var5.merge((CompoundTag)var4);
            } else {
               this.put(var3, var4.copy());
            }
         } else {
            this.put(var3, var4.copy());
         }
      }

      return this;
   }

   @Override
   public void accept(TagVisitor param0) {
      param0.visitCompound(this);
   }

   protected Map<String, Tag> entries() {
      return Collections.unmodifiableMap(this.tags);
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor param0) {
      for (Entry<String, Tag> var3 : this.tags.entrySet()) {
         Tag var4 = var3.getValue();
         TagType<?> var5 = var4.getType();
         StreamTagVisitor.EntryResult var6 = param0.visitEntry(var5);
         switch (var6) {
            case HALT:
               return StreamTagVisitor.ValueResult.HALT;
            case BREAK:
               return param0.visitContainerEnd();
            case SKIP:
               break;
            default:
               var6 = param0.visitEntry(var5, var3.getKey());
               switch (var6) {
                  case HALT:
                     return StreamTagVisitor.ValueResult.HALT;
                  case BREAK:
                     return param0.visitContainerEnd();
                  case SKIP:
                     break;
                  default:
                     StreamTagVisitor.ValueResult var7 = var4.accept(param0);
                     switch (var7) {
                        case HALT:
                           return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                           return param0.visitContainerEnd();
                     }
               }
         }
      }

      return param0.visitContainerEnd();
   }
}

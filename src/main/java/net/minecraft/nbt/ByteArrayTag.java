package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class ByteArrayTag extends CollectionTag<ByteTag> {
   private static final int SELF_SIZE_IN_BYTES = 24;
   public static final TagType<ByteArrayTag> TYPE = new TagType.VariableSize<ByteArrayTag>() {
      public ByteArrayTag load(DataInput param0, int param1, NbtAccounter param2) throws IOException {
         param2.accountBytes(24L);
         int var4 = param0.readInt();
         param2.accountBytes(1L * var4);
         byte[] var5 = new byte[var4];
         param0.readFully(var5);
         return new ByteArrayTag(var5);
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput param0, StreamTagVisitor param1) throws IOException {
         int var3 = param0.readInt();
         byte[] var4 = new byte[var3];
         param0.readFully(var4);
         return param1.visit(var4);
      }

      @Override
      public void skip(DataInput param0) throws IOException {
         param0.skipBytes(param0.readInt() * 1);
      }

      @Override
      public String getName() {
         return "BYTE[]";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Byte_Array";
      }
   };
   private byte[] data;

   public ByteArrayTag(byte[] param0) {
      this.data = param0;
   }

   public ByteArrayTag(List<Byte> param0) {
      this(toArray(param0));
   }

   private static byte[] toArray(List<Byte> param0) {
      byte[] var1 = new byte[param0.size()];

      for (int var2 = 0; var2 < param0.size(); var2++) {
         Byte var3 = param0.get(var2);
         var1[var2] = var3 == null ? 0 : var3;
      }

      return var1;
   }

   @Override
   public void write(DataOutput param0) throws IOException {
      param0.writeInt(this.data.length);
      param0.write(this.data);
   }

   @Override
   public int sizeInBytes() {
      return 24 + 1 * this.data.length;
   }

   @Override
   public byte getId() {
      return 7;
   }

   @Override
   public TagType<ByteArrayTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      return this.getAsString();
   }

   @Override
   public Tag copy() {
      byte[] var1 = new byte[this.data.length];
      System.arraycopy(this.data, 0, var1, 0, this.data.length);
      return new ByteArrayTag(var1);
   }

   @Override
   public boolean equals(Object param0) {
      return this == param0 ? true : param0 instanceof ByteArrayTag && Arrays.equals(this.data, ((ByteArrayTag)param0).data);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.data);
   }

   @Override
   public void accept(TagVisitor param0) {
      param0.visitByteArray(this);
   }

   public byte[] getAsByteArray() {
      return this.data;
   }

   @Override
   public int size() {
      return this.data.length;
   }

   public ByteTag get(int param0) {
      return ByteTag.valueOf(this.data[param0]);
   }

   public ByteTag set(int param0, ByteTag param1) {
      byte var3 = this.data[param0];
      this.data[param0] = param1.getAsByte();
      return ByteTag.valueOf(var3);
   }

   public void add(int param0, ByteTag param1) {
      this.data = ArrayUtils.add(this.data, param0, param1.getAsByte());
   }

   @Override
   public boolean setTag(int param0, Tag param1) {
      if (param1 instanceof NumericTag) {
         this.data[param0] = ((NumericTag)param1).getAsByte();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addTag(int param0, Tag param1) {
      if (param1 instanceof NumericTag) {
         this.data = ArrayUtils.add(this.data, param0, ((NumericTag)param1).getAsByte());
         return true;
      } else {
         return false;
      }
   }

   public ByteTag remove(int param0) {
      byte var2 = this.data[param0];
      this.data = ArrayUtils.remove(this.data, param0);
      return ByteTag.valueOf(var2);
   }

   @Override
   public byte getElementType() {
      return 1;
   }

   @Override
   public void clear() {
      this.data = new byte[0];
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor param0) {
      return param0.visit(this.data);
   }
}

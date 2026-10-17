package net.minecraft.nbt;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class LongArrayTag extends CollectionTag<LongTag> {
   private static final int SELF_SIZE_IN_BYTES = 24;
   public static final TagType<LongArrayTag> TYPE = new TagType.VariableSize<LongArrayTag>() {
      public LongArrayTag load(DataInput param0, int param1, NbtAccounter param2) throws IOException {
         param2.accountBytes(24L);
         int var4 = param0.readInt();
         param2.accountBytes(8L * var4);
         long[] var5 = new long[var4];

         for (int var6 = 0; var6 < var4; var6++) {
            var5[var6] = param0.readLong();
         }

         return new LongArrayTag(var5);
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput param0, StreamTagVisitor param1) throws IOException {
         int var3 = param0.readInt();
         long[] var4 = new long[var3];

         for (int var5 = 0; var5 < var3; var5++) {
            var4[var5] = param0.readLong();
         }

         return param1.visit(var4);
      }

      @Override
      public void skip(DataInput param0) throws IOException {
         param0.skipBytes(param0.readInt() * 8);
      }

      @Override
      public String getName() {
         return "LONG[]";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Long_Array";
      }
   };
   private long[] data;

   public LongArrayTag(long[] param0) {
      this.data = param0;
   }

   public LongArrayTag(LongSet param0) {
      this.data = param0.toLongArray();
   }

   public LongArrayTag(List<Long> param0) {
      this(toArray(param0));
   }

   private static long[] toArray(List<Long> param0) {
      long[] var1 = new long[param0.size()];

      for (int var2 = 0; var2 < param0.size(); var2++) {
         Long var3 = param0.get(var2);
         var1[var2] = var3 == null ? 0L : var3;
      }

      return var1;
   }

   @Override
   public void write(DataOutput param0) throws IOException {
      param0.writeInt(this.data.length);

      for (long var5 : this.data) {
         param0.writeLong(var5);
      }
   }

   @Override
   public int sizeInBytes() {
      return 24 + 8 * this.data.length;
   }

   @Override
   public byte getId() {
      return 12;
   }

   @Override
   public TagType<LongArrayTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      return this.getAsString();
   }

   public LongArrayTag copy() {
      long[] var1 = new long[this.data.length];
      System.arraycopy(this.data, 0, var1, 0, this.data.length);
      return new LongArrayTag(var1);
   }

   @Override
   public boolean equals(Object param0) {
      return this == param0 ? true : param0 instanceof LongArrayTag && Arrays.equals(this.data, ((LongArrayTag)param0).data);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.data);
   }

   @Override
   public void accept(TagVisitor param0) {
      param0.visitLongArray(this);
   }

   public long[] getAsLongArray() {
      return this.data;
   }

   @Override
   public int size() {
      return this.data.length;
   }

   public LongTag get(int param0) {
      return LongTag.valueOf(this.data[param0]);
   }

   public LongTag set(int param0, LongTag param1) {
      long var3 = this.data[param0];
      this.data[param0] = param1.getAsLong();
      return LongTag.valueOf(var3);
   }

   public void add(int param0, LongTag param1) {
      this.data = ArrayUtils.add(this.data, param0, param1.getAsLong());
   }

   @Override
   public boolean setTag(int param0, Tag param1) {
      if (param1 instanceof NumericTag) {
         this.data[param0] = ((NumericTag)param1).getAsLong();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addTag(int param0, Tag param1) {
      if (param1 instanceof NumericTag) {
         this.data = ArrayUtils.add(this.data, param0, ((NumericTag)param1).getAsLong());
         return true;
      } else {
         return false;
      }
   }

   public LongTag remove(int param0) {
      long var2 = this.data[param0];
      this.data = ArrayUtils.remove(this.data, param0);
      return LongTag.valueOf(var2);
   }

   @Override
   public byte getElementType() {
      return 4;
   }

   @Override
   public void clear() {
      this.data = new long[0];
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor param0) {
      return param0.visit(this.data);
   }
}

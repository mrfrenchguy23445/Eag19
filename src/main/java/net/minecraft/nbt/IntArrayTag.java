package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class IntArrayTag extends CollectionTag<IntTag> {
   private static final int SELF_SIZE_IN_BYTES = 24;
   public static final TagType<IntArrayTag> TYPE = new TagType.VariableSize<IntArrayTag>() {
      public IntArrayTag load(DataInput param0, int param1, NbtAccounter param2) throws IOException {
         param2.accountBytes(24L);
         int var4 = param0.readInt();
         param2.accountBytes(4L * var4);
         int[] var5 = new int[var4];

         for (int var6 = 0; var6 < var4; var6++) {
            var5[var6] = param0.readInt();
         }

         return new IntArrayTag(var5);
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput param0, StreamTagVisitor param1) throws IOException {
         int var3 = param0.readInt();
         int[] var4 = new int[var3];

         for (int var5 = 0; var5 < var3; var5++) {
            var4[var5] = param0.readInt();
         }

         return param1.visit(var4);
      }

      @Override
      public void skip(DataInput param0) throws IOException {
         param0.skipBytes(param0.readInt() * 4);
      }

      @Override
      public String getName() {
         return "INT[]";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Int_Array";
      }
   };
   private int[] data;

   public IntArrayTag(int[] param0) {
      this.data = param0;
   }

   public IntArrayTag(List<Integer> param0) {
      this(toArray(param0));
   }

   private static int[] toArray(List<Integer> param0) {
      int[] var1 = new int[param0.size()];

      for (int var2 = 0; var2 < param0.size(); var2++) {
         Integer var3 = param0.get(var2);
         var1[var2] = var3 == null ? 0 : var3;
      }

      return var1;
   }

   @Override
   public void write(DataOutput param0) throws IOException {
      param0.writeInt(this.data.length);

      for (int var5 : this.data) {
         param0.writeInt(var5);
      }
   }

   @Override
   public int sizeInBytes() {
      return 24 + 4 * this.data.length;
   }

   @Override
   public byte getId() {
      return 11;
   }

   @Override
   public TagType<IntArrayTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      return this.getAsString();
   }

   public IntArrayTag copy() {
      int[] var1 = new int[this.data.length];
      System.arraycopy(this.data, 0, var1, 0, this.data.length);
      return new IntArrayTag(var1);
   }

   @Override
   public boolean equals(Object param0) {
      return this == param0 ? true : param0 instanceof IntArrayTag && Arrays.equals(this.data, ((IntArrayTag)param0).data);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.data);
   }

   public int[] getAsIntArray() {
      return this.data;
   }

   @Override
   public void accept(TagVisitor param0) {
      param0.visitIntArray(this);
   }

   @Override
   public int size() {
      return this.data.length;
   }

   public IntTag get(int param0) {
      return IntTag.valueOf(this.data[param0]);
   }

   public IntTag set(int param0, IntTag param1) {
      int var3 = this.data[param0];
      this.data[param0] = param1.getAsInt();
      return IntTag.valueOf(var3);
   }

   public void add(int param0, IntTag param1) {
      this.data = ArrayUtils.add(this.data, param0, param1.getAsInt());
   }

   @Override
   public boolean setTag(int param0, Tag param1) {
      if (param1 instanceof NumericTag) {
         this.data[param0] = ((NumericTag)param1).getAsInt();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addTag(int param0, Tag param1) {
      if (param1 instanceof NumericTag) {
         this.data = ArrayUtils.add(this.data, param0, ((NumericTag)param1).getAsInt());
         return true;
      } else {
         return false;
      }
   }

   public IntTag remove(int param0) {
      int var2 = this.data[param0];
      this.data = ArrayUtils.remove(this.data, param0);
      return IntTag.valueOf(var2);
   }

   @Override
   public byte getElementType() {
      return 3;
   }

   @Override
   public void clear() {
      this.data = new int[0];
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor param0) {
      return param0.visit(this.data);
   }
}

package net.minecraft.nbt;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ListTag extends CollectionTag<Tag> {
   private static final int SELF_SIZE_IN_BYTES = 37;
   public static final TagType<ListTag> TYPE = new TagType.VariableSize<ListTag>() {
      public ListTag load(DataInput param0, int param1, NbtAccounter param2) throws IOException {
         param2.accountBytes(37L);
         if (param1 > 512) {
            throw new RuntimeException("Tried to read NBT tag with too high complexity, depth > 512");
         }

         byte var4 = param0.readByte();
         int var5 = param0.readInt();
         if (var4 == 0 && var5 > 0) {
            throw new RuntimeException("Missing type on ListTag");
         }

         param2.accountBytes(4L * var5);
         TagType<?> var6 = TagTypes.getType(var4);
         List<Tag> var7 = Lists.newArrayListWithCapacity(var5);

         for (int var8 = 0; var8 < var5; var8++) {
            var7.add(var6.load(param0, param1 + 1, param2));
         }

         return new ListTag(var7, var4);
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput param0, StreamTagVisitor param1) throws IOException {
         TagType<?> var3 = TagTypes.getType(param0.readByte());
         int var4 = param0.readInt();
         switch (param1.visitList(var3, var4)) {
            case HALT:
               return StreamTagVisitor.ValueResult.HALT;
            case BREAK:
               var3.skip(param0, var4);
               return param1.visitContainerEnd();
            default:
               int var5 = 0;

               while (true) {
                  label41: {
                     if (var5 < var4) {
                        switch (param1.visitElement(var3, var5)) {
                           case HALT:
                              return StreamTagVisitor.ValueResult.HALT;
                           case BREAK:
                              var3.skip(param0);
                              break;
                           case SKIP:
                              var3.skip(param0);
                              break label41;
                           default:
                              switch (var3.parse(param0, param1)) {
                                 case HALT:
                                    return StreamTagVisitor.ValueResult.HALT;
                                 case BREAK:
                                    break;
                                 default:
                                    break label41;
                              }
                        }
                     }

                     int var6 = var4 - 1 - var5;
                     if (var6 > 0) {
                        var3.skip(param0, var6);
                     }

                     return param1.visitContainerEnd();
                  }

                  var5++;
               }
         }
      }

      @Override
      public void skip(DataInput param0) throws IOException {
         TagType<?> var2 = TagTypes.getType(param0.readByte());
         int var3 = param0.readInt();
         var2.skip(param0, var3);
      }

      @Override
      public String getName() {
         return "LIST";
      }

      @Override
      public String getPrettyName() {
         return "TAG_List";
      }
   };
   private final List<Tag> list;
   private byte type;

   ListTag(List<Tag> param0, byte param1) {
      this.list = param0;
      this.type = param1;
   }

   public ListTag() {
      this(Lists.newArrayList(), (byte)0);
   }

   @Override
   public void write(DataOutput param0) throws IOException {
      if (this.list.isEmpty()) {
         this.type = 0;
      } else {
         this.type = this.list.get(0).getId();
      }

      param0.writeByte(this.type);
      param0.writeInt(this.list.size());

      for (Tag var3 : this.list) {
         var3.write(param0);
      }
   }

   @Override
   public int sizeInBytes() {
      int var1 = 37;
      var1 += 4 * this.list.size();

      for (Tag var3 : this.list) {
         var1 += var3.sizeInBytes();
      }

      return var1;
   }

   @Override
   public byte getId() {
      return 9;
   }

   @Override
   public TagType<ListTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      return this.getAsString();
   }

   private void updateTypeAfterRemove() {
      if (this.list.isEmpty()) {
         this.type = 0;
      }
   }

   @Override
   public Tag remove(int param0) {
      Tag var2 = this.list.remove(param0);
      this.updateTypeAfterRemove();
      return var2;
   }

   @Override
   public boolean isEmpty() {
      return this.list.isEmpty();
   }

   public CompoundTag getCompound(int param0) {
      if (param0 >= 0 && param0 < this.list.size()) {
         Tag var2 = this.list.get(param0);
         if (var2.getId() == 10) {
            return (CompoundTag)var2;
         }
      }

      return new CompoundTag();
   }

   public ListTag getList(int param0) {
      if (param0 >= 0 && param0 < this.list.size()) {
         Tag var2 = this.list.get(param0);
         if (var2.getId() == 9) {
            return (ListTag)var2;
         }
      }

      return new ListTag();
   }

   public short getShort(int param0) {
      if (param0 >= 0 && param0 < this.list.size()) {
         Tag var2 = this.list.get(param0);
         if (var2.getId() == 2) {
            return ((ShortTag)var2).getAsShort();
         }
      }

      return 0;
   }

   public int getInt(int param0) {
      if (param0 >= 0 && param0 < this.list.size()) {
         Tag var2 = this.list.get(param0);
         if (var2.getId() == 3) {
            return ((IntTag)var2).getAsInt();
         }
      }

      return 0;
   }

   public int[] getIntArray(int param0) {
      if (param0 >= 0 && param0 < this.list.size()) {
         Tag var2 = this.list.get(param0);
         if (var2.getId() == 11) {
            return ((IntArrayTag)var2).getAsIntArray();
         }
      }

      return new int[0];
   }

   public long[] getLongArray(int param0) {
      if (param0 >= 0 && param0 < this.list.size()) {
         Tag var2 = this.list.get(param0);
         if (var2.getId() == 11) {
            return ((LongArrayTag)var2).getAsLongArray();
         }
      }

      return new long[0];
   }

   public double getDouble(int param0) {
      if (param0 >= 0 && param0 < this.list.size()) {
         Tag var2 = this.list.get(param0);
         if (var2.getId() == 6) {
            return ((DoubleTag)var2).getAsDouble();
         }
      }

      return 0.0;
   }

   public float getFloat(int param0) {
      if (param0 >= 0 && param0 < this.list.size()) {
         Tag var2 = this.list.get(param0);
         if (var2.getId() == 5) {
            return ((FloatTag)var2).getAsFloat();
         }
      }

      return 0.0F;
   }

   public String getString(int param0) {
      if (param0 >= 0 && param0 < this.list.size()) {
         Tag var2 = this.list.get(param0);
         return var2.getId() == 8 ? var2.getAsString() : var2.toString();
      } else {
         return "";
      }
   }

   @Override
   public int size() {
      return this.list.size();
   }

   public Tag get(int param0) {
      return this.list.get(param0);
   }

   @Override
   public Tag set(int param0, Tag param1) {
      Tag var3 = this.get(param0);
      if (!this.setTag(param0, param1)) {
         throw new UnsupportedOperationException(String.format(Locale.ROOT, "Trying to add tag of type %d to list of %d", param1.getId(), this.type));
      } else {
         return var3;
      }
   }

   @Override
   public void add(int param0, Tag param1) {
      if (!this.addTag(param0, param1)) {
         throw new UnsupportedOperationException(String.format(Locale.ROOT, "Trying to add tag of type %d to list of %d", param1.getId(), this.type));
      }
   }

   @Override
   public boolean setTag(int param0, Tag param1) {
      if (this.updateType(param1)) {
         this.list.set(param0, param1);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addTag(int param0, Tag param1) {
      if (this.updateType(param1)) {
         this.list.add(param0, param1);
         return true;
      } else {
         return false;
      }
   }

   private boolean updateType(Tag param0) {
      if (param0.getId() == 0) {
         return false;
      } else if (this.type == 0) {
         this.type = param0.getId();
         return true;
      } else {
         return this.type == param0.getId();
      }
   }

   public ListTag copy() {
      Iterable<Tag> var1 = TagTypes.getType(this.type).isValue() ? this.list : Iterables.transform(this.list, Tag::copy);
      List<Tag> var2 = Lists.newArrayList(var1);
      return new ListTag(var2, this.type);
   }

   @Override
   public boolean equals(Object param0) {
      return this == param0 ? true : param0 instanceof ListTag && Objects.equals(this.list, ((ListTag)param0).list);
   }

   @Override
   public int hashCode() {
      return this.list.hashCode();
   }

   @Override
   public void accept(TagVisitor param0) {
      param0.visitList(this);
   }

   @Override
   public byte getElementType() {
      return this.type;
   }

   @Override
   public void clear() {
      this.list.clear();
      this.type = 0;
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor param0) {
      switch (param0.visitList(TagTypes.getType(this.type), this.list.size())) {
         case HALT:
            return StreamTagVisitor.ValueResult.HALT;
         case BREAK:
            return param0.visitContainerEnd();
         default:
            int var2 = 0;

            while (var2 < this.list.size()) {
               Tag var3 = this.list.get(var2);
               switch (param0.visitElement(var3.getType(), var2)) {
                  case HALT:
                     return StreamTagVisitor.ValueResult.HALT;
                  case BREAK:
                     return param0.visitContainerEnd();
                  default:
                     switch (var3.accept(param0)) {
                        case HALT:
                           return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                           return param0.visitContainerEnd();
                     }
                  case SKIP:
                     var2++;
               }
            }

            return param0.visitContainerEnd();
      }
   }
}

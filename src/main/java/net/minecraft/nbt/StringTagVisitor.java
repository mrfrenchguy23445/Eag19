package net.minecraft.nbt;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class StringTagVisitor implements TagVisitor {
   private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
   private final StringBuilder builder = new StringBuilder();

   public String visit(Tag param0) {
      param0.accept(this);
      return this.builder.toString();
   }

   @Override
   public void visitString(StringTag param0) {
      this.builder.append(StringTag.quoteAndEscape(param0.getAsString()));
   }

   @Override
   public void visitByte(ByteTag param0) {
      this.builder.append(param0.getAsNumber()).append('b');
   }

   @Override
   public void visitShort(ShortTag param0) {
      this.builder.append(param0.getAsNumber()).append('s');
   }

   @Override
   public void visitInt(IntTag param0) {
      this.builder.append(param0.getAsNumber());
   }

   @Override
   public void visitLong(LongTag param0) {
      this.builder.append(param0.getAsNumber()).append('L');
   }

   @Override
   public void visitFloat(FloatTag param0) {
      this.builder.append(param0.getAsFloat()).append('f');
   }

   @Override
   public void visitDouble(DoubleTag param0) {
      this.builder.append(param0.getAsDouble()).append('d');
   }

   @Override
   public void visitByteArray(ByteArrayTag param0) {
      this.builder.append("[B;");
      byte[] var2 = param0.getAsByteArray();

      for (int var3 = 0; var3 < var2.length; var3++) {
         if (var3 != 0) {
            this.builder.append(',');
         }

         this.builder.append(var2[var3]).append('B');
      }

      this.builder.append(']');
   }

   @Override
   public void visitIntArray(IntArrayTag param0) {
      this.builder.append("[I;");
      int[] var2 = param0.getAsIntArray();

      for (int var3 = 0; var3 < var2.length; var3++) {
         if (var3 != 0) {
            this.builder.append(',');
         }

         this.builder.append(var2[var3]);
      }

      this.builder.append(']');
   }

   @Override
   public void visitLongArray(LongArrayTag param0) {
      this.builder.append("[L;");
      long[] var2 = param0.getAsLongArray();

      for (int var3 = 0; var3 < var2.length; var3++) {
         if (var3 != 0) {
            this.builder.append(',');
         }

         this.builder.append(var2[var3]).append('L');
      }

      this.builder.append(']');
   }

   @Override
   public void visitList(ListTag param0) {
      this.builder.append('[');

      for (int var2 = 0; var2 < param0.size(); var2++) {
         if (var2 != 0) {
            this.builder.append(',');
         }

         this.builder.append(new StringTagVisitor().visit(param0.get(var2)));
      }

      this.builder.append(']');
   }

   @Override
   public void visitCompound(CompoundTag param0) {
      this.builder.append('{');
      List<String> var2 = Lists.newArrayList(param0.getAllKeys());
      Collections.sort(var2);

      for (String var4 : var2) {
         if (this.builder.length() != 1) {
            this.builder.append(',');
         }

         this.builder.append(handleEscape(var4)).append(':').append(new StringTagVisitor().visit(param0.get(var4)));
      }

      this.builder.append('}');
   }

   protected static String handleEscape(String param0) {
      return SIMPLE_VALUE.matcher(param0).matches() ? param0 : StringTag.quoteAndEscape(param0);
   }

   @Override
   public void visitEnd(EndTag param0) {
      this.builder.append("END");
   }
}

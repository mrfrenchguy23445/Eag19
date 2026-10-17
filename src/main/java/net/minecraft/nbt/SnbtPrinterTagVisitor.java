package net.minecraft.nbt;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.Util;

public class SnbtPrinterTagVisitor implements TagVisitor {
   private static final Map<String, List<String>> KEY_ORDER = Util.make(Maps.newHashMap(), param0 -> {
      param0.put("{}", Lists.newArrayList("DataVersion", "author", "size", "data", "entities", "palette", "palettes"));
      param0.put("{}.data.[].{}", Lists.newArrayList("pos", "state", "nbt"));
      param0.put("{}.entities.[].{}", Lists.newArrayList("blockPos", "pos"));
   });
   private static final Set<String> NO_INDENTATION = Sets.newHashSet("{}.size.[]", "{}.data.[].{}", "{}.palette.[].{}", "{}.entities.[].{}");
   private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
   private static final String NAME_VALUE_SEPARATOR = String.valueOf(':');
   private static final String ELEMENT_SEPARATOR = String.valueOf(',');
   private static final String LIST_OPEN = "[";
   private static final String LIST_CLOSE = "]";
   private static final String LIST_TYPE_SEPARATOR = ";";
   private static final String ELEMENT_SPACING = " ";
   private static final String STRUCT_OPEN = "{";
   private static final String STRUCT_CLOSE = "}";
   private static final String NEWLINE = "\n";
   private final String indentation;
   private final int depth;
   private final List<String> path;
   private String result = "";

   public SnbtPrinterTagVisitor() {
      this("    ", 0, Lists.newArrayList());
   }

   public SnbtPrinterTagVisitor(String param0, int param1, List<String> param2) {
      this.indentation = param0;
      this.depth = param1;
      this.path = param2;
   }

   public String visit(Tag param0) {
      param0.accept(this);
      return this.result;
   }

   @Override
   public void visitString(StringTag param0) {
      this.result = StringTag.quoteAndEscape(param0.getAsString());
   }

   @Override
   public void visitByte(ByteTag param0) {
      this.result = param0.getAsNumber() + "b";
   }

   @Override
   public void visitShort(ShortTag param0) {
      this.result = param0.getAsNumber() + "s";
   }

   @Override
   public void visitInt(IntTag param0) {
      this.result = String.valueOf(param0.getAsNumber());
   }

   @Override
   public void visitLong(LongTag param0) {
      this.result = param0.getAsNumber() + "L";
   }

   @Override
   public void visitFloat(FloatTag param0) {
      this.result = param0.getAsFloat() + "f";
   }

   @Override
   public void visitDouble(DoubleTag param0) {
      this.result = param0.getAsDouble() + "d";
   }

   @Override
   public void visitByteArray(ByteArrayTag param0) {
      StringBuilder var2 = new StringBuilder("[").append("B").append(";");
      byte[] var3 = param0.getAsByteArray();

      for (int var4 = 0; var4 < var3.length; var4++) {
         var2.append(" ").append(var3[var4]).append("B");
         if (var4 != var3.length - 1) {
            var2.append(ELEMENT_SEPARATOR);
         }
      }

      var2.append("]");
      this.result = var2.toString();
   }

   @Override
   public void visitIntArray(IntArrayTag param0) {
      StringBuilder var2 = new StringBuilder("[").append("I").append(";");
      int[] var3 = param0.getAsIntArray();

      for (int var4 = 0; var4 < var3.length; var4++) {
         var2.append(" ").append(var3[var4]);
         if (var4 != var3.length - 1) {
            var2.append(ELEMENT_SEPARATOR);
         }
      }

      var2.append("]");
      this.result = var2.toString();
   }

   @Override
   public void visitLongArray(LongArrayTag param0) {
      String var2 = "L";
      StringBuilder var3 = new StringBuilder("[").append("L").append(";");
      long[] var4 = param0.getAsLongArray();

      for (int var5 = 0; var5 < var4.length; var5++) {
         var3.append(" ").append(var4[var5]).append("L");
         if (var5 != var4.length - 1) {
            var3.append(ELEMENT_SEPARATOR);
         }
      }

      var3.append("]");
      this.result = var3.toString();
   }

   @Override
   public void visitList(ListTag param0) {
      if (param0.isEmpty()) {
         this.result = "[]";
      } else {
         StringBuilder var2 = new StringBuilder("[");
         this.pushPath("[]");
         String var3 = NO_INDENTATION.contains(this.pathString()) ? "" : this.indentation;
         if (!var3.isEmpty()) {
            var2.append("\n");
         }

         for (int var4 = 0; var4 < param0.size(); var4++) {
            var2.append(Strings.repeat(var3, this.depth + 1));
            var2.append(new SnbtPrinterTagVisitor(var3, this.depth + 1, this.path).visit(param0.get(var4)));
            if (var4 != param0.size() - 1) {
               var2.append(ELEMENT_SEPARATOR).append(var3.isEmpty() ? " " : "\n");
            }
         }

         if (!var3.isEmpty()) {
            var2.append("\n").append(Strings.repeat(var3, this.depth));
         }

         var2.append("]");
         this.result = var2.toString();
         this.popPath();
      }
   }

   @Override
   public void visitCompound(CompoundTag param0) {
      if (param0.isEmpty()) {
         this.result = "{}";
      } else {
         StringBuilder var2 = new StringBuilder("{");
         this.pushPath("{}");
         String var3 = NO_INDENTATION.contains(this.pathString()) ? "" : this.indentation;
         if (!var3.isEmpty()) {
            var2.append("\n");
         }

         Collection<String> var4 = this.getKeys(param0);
         Iterator<String> var5 = var4.iterator();

         while (var5.hasNext()) {
            String var6 = var5.next();
            Tag var7 = param0.get(var6);
            this.pushPath(var6);
            var2.append(Strings.repeat(var3, this.depth + 1))
               .append(handleEscapePretty(var6))
               .append(NAME_VALUE_SEPARATOR)
               .append(" ")
               .append(new SnbtPrinterTagVisitor(var3, this.depth + 1, this.path).visit(var7));
            this.popPath();
            if (var5.hasNext()) {
               var2.append(ELEMENT_SEPARATOR).append(var3.isEmpty() ? " " : "\n");
            }
         }

         if (!var3.isEmpty()) {
            var2.append("\n").append(Strings.repeat(var3, this.depth));
         }

         var2.append("}");
         this.result = var2.toString();
         this.popPath();
      }
   }

   private void popPath() {
      this.path.remove(this.path.size() - 1);
   }

   private void pushPath(String param0) {
      this.path.add(param0);
   }

   protected List<String> getKeys(CompoundTag param0) {
      Set<String> var2 = Sets.newHashSet(param0.getAllKeys());
      List<String> var3 = Lists.newArrayList();
      List<String> var4 = KEY_ORDER.get(this.pathString());
      if (var4 != null) {
         for (String var6 : var4) {
            if (var2.remove(var6)) {
               var3.add(var6);
            }
         }

         if (!var2.isEmpty()) {
            var2.stream().sorted().forEach(var3::add);
         }
      } else {
         var3.addAll(var2);
         Collections.sort(var3);
      }

      return var3;
   }

   public String pathString() {
      return String.join(".", this.path);
   }

   protected static String handleEscapePretty(String param0) {
      return SIMPLE_VALUE.matcher(param0).matches() ? param0 : StringTag.quoteAndEscape(param0);
   }

   @Override
   public void visitEnd(EndTag param0) {
   }
}

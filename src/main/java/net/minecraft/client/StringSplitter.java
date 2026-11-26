package net.minecraft.client;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

public class StringSplitter {
   final StringSplitter.WidthProvider widthProvider;

   public StringSplitter(StringSplitter.WidthProvider param0) {
      this.widthProvider = param0;
   }

   public float stringWidth(@Nullable String param0) {
      if (param0 == null) {
         return 0.0F;
      }

      MutableFloat var2 = new MutableFloat();
      StringDecomposer.iterateFormatted(param0, Style.EMPTY, (l_param1, l_param2, l_param3) -> {
         var2.add(this.widthProvider.getWidth(l_param3, l_param2));
         return true;
      });
      return var2.floatValue();
   }

   public float stringWidth(FormattedText param0) {
      MutableFloat var2 = new MutableFloat();
      StringDecomposer.iterateFormatted(param0, Style.EMPTY, (l_param1, l_param2, l_param3) -> {
         var2.add(this.widthProvider.getWidth(l_param3, l_param2));
         return true;
      });
      return var2.floatValue();
   }

   public float stringWidth(FormattedCharSequence param0) {
      MutableFloat var2 = new MutableFloat();
      param0.accept((l_param1, l_param2, l_param3) -> {
         var2.add(this.widthProvider.getWidth(l_param3, l_param2));
         return true;
      });
      return var2.floatValue();
   }

   public int plainIndexAtWidth(String param0, int param1, Style param2) {
      StringSplitter.WidthLimitedCharSink var4 = new StringSplitter.WidthLimitedCharSink(param1);
      StringDecomposer.iterate(param0, param2, var4);
      return var4.getPosition();
   }

   public String plainHeadByWidth(String param0, int param1, Style param2) {
      return param0.substring(0, this.plainIndexAtWidth(param0, param1, param2));
   }

   public String plainTailByWidth(String param0, int param1, Style param2) {
      MutableFloat var4 = new MutableFloat();
      MutableInt var5 = new MutableInt(param0.length());
      StringDecomposer.iterateBackwards(param0, param2, (l_param3, l_param4, l_param5) -> {
         float l_var7 = var4.addAndGet(this.widthProvider.getWidth(l_param5, l_param4));
         if (l_var7 > param1) {
            return false;
         }

         var5.setValue(l_param3);
         return true;
      });
      return param0.substring(var5.intValue());
   }

   public int formattedIndexByWidth(String param0, int param1, Style param2) {
      StringSplitter.WidthLimitedCharSink var4 = new StringSplitter.WidthLimitedCharSink(param1);
      StringDecomposer.iterateFormatted(param0, param2, var4);
      return var4.getPosition();
   }

   @Nullable
   public Style componentStyleAtWidth(FormattedText param0, int param1) {
      StringSplitter.WidthLimitedCharSink var3 = new StringSplitter.WidthLimitedCharSink(param1);
      return param0.<Style>visit(
            (l_param1, l_param2) -> StringDecomposer.iterateFormatted(l_param2, l_param1, var3) ? Optional.empty() : Optional.of(l_param1), Style.EMPTY
         )
         .orElse(null);
   }

   @Nullable
   public Style componentStyleAtWidth(FormattedCharSequence param0, int param1) {
      StringSplitter.WidthLimitedCharSink var3 = new StringSplitter.WidthLimitedCharSink(param1);
      MutableObject<Style> var4 = new MutableObject<>();
      param0.accept((l_param2, l_param3, l_param4) -> {
         if (!var3.accept(l_param2, l_param3, l_param4)) {
            var4.setValue(l_param3);
            return false;
         } else {
            return true;
         }
      });
      return var4.getValue();
   }

   public String formattedHeadByWidth(String param0, int param1, Style param2) {
      return param0.substring(0, this.formattedIndexByWidth(param0, param1, param2));
   }

   public FormattedText headByWidth(FormattedText param0, int param1, Style param2) {
      final StringSplitter.WidthLimitedCharSink var4 = new StringSplitter.WidthLimitedCharSink(param1);
      return param0.visit(new FormattedText.StyledContentConsumer<FormattedText>() {
         private final ComponentCollector collector = new ComponentCollector();

         @Override
         public Optional<FormattedText> accept(Style l_param0, String l_param1) {
            var4.resetPosition();
            if (!StringDecomposer.iterateFormatted(l_param1, l_param0, var4)) {
               String l_var3 = l_param1.substring(0, var4.getPosition());
               if (!l_var3.isEmpty()) {
                  this.collector.append(FormattedText.of(l_var3, l_param0));
               }

               return Optional.of(this.collector.getResultOrEmpty());
            } else {
               if (!l_param1.isEmpty()) {
                  this.collector.append(FormattedText.of(l_param1, l_param0));
               }

               return Optional.empty();
            }
         }
      }, param2).orElse(param0);
   }

   public int findLineBreak(String param0, int param1, Style param2) {
      StringSplitter.LineBreakFinder var4 = new StringSplitter.LineBreakFinder(param1);
      StringDecomposer.iterateFormatted(param0, param2, var4);
      return var4.getSplitPosition();
   }

   public static int getWordPosition(String param0, int param1, int param2, boolean param3) {
      int var4 = param2;
      boolean var5 = param1 < 0;
      int var6 = Math.abs(param1);

      for (int var7 = 0; var7 < var6; var7++) {
         if (var5) {
            while (param3 && var4 > 0 && (param0.charAt(var4 - 1) == ' ' || param0.charAt(var4 - 1) == '\n')) {
               var4--;
            }

            while (var4 > 0 && param0.charAt(var4 - 1) != ' ' && param0.charAt(var4 - 1) != '\n') {
               var4--;
            }
         } else {
            int var8 = param0.length();
            int var9 = param0.indexOf(32, var4);
            int var10 = param0.indexOf(10, var4);
            if (var9 == -1 && var10 == -1) {
               var4 = -1;
            } else if (var9 != -1 && var10 != -1) {
               var4 = Math.min(var9, var10);
            } else if (var9 != -1) {
               var4 = var9;
            } else {
               var4 = var10;
            }

            if (var4 == -1) {
               var4 = var8;
            } else {
               while (param3 && var4 < var8 && (param0.charAt(var4) == ' ' || param0.charAt(var4) == '\n')) {
                  var4++;
               }
            }
         }
      }

      return var4;
   }

   public void splitLines(String param0, int param1, Style param2, boolean param3, StringSplitter.LinePosConsumer param4) {
      int var6 = 0;
      int var7 = param0.length();
      Style var8 = param2;

      while (var6 < var7) {
         StringSplitter.LineBreakFinder var9 = new StringSplitter.LineBreakFinder(param1);
         boolean var10 = StringDecomposer.iterateFormatted(param0, var6, var8, param2, var9);
         if (var10) {
            param4.accept(var8, var6, var7);
            break;
         }

         int var11 = var9.getSplitPosition();
         char var12 = param0.charAt(var11);
         int var13 = var12 != '\n' && var12 != ' ' ? var11 : var11 + 1;
         param4.accept(var8, var6, param3 ? var13 : var11);
         var6 = var13;
         var8 = var9.getSplitStyle();
      }
   }

   public List<FormattedText> splitLines(String param0, int param1, Style param2) {
      List<FormattedText> var4 = Lists.newArrayList();
      this.splitLines(
         param0, param1, param2, false, (l_param2, l_param3, l_param4) -> var4.add(FormattedText.of(param0.substring(l_param3, l_param4), l_param2))
      );
      return var4;
   }

   public List<FormattedText> splitLines(FormattedText param0, int param1, Style param2) {
      List<FormattedText> var4 = Lists.newArrayList();
      this.splitLines(param0, param1, param2, (l_param1, l_param2) -> var4.add(l_param1));
      return var4;
   }

   public List<FormattedText> splitLines(FormattedText param0, int param1, Style param2, FormattedText param3) {
      List<FormattedText> var5 = Lists.newArrayList();
      this.splitLines(param0, param1, param2, (l_param2, l_param3) -> var5.add(l_param3 ? FormattedText.composite(param3, l_param2) : l_param2));
      return var5;
   }

   public void splitLines(FormattedText param0, int param1, Style param2, BiConsumer<FormattedText, Boolean> param3) {
      List<StringSplitter.LineComponent> var5 = Lists.newArrayList();
      param0.visit((l_param1, l_param2) -> {
         if (!l_param2.isEmpty()) {
            var5.add(new StringSplitter.LineComponent(l_param2, l_param1));
         }

         return Optional.empty();
      }, param2);
      StringSplitter.FlatComponents var6 = new StringSplitter.FlatComponents(var5);
      boolean var7 = true;
      boolean var8 = false;
      boolean var9 = false;

      while (var7) {
         var7 = false;
         StringSplitter.LineBreakFinder var10 = new StringSplitter.LineBreakFinder(param1);

         for (StringSplitter.LineComponent var12 : var6.parts) {
            boolean var13 = StringDecomposer.iterateFormatted(var12.contents, 0, var12.style, param2, var10);
            if (!var13) {
               int var14 = var10.getSplitPosition();
               Style var15 = var10.getSplitStyle();
               char var16 = var6.charAt(var14);
               boolean var17 = var16 == '\n';
               boolean var18 = var17 || var16 == ' ';
               var8 = var17;
               FormattedText var19 = var6.splitAt(var14, var18 ? 1 : 0, var15);
               param3.accept(var19, var9);
               var9 = !var17;
               var7 = true;
               break;
            }

            var10.addToOffset(var12.contents.length());
         }
      }

      FormattedText var10 = var6.getRemainder();
      if (var10 != null) {
         param3.accept(var10, var9);
      } else if (var8) {
         param3.accept(FormattedText.EMPTY, false);
      }
   }

   static class FlatComponents {
      final List<StringSplitter.LineComponent> parts;
      private String flatParts;

      public FlatComponents(List<StringSplitter.LineComponent> param0) {
         this.parts = param0;
         this.flatParts = param0.stream().map(l_param0 -> l_param0.contents).collect(Collectors.joining());
      }

      public char charAt(int param0) {
         return this.flatParts.charAt(param0);
      }

      public FormattedText splitAt(int param0, int param1, Style param2) {
         ComponentCollector var4 = new ComponentCollector();
         ListIterator<StringSplitter.LineComponent> var5 = this.parts.listIterator();
         int var6 = param0;
         boolean var7 = false;

         while (var5.hasNext()) {
            StringSplitter.LineComponent var8 = var5.next();
            String var9 = var8.contents;
            int var10 = var9.length();
            if (!var7) {
               if (var6 > var10) {
                  var4.append(var8);
                  var5.remove();
                  var6 -= var10;
               } else {
                  String var11 = var9.substring(0, var6);
                  if (!var11.isEmpty()) {
                     var4.append(FormattedText.of(var11, var8.style));
                  }

                  var6 += param1;
                  var7 = true;
               }
            }

            if (var7) {
               if (var6 <= var10) {
                  String var11 = var9.substring(var6);
                  if (var11.isEmpty()) {
                     var5.remove();
                  } else {
                     var5.set(new StringSplitter.LineComponent(var11, param2));
                  }
                  break;
               }

               var5.remove();
               var6 -= var10;
            }
         }

         this.flatParts = this.flatParts.substring(param0 + param1);
         return var4.getResultOrEmpty();
      }

      @Nullable
      public FormattedText getRemainder() {
         ComponentCollector var1 = new ComponentCollector();
         this.parts.forEach(var1::append);
         this.parts.clear();
         return var1.getResult();
      }
   }

   class LineBreakFinder implements FormattedCharSink {
      private final float maxWidth;
      private int lineBreak = -1;
      private Style lineBreakStyle = Style.EMPTY;
      private boolean hadNonZeroWidthChar;
      private float width;
      private int lastSpace = -1;
      private Style lastSpaceStyle = Style.EMPTY;
      private int nextChar;
      private int offset;

      public LineBreakFinder(float param1) {
         this.maxWidth = Math.max(param1, 1.0F);
      }

      @Override
      public boolean accept(int param0, Style param1, int param2) {
         int var4 = param0 + this.offset;
         switch (param2) {
            case 10:
               return this.finishIteration(var4, param1);
            case 32:
               this.lastSpace = var4;
               this.lastSpaceStyle = param1;
            default:
               float var5 = StringSplitter.this.widthProvider.getWidth(param2, param1);
               this.width += var5;
               if (!this.hadNonZeroWidthChar || !(this.width > this.maxWidth)) {
                  this.hadNonZeroWidthChar |= var5 != 0.0F;
                  this.nextChar = var4 + Character.charCount(param2);
                  return true;
               } else {
                  return this.lastSpace != -1 ? this.finishIteration(this.lastSpace, this.lastSpaceStyle) : this.finishIteration(var4, param1);
               }
         }
      }

      private boolean finishIteration(int param0, Style param1) {
         this.lineBreak = param0;
         this.lineBreakStyle = param1;
         return false;
      }

      private boolean lineBreakFound() {
         return this.lineBreak != -1;
      }

      public int getSplitPosition() {
         return this.lineBreakFound() ? this.lineBreak : this.nextChar;
      }

      public Style getSplitStyle() {
         return this.lineBreakStyle;
      }

      public void addToOffset(int param0) {
         this.offset += param0;
      }
   }

   static class LineComponent implements FormattedText {
      final String contents;
      final Style style;

      public LineComponent(String param0, Style param1) {
         this.contents = param0;
         this.style = param1;
      }

      @Override
      public <T> Optional<T> visit(FormattedText.ContentConsumer<T> param0) {
         return param0.accept(this.contents);
      }

      @Override
      public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> param0, Style param1) {
         return param0.accept(this.style.applyTo(param1), this.contents);
      }
   }

   @FunctionalInterface
   public interface LinePosConsumer {
      void accept(Style var1, int var2, int var3);
   }

   class WidthLimitedCharSink implements FormattedCharSink {
      private float maxWidth;
      private int position;

      public WidthLimitedCharSink(float param1) {
         this.maxWidth = param1;
      }

      @Override
      public boolean accept(int param0, Style param1, int param2) {
         this.maxWidth = this.maxWidth - StringSplitter.this.widthProvider.getWidth(param2, param1);
         if (this.maxWidth >= 0.0F) {
            this.position = param0 + Character.charCount(param2);
            return true;
         } else {
            return false;
         }
      }

      public int getPosition() {
         return this.position;
      }

      public void resetPosition() {
         this.position = 0;
      }
   }

   @FunctionalInterface
   public interface WidthProvider {
      float getWidth(int var1, Style var2);
   }
}

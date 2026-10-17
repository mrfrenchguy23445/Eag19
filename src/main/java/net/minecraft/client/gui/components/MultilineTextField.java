package net.minecraft.client.gui.components;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;

public class MultilineTextField {
   public static final int NO_CHARACTER_LIMIT = Integer.MAX_VALUE;
   private static final int LINE_SEEK_PIXEL_BIAS = 2;
   private final Font font;
   private final List<MultilineTextField.StringView> displayLines = Lists.newArrayList();
   private String value;
   private int cursor;
   private int selectCursor;
   private boolean selecting;
   private int characterLimit = Integer.MAX_VALUE;
   private final int width;
   private Consumer<String> valueListener = l_param0 -> {};
   private Runnable cursorListener = () -> {};

   public MultilineTextField(Font param0, int param1) {
      this.font = param0;
      this.width = param1;
      this.setValue("");
   }

   public int characterLimit() {
      return this.characterLimit;
   }

   public void setCharacterLimit(int param0) {
      if (param0 < 0) {
         throw new IllegalArgumentException("Character limit cannot be negative");
      }

      this.characterLimit = param0;
   }

   public boolean hasCharacterLimit() {
      return this.characterLimit != Integer.MAX_VALUE;
   }

   public void setValueListener(Consumer<String> param0) {
      this.valueListener = param0;
   }

   public void setCursorListener(Runnable param0) {
      this.cursorListener = param0;
   }

   public void setValue(String param0) {
      this.value = this.truncateFullText(param0);
      this.cursor = this.value.length();
      this.selectCursor = this.cursor;
      this.onValueChange();
   }

   public String value() {
      return this.value;
   }

   public void insertText(String param0) {
      if (!param0.isEmpty() || this.hasSelection()) {
         String var2 = this.truncateInsertionText(SharedConstants.filterText(param0, true));
         MultilineTextField.StringView var3 = this.getSelected();
         this.value = new StringBuilder(this.value).replace(var3.beginIndex, var3.endIndex, var2).toString();
         this.cursor = var3.beginIndex + var2.length();
         this.selectCursor = this.cursor;
         this.onValueChange();
      }
   }

   public void deleteText(int param0) {
      if (!this.hasSelection()) {
         this.selectCursor = Mth.clamp(this.cursor + param0, 0, this.value.length());
      }

      this.insertText("");
   }

   public int cursor() {
      return this.cursor;
   }

   public void setSelecting(boolean param0) {
      this.selecting = param0;
   }

   public MultilineTextField.StringView getSelected() {
      return new MultilineTextField.StringView(Math.min(this.selectCursor, this.cursor), Math.max(this.selectCursor, this.cursor));
   }

   public int getLineCount() {
      return this.displayLines.size();
   }

   public int getLineAtCursor() {
      for (int var1 = 0; var1 < this.displayLines.size(); var1++) {
         MultilineTextField.StringView var2 = this.displayLines.get(var1);
         if (this.cursor >= var2.beginIndex && this.cursor <= var2.endIndex) {
            return var1;
         }
      }

      return -1;
   }

   public MultilineTextField.StringView getLineView(int param0) {
      return this.displayLines.get(Mth.clamp(param0, 0, this.displayLines.size() - 1));
   }

   public void seekCursor(Whence param0, int param1) {
      switch (param0) {
         case ABSOLUTE:
            this.cursor = param1;
            break;
         case RELATIVE:
            this.cursor += param1;
            break;
         case END:
            this.cursor = this.value.length() + param1;
      }

      this.cursor = Mth.clamp(this.cursor, 0, this.value.length());
      this.cursorListener.run();
      if (!this.selecting) {
         this.selectCursor = this.cursor;
      }
   }

   public void seekCursorLine(int param0) {
      if (param0 != 0) {
         int var2 = this.font.width(this.value.substring(this.getCursorLineView().beginIndex, this.cursor)) + 2;
         MultilineTextField.StringView var3 = this.getCursorLineView(param0);
         int var4 = this.font.plainSubstrByWidth(this.value.substring(var3.beginIndex, var3.endIndex), var2).length();
         this.seekCursor(Whence.ABSOLUTE, var3.beginIndex + var4);
      }
   }

   public void seekCursorToPoint(double param0, double param2) {
      int var5 = Mth.floor(param0);
      int var6 = Mth.floor(param2 / 9.0);
      MultilineTextField.StringView var7 = this.displayLines.get(Mth.clamp(var6, 0, this.displayLines.size() - 1));
      int var8 = this.font.plainSubstrByWidth(this.value.substring(var7.beginIndex, var7.endIndex), var5).length();
      this.seekCursor(Whence.ABSOLUTE, var7.beginIndex + var8);
   }

   public boolean keyPressed(int param0) {
      this.selecting = Screen.hasShiftDown();
      if (Screen.isSelectAll(param0)) {
         this.cursor = this.value.length();
         this.selectCursor = 0;
         return true;
      }

      if (Screen.isCopy(param0)) {
         Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
         return true;
      }

      if (Screen.isPaste(param0)) {
         this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
         return true;
      }

      if (Screen.isCut(param0)) {
         Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
         this.insertText("");
         return true;
      }

      switch (param0) {
         case 257:
         case 335:
            this.insertText("\n");
            return true;
         case 259:
            if (Screen.hasControlDown()) {
               MultilineTextField.StringView var2 = this.getPreviousWord();
               this.deleteText(var2.beginIndex - this.cursor);
            } else {
               this.deleteText(-1);
            }

            return true;
         case 261:
            if (Screen.hasControlDown()) {
               MultilineTextField.StringView var2 = this.getNextWord();
               this.deleteText(var2.beginIndex - this.cursor);
            } else {
               this.deleteText(1);
            }

            return true;
         case 262:
            if (Screen.hasControlDown()) {
               MultilineTextField.StringView var2 = this.getNextWord();
               this.seekCursor(Whence.ABSOLUTE, var2.beginIndex);
            } else {
               this.seekCursor(Whence.RELATIVE, 1);
            }

            return true;
         case 263:
            if (Screen.hasControlDown()) {
               MultilineTextField.StringView var2 = this.getPreviousWord();
               this.seekCursor(Whence.ABSOLUTE, var2.beginIndex);
            } else {
               this.seekCursor(Whence.RELATIVE, -1);
            }

            return true;
         case 264:
            if (!Screen.hasControlDown()) {
               this.seekCursorLine(1);
            }

            return true;
         case 265:
            if (!Screen.hasControlDown()) {
               this.seekCursorLine(-1);
            }

            return true;
         case 266:
            this.seekCursor(Whence.ABSOLUTE, 0);
            return true;
         case 267:
            this.seekCursor(Whence.END, 0);
            return true;
         case 268:
            if (Screen.hasControlDown()) {
               this.seekCursor(Whence.ABSOLUTE, 0);
            } else {
               this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().beginIndex);
            }

            return true;
         case 269:
            if (Screen.hasControlDown()) {
               this.seekCursor(Whence.END, 0);
            } else {
               this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().endIndex);
            }

            return true;
         default:
            return false;
      }
   }

   public Iterable<MultilineTextField.StringView> iterateLines() {
      return this.displayLines;
   }

   public boolean hasSelection() {
      return this.selectCursor != this.cursor;
   }

   @VisibleForTesting
   public String getSelectedText() {
      MultilineTextField.StringView var1 = this.getSelected();
      return this.value.substring(var1.beginIndex, var1.endIndex);
   }

   private MultilineTextField.StringView getCursorLineView() {
      return this.getCursorLineView(0);
   }

   private MultilineTextField.StringView getCursorLineView(int param0) {
      int var2 = this.getLineAtCursor();
      if (var2 < 0) {
         throw new IllegalStateException("Cursor is not within text (cursor = " + this.cursor + ", length = " + this.value.length() + ")");
      } else {
         return this.displayLines.get(Mth.clamp(var2 + param0, 0, this.displayLines.size() - 1));
      }
   }

   @VisibleForTesting
   public MultilineTextField.StringView getPreviousWord() {
      if (this.value.isEmpty()) {
         return MultilineTextField.StringView.EMPTY;
      }

      int var1 = Mth.clamp(this.cursor, 0, this.value.length() - 1);

      while (var1 > 0 && Character.isWhitespace(this.value.charAt(var1 - 1))) {
         var1--;
      }

      while (var1 > 0 && !Character.isWhitespace(this.value.charAt(var1 - 1))) {
         var1--;
      }

      return new MultilineTextField.StringView(var1, this.getWordEndPosition(var1));
   }

   @VisibleForTesting
   public MultilineTextField.StringView getNextWord() {
      if (this.value.isEmpty()) {
         return MultilineTextField.StringView.EMPTY;
      }

      int var1 = Mth.clamp(this.cursor, 0, this.value.length() - 1);

      while (var1 < this.value.length() && !Character.isWhitespace(this.value.charAt(var1))) {
         var1++;
      }

      while (var1 < this.value.length() && Character.isWhitespace(this.value.charAt(var1))) {
         var1++;
      }

      return new MultilineTextField.StringView(var1, this.getWordEndPosition(var1));
   }

   private int getWordEndPosition(int param0) {
      int var2 = param0;

      while (var2 < this.value.length() && !Character.isWhitespace(this.value.charAt(var2))) {
         var2++;
      }

      return var2;
   }

   private void onValueChange() {
      this.reflowDisplayLines();
      this.valueListener.accept(this.value);
      this.cursorListener.run();
   }

   private void reflowDisplayLines() {
      this.displayLines.clear();
      if (this.value.isEmpty()) {
         this.displayLines.add(MultilineTextField.StringView.EMPTY);
      } else {
         this.font
            .getSplitter()
            .splitLines(
               this.value,
               this.width,
               Style.EMPTY,
               false,
               (l_param0, l_param1, l_param2) -> this.displayLines.add(new MultilineTextField.StringView(l_param1, l_param2))
            );
         if (this.value.charAt(this.value.length() - 1) == '\n') {
            this.displayLines.add(new MultilineTextField.StringView(this.value.length(), this.value.length()));
         }
      }
   }

   private String truncateFullText(String param0) {
      return this.hasCharacterLimit() ? StringUtil.truncateStringIfNecessary(param0, this.characterLimit, false) : param0;
   }

   private String truncateInsertionText(String param0) {
      if (this.hasCharacterLimit()) {
         int var2 = this.characterLimit - this.value.length();
         return StringUtil.truncateStringIfNecessary(param0, var2, false);
      } else {
         return param0;
      }
   }

   protected record StringView(int beginIndex, int endIndex) {
      static final MultilineTextField.StringView EMPTY = new MultilineTextField.StringView(0, 0);
   }
}

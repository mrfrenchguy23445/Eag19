package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public class EditBox extends AbstractWidget implements Renderable {
   public static final int BACKWARDS = -1;
   public static final int FORWARDS = 1;
   private static final int CURSOR_INSERT_WIDTH = 1;
   private static final int CURSOR_INSERT_COLOR = -3092272;
   private static final String CURSOR_APPEND_CHARACTER = "_";
   public static final int DEFAULT_TEXT_COLOR = 14737632;
   private static final int BORDER_COLOR_FOCUSED = -1;
   private static final int BORDER_COLOR = -6250336;
   private static final int BACKGROUND_COLOR = -16777216;
   private final Font font;
   private String value = "";
   private int maxLength = 32;
   private int frame;
   private boolean bordered = true;
   private boolean canLoseFocus = true;
   private boolean isEditable = true;
   private boolean shiftPressed;
   private int displayPos;
   private int cursorPos;
   private int highlightPos;
   private int textColor = 14737632;
   private int textColorUneditable = 7368816;
   @Nullable
   private String suggestion;
   @Nullable
   private Consumer<String> responder;
   private Predicate<String> filter = Objects::nonNull;
   private BiFunction<String, Integer, FormattedCharSequence> formatter = (l_param0, l_param1) -> FormattedCharSequence.forward(l_param0, Style.EMPTY);
   @Nullable
   private Component hint;

   public EditBox(Font param0, int param1, int param2, int param3, int param4, Component param5) {
      this(param0, param1, param2, param3, param4, null, param5);
   }

   public EditBox(Font param0, int param1, int param2, int param3, int param4, @Nullable EditBox param5, Component param6) {
      super(param1, param2, param3, param4, param6);
      this.font = param0;
      if (param5 != null) {
         this.setValue(param5.getValue());
      }
   }

   public void setResponder(Consumer<String> param0) {
      this.responder = param0;
   }

   public void setFormatter(BiFunction<String, Integer, FormattedCharSequence> param0) {
      this.formatter = param0;
   }

   public void tick() {
      this.frame++;
   }

   @Override
   protected MutableComponent createNarrationMessage() {
      Component var1 = this.getMessage();
      return Component.translatable("gui.narrate.editBox", var1, this.value);
   }

   public void setValue(String param0) {
      if (this.filter.test(param0)) {
         if (param0.length() > this.maxLength) {
            this.value = param0.substring(0, this.maxLength);
         } else {
            this.value = param0;
         }

         this.moveCursorToEnd();
         this.setHighlightPos(this.cursorPos);
         this.onValueChange(param0);
      }
   }

   public String getValue() {
      return this.value;
   }

   public String getHighlighted() {
      int var1 = Math.min(this.cursorPos, this.highlightPos);
      int var2 = Math.max(this.cursorPos, this.highlightPos);
      return this.value.substring(var1, var2);
   }

   public void setFilter(Predicate<String> param0) {
      this.filter = param0;
   }

   public void insertText(String param0) {
      int var2 = Math.min(this.cursorPos, this.highlightPos);
      int var3 = Math.max(this.cursorPos, this.highlightPos);
      int var4 = this.maxLength - this.value.length() - (var2 - var3);
      String var5 = SharedConstants.filterText(param0);
      int var6 = var5.length();
      if (var4 < var6) {
         var5 = var5.substring(0, var4);
         var6 = var4;
      }

      String var7 = new StringBuilder(this.value).replace(var2, var3, var5).toString();
      if (this.filter.test(var7)) {
         this.value = var7;
         this.setCursorPosition(var2 + var6);
         this.setHighlightPos(this.cursorPos);
         this.onValueChange(this.value);
      }
   }

   private void onValueChange(String param0) {
      if (this.responder != null) {
         this.responder.accept(param0);
      }
   }

   private void deleteText(int param0) {
      if (Screen.hasControlDown()) {
         this.deleteWords(param0);
      } else {
         this.deleteChars(param0);
      }
   }

   public void deleteWords(int param0) {
      if (!this.value.isEmpty()) {
         if (this.highlightPos != this.cursorPos) {
            this.insertText("");
         } else {
            this.deleteChars(this.getWordPosition(param0) - this.cursorPos);
         }
      }
   }

   public void deleteChars(int param0) {
      if (!this.value.isEmpty()) {
         if (this.highlightPos != this.cursorPos) {
            this.insertText("");
         } else {
            int var2 = this.getCursorPos(param0);
            int var3 = Math.min(var2, this.cursorPos);
            int var4 = Math.max(var2, this.cursorPos);
            if (var3 != var4) {
               String var5 = new StringBuilder(this.value).delete(var3, var4).toString();
               if (this.filter.test(var5)) {
                  this.value = var5;
                  this.moveCursorTo(var3);
               }
            }
         }
      }
   }

   public int getWordPosition(int param0) {
      return this.getWordPosition(param0, this.getCursorPosition());
   }

   private int getWordPosition(int param0, int param1) {
      return this.getWordPosition(param0, param1, true);
   }

   private int getWordPosition(int param0, int param1, boolean param2) {
      int var4 = param1;
      boolean var5 = param0 < 0;
      int var6 = Math.abs(param0);

      for (int var7 = 0; var7 < var6; var7++) {
         if (!var5) {
            int var8 = this.value.length();
            var4 = this.value.indexOf(32, var4);
            if (var4 == -1) {
               var4 = var8;
            } else {
               while (param2 && var4 < var8 && this.value.charAt(var4) == ' ') {
                  var4++;
               }
            }
         } else {
            while (param2 && var4 > 0 && this.value.charAt(var4 - 1) == ' ') {
               var4--;
            }

            while (var4 > 0 && this.value.charAt(var4 - 1) != ' ') {
               var4--;
            }
         }
      }

      return var4;
   }

   public void moveCursor(int param0) {
      this.moveCursorTo(this.getCursorPos(param0));
   }

   private int getCursorPos(int param0) {
      return Util.offsetByCodepoints(this.value, this.cursorPos, param0);
   }

   public void moveCursorTo(int param0) {
      this.setCursorPosition(param0);
      if (!this.shiftPressed) {
         this.setHighlightPos(this.cursorPos);
      }

      this.onValueChange(this.value);
   }

   public void setCursorPosition(int param0) {
      this.cursorPos = Mth.clamp(param0, 0, this.value.length());
   }

   public void moveCursorToStart() {
      this.moveCursorTo(0);
   }

   public void moveCursorToEnd() {
      this.moveCursorTo(this.value.length());
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (!this.canConsumeInput()) {
         return false;
      }

      this.shiftPressed = Screen.hasShiftDown();
      if (Screen.isSelectAll(param0)) {
         this.moveCursorToEnd();
         this.setHighlightPos(0);
         return true;
      }

      if (Screen.isCopy(param0)) {
         Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
         return true;
      }

      if (Screen.isPaste(param0)) {
         if (this.isEditable) {
            this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
         }

         return true;
      } else if (Screen.isCut(param0)) {
         Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
         if (this.isEditable) {
            this.insertText("");
         }

         return true;
      } else {
         switch (param0) {
            case 259:
               if (this.isEditable) {
                  this.shiftPressed = false;
                  this.deleteText(-1);
                  this.shiftPressed = Screen.hasShiftDown();
               }

               return true;
            case 260:
            case 264:
            case 265:
            case 266:
            case 267:
            default:
               return false;
            case 261:
               if (this.isEditable) {
                  this.shiftPressed = false;
                  this.deleteText(1);
                  this.shiftPressed = Screen.hasShiftDown();
               }

               return true;
            case 262:
               if (Screen.hasControlDown()) {
                  this.moveCursorTo(this.getWordPosition(1));
               } else {
                  this.moveCursor(1);
               }

               return true;
            case 263:
               if (Screen.hasControlDown()) {
                  this.moveCursorTo(this.getWordPosition(-1));
               } else {
                  this.moveCursor(-1);
               }

               return true;
            case 268:
               this.moveCursorToStart();
               return true;
            case 269:
               this.moveCursorToEnd();
               return true;
         }
      }
   }

   public boolean canConsumeInput() {
      return this.isVisible() && this.isFocused() && this.isEditable();
   }

   @Override
   public boolean charTyped(char param0, int param1) {
      if (!this.canConsumeInput()) {
         return false;
      }

      if (SharedConstants.isAllowedChatCharacter(param0)) {
         if (this.isEditable) {
            this.insertText(Character.toString(param0));
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (this.isVisible() && param4 == 0) {
         boolean var6 = param0 >= this.getX() && param0 < this.getX() + this.width && param2 >= this.getY() && param2 < this.getY() + this.height;
         if (this.canLoseFocus) {
            this.setFocused(var6);
         }

         if (this.isFocused() && var6 && param4 == 0) {
            int var7 = Mth.floor(param0) - this.getX();
            if (this.bordered) {
               var7 -= 4;
            }

            String var8 = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
            this.moveCursorTo(this.font.plainSubstrByWidth(var8, var7).length() + this.displayPos);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
      if (this.isVisible()) {
         if (this.isBordered()) {
            int var5 = this.isFocused() ? -1 : -6250336;
            fill(param0, this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, var5);
            fill(param0, this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -16777216);
         }

         int var5 = this.isEditable ? this.textColor : this.textColorUneditable;
         int var6 = this.cursorPos - this.displayPos;
         int var7 = this.highlightPos - this.displayPos;
         String var8 = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
         boolean var9 = var6 >= 0 && var6 <= var8.length();
         boolean var10 = this.isFocused() && this.frame / 6 % 2 == 0 && var9;
         int var11 = this.bordered ? this.getX() + 4 : this.getX();
         int var12 = this.bordered ? this.getY() + (this.height - 8) / 2 : this.getY();
         int var13 = var11;
         if (var7 > var8.length()) {
            var7 = var8.length();
         }

         if (!var8.isEmpty()) {
            String var14 = var9 ? var8.substring(0, var6) : var8;
            var13 = this.font.drawShadow(param0, this.formatter.apply(var14, this.displayPos), var13, var12, var5);
         }

         boolean var14 = this.cursorPos < this.value.length() || this.value.length() >= this.getMaxLength();
         int var15 = var13;
         if (!var9) {
            var15 = var6 > 0 ? var11 + this.width : var11;
         } else if (var14) {
            var15--;
            var13--;
         }

         if (!var8.isEmpty() && var9 && var6 < var8.length()) {
            this.font.drawShadow(param0, this.formatter.apply(var8.substring(var6), this.cursorPos), var13, var12, var5);
         }

         if (this.hint != null && var8.isEmpty() && !this.isFocused()) {
            this.font.drawShadow(param0, this.hint, var13, var12, var5);
         }

         if (!var14 && this.suggestion != null) {
            this.font.drawShadow(param0, this.suggestion, var15 - 1, var12, -8355712);
         }

         if (var10) {
            if (var14) {
               GuiComponent.fill(param0, var15, var12 - 1, var15 + 1, var12 + 1 + 9, -3092272);
            } else {
               this.font.drawShadow(param0, "_", var15, var12, var5);
            }
         }

         if (var7 != var6) {
            int var16 = var11 + this.font.width(var8.substring(0, var7));
            this.renderHighlight(param0, var15, var12 - 1, var16 - 1, var12 + 1 + 9);
         }
      }
   }

   private void renderHighlight(PoseStack param0, int param1, int param2, int param3, int param4) {
      if (param1 < param3) {
         int var6 = param1;
         param1 = param3;
         param3 = var6;
      }

      if (param2 < param4) {
         int var6 = param2;
         param2 = param4;
         param4 = var6;
      }

      if (param3 > this.getX() + this.width) {
         param3 = this.getX() + this.width;
      }

      if (param1 > this.getX() + this.width) {
         param1 = this.getX() + this.width;
      }

      // PORT NOTE (Eag19 / TeaVM WASM-GC): vanilla inverts the framebuffer here with
      // GL_OR_REVERSE, which WebGL cannot express (see PlatformOpenGL._glLogicOp). A
      // translucent fill draws the selection without covering the glyphs underneath.
      fill(param0, param1, param2, param3, param4, 0x6600AAFF);
   }

   public void setMaxLength(int param0) {
      this.maxLength = param0;
      if (this.value.length() > param0) {
         this.value = this.value.substring(0, param0);
         this.onValueChange(this.value);
      }
   }

   private int getMaxLength() {
      return this.maxLength;
   }

   public int getCursorPosition() {
      return this.cursorPos;
   }

   private boolean isBordered() {
      return this.bordered;
   }

   public void setBordered(boolean param0) {
      this.bordered = param0;
   }

   public void setTextColor(int param0) {
      this.textColor = param0;
   }

   public void setTextColorUneditable(int param0) {
      this.textColorUneditable = param0;
   }

   @Nullable
   @Override
   public ComponentPath nextFocusPath(FocusNavigationEvent param0) {
      return this.visible && this.isEditable ? super.nextFocusPath(param0) : null;
   }

   @Override
   public boolean isMouseOver(double param0, double param2) {
      return this.visible && param0 >= this.getX() && param0 < this.getX() + this.width && param2 >= this.getY() && param2 < this.getY() + this.height;
   }

   @Override
   public void setFocused(boolean param0) {
      if (this.canLoseFocus || param0) {
         super.setFocused(param0);
         if (param0) {
            this.frame = 0;
         }
      }
   }

   private boolean isEditable() {
      return this.isEditable;
   }

   public void setEditable(boolean param0) {
      this.isEditable = param0;
   }

   public int getInnerWidth() {
      return this.isBordered() ? this.width - 8 : this.width;
   }

   public void setHighlightPos(int param0) {
      int var2 = this.value.length();
      this.highlightPos = Mth.clamp(param0, 0, var2);
      if (this.font != null) {
         if (this.displayPos > var2) {
            this.displayPos = var2;
         }

         int var3 = this.getInnerWidth();
         String var4 = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), var3);
         int var5 = var4.length() + this.displayPos;
         if (this.highlightPos == this.displayPos) {
            this.displayPos = this.displayPos - this.font.plainSubstrByWidth(this.value, var3, true).length();
         }

         if (this.highlightPos > var5) {
            this.displayPos = this.displayPos + (this.highlightPos - var5);
         } else if (this.highlightPos <= this.displayPos) {
            this.displayPos = this.displayPos - (this.displayPos - this.highlightPos);
         }

         this.displayPos = Mth.clamp(this.displayPos, 0, var2);
      }
   }

   public void setCanLoseFocus(boolean param0) {
      this.canLoseFocus = param0;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void setVisible(boolean param0) {
      this.visible = param0;
   }

   public void setSuggestion(@Nullable String param0) {
      this.suggestion = param0;
   }

   public int getScreenX(int param0) {
      return param0 > this.value.length() ? this.getX() : this.getX() + this.font.width(this.value.substring(0, param0));
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput param0) {
      param0.add(NarratedElementType.TITLE, this.createNarrationMessage());
   }

   public void setHint(Component param0) {
      this.hint = param0;
   }
}

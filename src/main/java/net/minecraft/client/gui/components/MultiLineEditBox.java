package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MultiLineEditBox extends AbstractScrollWidget {
   private static final int CURSOR_INSERT_WIDTH = 1;
   private static final int CURSOR_INSERT_COLOR = -3092272;
   private static final String CURSOR_APPEND_CHARACTER = "_";
   private static final int TEXT_COLOR = -2039584;
   private static final int PLACEHOLDER_TEXT_COLOR = -857677600;
   private final Font font;
   private final Component placeholder;
   private final MultilineTextField textField;
   private int frame;

   public MultiLineEditBox(Font param0, int param1, int param2, int param3, int param4, Component param5, Component param6) {
      super(param1, param2, param3, param4, param6);
      this.font = param0;
      this.placeholder = param5;
      this.textField = new MultilineTextField(param0, param3 - this.totalInnerPadding());
      this.textField.setCursorListener(this::scrollToCursor);
   }

   public void setCharacterLimit(int param0) {
      this.textField.setCharacterLimit(param0);
   }

   public void setValueListener(Consumer<String> param0) {
      this.textField.setValueListener(param0);
   }

   public void setValue(String param0) {
      this.textField.setValue(param0);
   }

   public String getValue() {
      return this.textField.value();
   }

   public void tick() {
      this.frame++;
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput param0) {
      param0.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox", this.getMessage(), this.getValue()));
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (super.mouseClicked(param0, param2, param4)) {
         return true;
      } else if (this.withinContentAreaPoint(param0, param2) && param4 == 0) {
         this.textField.setSelecting(Screen.hasShiftDown());
         this.seekCursorScreen(param0, param2);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseDragged(double param0, double param2, int param4, double param5, double param7) {
      if (super.mouseDragged(param0, param2, param4, param5, param7)) {
         return true;
      } else if (this.withinContentAreaPoint(param0, param2) && param4 == 0) {
         this.textField.setSelecting(true);
         this.seekCursorScreen(param0, param2);
         this.textField.setSelecting(Screen.hasShiftDown());
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      return this.textField.keyPressed(param0);
   }

   @Override
   public boolean charTyped(char param0, int param1) {
      if (this.visible && this.isFocused() && SharedConstants.isAllowedChatCharacter(param0)) {
         this.textField.insertText(Character.toString(param0));
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void renderContents(PoseStack param0, int param1, int param2, float param3) {
      String var5 = this.textField.value();
      if (var5.isEmpty() && !this.isFocused()) {
         this.font
            .drawWordWrap(
               param0,
               this.placeholder,
               this.getX() + this.innerPadding(),
               this.getY() + this.innerPadding(),
               this.width - this.totalInnerPadding(),
               -857677600
            );
      } else {
         int var6 = this.textField.cursor();
         boolean var7 = this.isFocused() && this.frame / 6 % 2 == 0;
         boolean var8 = var6 < var5.length();
         int var9 = 0;
         int var10 = 0;
         int var11 = this.getY() + this.innerPadding();

         for (MultilineTextField.StringView var13 : this.textField.iterateLines()) {
            boolean var14 = this.withinContentAreaTopBottom(var11, var11 + 9);
            if (var7 && var8 && var6 >= var13.beginIndex() && var6 <= var13.endIndex()) {
               if (var14) {
                  var9 = this.font.drawShadow(param0, var5.substring(var13.beginIndex(), var6), this.getX() + this.innerPadding(), var11, -2039584) - 1;
                  GuiComponent.fill(param0, var9, var11 - 1, var9 + 1, var11 + 1 + 9, -3092272);
                  this.font.drawShadow(param0, var5.substring(var6, var13.endIndex()), var9, var11, -2039584);
               }
            } else {
               if (var14) {
                  var9 = this.font.drawShadow(param0, var5.substring(var13.beginIndex(), var13.endIndex()), this.getX() + this.innerPadding(), var11, -2039584)
                     - 1;
               }

               var10 = var11;
            }

            var11 += 9;
         }

         if (var7 && !var8 && this.withinContentAreaTopBottom(var10, var10 + 9)) {
            this.font.drawShadow(param0, "_", var9, var10, -3092272);
         }

         if (this.textField.hasSelection()) {
            MultilineTextField.StringView var12 = this.textField.getSelected();
            int var13 = this.getX() + this.innerPadding();
            var11 = this.getY() + this.innerPadding();

            for (MultilineTextField.StringView var15 : this.textField.iterateLines()) {
               if (var12.beginIndex() > var15.endIndex()) {
                  var11 += 9;
               } else {
                  if (var15.beginIndex() > var12.endIndex()) {
                     break;
                  }

                  if (this.withinContentAreaTopBottom(var11, var11 + 9)) {
                     int var16 = this.font.width(var5.substring(var15.beginIndex(), Math.max(var12.beginIndex(), var15.beginIndex())));
                     int var17;
                     if (var12.endIndex() > var15.endIndex()) {
                        var17 = this.width - this.innerPadding();
                     } else {
                        var17 = this.font.width(var5.substring(var15.beginIndex(), var12.endIndex()));
                     }

                     this.renderHighlight(param0, var13 + var16, var11, var13 + var17, var11 + 9);
                  }

                  var11 += 9;
               }
            }
         }
      }
   }

   @Override
   protected void renderDecorations(PoseStack param0) {
      super.renderDecorations(param0);
      if (this.textField.hasCharacterLimit()) {
         int var2 = this.textField.characterLimit();
         Component var3 = Component.translatable("gui.multiLineEditBox.character_limit", this.textField.value().length(), var2);
         drawString(param0, this.font, var3, this.getX() + this.width - this.font.width(var3), this.getY() + this.height + 4, 10526880);
      }
   }

   @Override
   public int getInnerHeight() {
      return 9 * this.textField.getLineCount();
   }

   @Override
   protected boolean scrollbarVisible() {
      return this.textField.getLineCount() > this.getDisplayableLineCount();
   }

   @Override
   protected double scrollRate() {
      return 9.0 / 2.0;
   }

   private void renderHighlight(PoseStack param0, int param1, int param2, int param3, int param4) {
      RenderSystem.enableColorLogicOp();
      RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
      fill(param0, param1, param2, param3, param4, -16776961);
      RenderSystem.disableColorLogicOp();
   }

   private void scrollToCursor() {
      double var1 = this.scrollAmount();
      MultilineTextField.StringView var3 = this.textField.getLineView((int)(var1 / 9.0));
      if (this.textField.cursor() <= var3.beginIndex()) {
         var1 = this.textField.getLineAtCursor() * 9;
      } else {
         MultilineTextField.StringView var4 = this.textField.getLineView((int)((var1 + this.height) / 9.0) - 1);
         if (this.textField.cursor() > var4.endIndex()) {
            var1 = this.textField.getLineAtCursor() * 9 - this.height + 9 + this.totalInnerPadding();
         }
      }

      this.setScrollAmount(var1);
   }

   private double getDisplayableLineCount() {
      return (this.height - this.totalInnerPadding()) / 9.0;
   }

   private void seekCursorScreen(double param0, double param2) {
      double var5 = param0 - this.getX() - this.innerPadding();
      double var7 = param2 - this.getY() - this.innerPadding() + this.scrollAmount();
      this.textField.seekCursorToPoint(var5, var7);
   }
}

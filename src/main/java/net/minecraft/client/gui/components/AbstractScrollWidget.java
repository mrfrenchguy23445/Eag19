package net.minecraft.client.gui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class AbstractScrollWidget extends AbstractWidget implements Renderable, GuiEventListener {
   private static final int BORDER_COLOR_FOCUSED = -1;
   private static final int BORDER_COLOR = -6250336;
   private static final int BACKGROUND_COLOR = -16777216;
   private static final int INNER_PADDING = 4;
   private double scrollAmount;
   private boolean scrolling;

   public AbstractScrollWidget(int param0, int param1, int param2, int param3, Component param4) {
      super(param0, param1, param2, param3, param4);
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (!this.visible) {
         return false;
      } else {
         boolean var6 = this.withinContentAreaPoint(param0, param2);
         boolean var7 = this.scrollbarVisible()
            && param0 >= this.getX() + this.width
            && param0 <= this.getX() + this.width + 8
            && param2 >= this.getY()
            && param2 < this.getY() + this.height;
         if (var7 && param4 == 0) {
            this.scrolling = true;
            return true;
         } else {
            return var6 || var7;
         }
      }
   }

   @Override
   public boolean mouseReleased(double param0, double param2, int param4) {
      if (param4 == 0) {
         this.scrolling = false;
      }

      return super.mouseReleased(param0, param2, param4);
   }

   @Override
   public boolean mouseDragged(double param0, double param2, int param4, double param5, double param7) {
      if (this.visible && this.isFocused() && this.scrolling) {
         if (param2 < this.getY()) {
            this.setScrollAmount(0.0);
         } else if (param2 > this.getY() + this.height) {
            this.setScrollAmount(this.getMaxScrollAmount());
         } else {
            int var10 = this.getScrollBarHeight();
            double var11 = Math.max(1, this.getMaxScrollAmount() / (this.height - var10));
            this.setScrollAmount(this.scrollAmount + param7 * var11);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseScrolled(double param0, double param2, double param4) {
      if (!this.visible) {
         return false;
      }

      this.setScrollAmount(this.scrollAmount - param4 * this.scrollRate());
      return true;
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      boolean var4 = param0 == 265;
      boolean var5 = param0 == 264;
      if (var4 || var5) {
         double var6 = this.scrollAmount;
         this.setScrollAmount(this.scrollAmount + (var4 ? -1 : 1) * this.scrollRate());
         if (var6 != this.scrollAmount) {
            return true;
         }
      }

      return super.keyPressed(param0, param1, param2);
   }

   @Override
   public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
      if (this.visible) {
         this.renderBackground(param0);
         enableScissor(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1);
         param0.pushPose();
         param0.translate(0.0, -this.scrollAmount, 0.0);
         this.renderContents(param0, param1, param2, param3);
         param0.popPose();
         disableScissor();
         this.renderDecorations(param0);
      }
   }

   private int getScrollBarHeight() {
      return Mth.clamp((int)((float)(this.height * this.height) / this.getContentHeight()), 32, this.height);
   }

   protected void renderDecorations(PoseStack param0) {
      if (this.scrollbarVisible()) {
         this.renderScrollBar(param0);
      }
   }

   protected int innerPadding() {
      return 4;
   }

   protected int totalInnerPadding() {
      return this.innerPadding() * 2;
   }

   protected double scrollAmount() {
      return this.scrollAmount;
   }

   protected void setScrollAmount(double param0) {
      this.scrollAmount = Mth.clamp(param0, 0.0, this.getMaxScrollAmount());
   }

   protected int getMaxScrollAmount() {
      return Math.max(0, this.getContentHeight() - (this.height - 4));
   }

   private int getContentHeight() {
      return this.getInnerHeight() + 4;
   }

   private void renderBackground(PoseStack param0) {
      int var2 = this.isFocused() ? -1 : -6250336;
      fill(param0, this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, var2);
      fill(param0, this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, -16777216);
   }

   private void renderScrollBar(PoseStack param0) {
      int var2 = this.getScrollBarHeight();
      int var3 = this.getX() + this.width;
      int var4 = this.getX() + this.width + 8;
      int var5 = Math.max(this.getY(), (int)this.scrollAmount * (this.height - var2) / this.getMaxScrollAmount() + this.getY());
      int var6 = var5 + var2;
      fill(param0, var3, var5, var4, var6, -8355712);
      fill(param0, var3, var5, var4 - 1, var6 - 1, -4144960);
   }

   protected boolean withinContentAreaTopBottom(int param0, int param1) {
      return param1 - this.scrollAmount >= this.getY() && param0 - this.scrollAmount <= this.getY() + this.height;
   }

   protected boolean withinContentAreaPoint(double param0, double param2) {
      return param0 >= this.getX() && param0 < this.getX() + this.width && param2 >= this.getY() && param2 < this.getY() + this.height;
   }

   protected abstract int getInnerHeight();

   protected abstract boolean scrollbarVisible();

   protected abstract double scrollRate();

   protected abstract void renderContents(PoseStack var1, int var2, int var3, float var4);
}

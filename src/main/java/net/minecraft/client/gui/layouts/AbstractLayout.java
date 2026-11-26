package net.minecraft.client.gui.layouts;

import net.minecraft.util.Mth;

public abstract class AbstractLayout implements Layout {
   private int x;
   private int y;
   protected int width;
   protected int height;

   public AbstractLayout(int param0, int param1, int param2, int param3) {
      this.x = param0;
      this.y = param1;
      this.width = param2;
      this.height = param3;
   }

   @Override
   public void setX(int param0) {
      this.visitChildren(l_param1 -> {
         int l_var3 = l_param1.getX() + (param0 - this.getX());
         l_param1.setX(l_var3);
      });
      this.x = param0;
   }

   @Override
   public void setY(int param0) {
      this.visitChildren(l_param1 -> {
         int l_var3 = l_param1.getY() + (param0 - this.getY());
         l_param1.setY(l_var3);
      });
      this.y = param0;
   }

   @Override
   public int getX() {
      return this.x;
   }

   @Override
   public int getY() {
      return this.y;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   protected abstract static class AbstractChildWrapper {
      public final LayoutElement child;
      public final LayoutSettings.LayoutSettingsImpl layoutSettings;

      protected AbstractChildWrapper(LayoutElement param0, LayoutSettings param1) {
         this.child = param0;
         this.layoutSettings = param1.getExposed();
      }

      public int getHeight() {
         return this.child.getHeight() + this.layoutSettings.paddingTop + this.layoutSettings.paddingBottom;
      }

      public int getWidth() {
         return this.child.getWidth() + this.layoutSettings.paddingLeft + this.layoutSettings.paddingRight;
      }

      public void setX(int param0, int param1) {
         float var3 = this.layoutSettings.paddingLeft;
         float var4 = param1 - this.child.getWidth() - this.layoutSettings.paddingRight;
         int var5 = (int)Mth.lerp(this.layoutSettings.xAlignment, var3, var4);
         this.child.setX(var5 + param0);
      }

      public void setY(int param0, int param1) {
         float var3 = this.layoutSettings.paddingTop;
         float var4 = param1 - this.child.getHeight() - this.layoutSettings.paddingBottom;
         int var5 = Math.round(Mth.lerp(this.layoutSettings.yAlignment, var3, var4));
         this.child.setY(var5 + param0);
      }
   }
}

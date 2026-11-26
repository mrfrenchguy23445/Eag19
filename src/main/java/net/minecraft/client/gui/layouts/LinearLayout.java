package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class LinearLayout extends AbstractLayout {
   private final LinearLayout.Orientation orientation;
   private final List<LinearLayout.ChildContainer> children = new ArrayList<>();
   private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults();

   public LinearLayout(int param0, int param1, LinearLayout.Orientation param2) {
      this(0, 0, param0, param1, param2);
   }

   public LinearLayout(int param0, int param1, int param2, int param3, LinearLayout.Orientation param4) {
      super(param0, param1, param2, param3);
      this.orientation = param4;
   }

   @Override
   public void arrangeElements() {
      super.arrangeElements();
      if (!this.children.isEmpty()) {
         int var1 = 0;
         int var2 = this.orientation.getSecondaryLength(this);

         for (LinearLayout.ChildContainer var4 : this.children) {
            var1 += this.orientation.getPrimaryLength(var4);
            var2 = Math.max(var2, this.orientation.getSecondaryLength(var4));
         }

         int var3 = this.orientation.getPrimaryLength(this) - var1;
         int var4 = this.orientation.getPrimaryPosition(this);
         Iterator<LinearLayout.ChildContainer> var5 = this.children.iterator();
         LinearLayout.ChildContainer var6 = var5.next();
         this.orientation.setPrimaryPosition(var6, var4);
         var4 += this.orientation.getPrimaryLength(var6);
         if (this.children.size() >= 2) {
            Divisor var7 = new Divisor(var3, this.children.size() - 1);

            while (var7.hasNext()) {
               var4 += var7.nextInt();
               LinearLayout.ChildContainer var8 = var5.next();
               this.orientation.setPrimaryPosition(var8, var4);
               var4 += this.orientation.getPrimaryLength(var8);
            }
         }

         int var7 = this.orientation.getSecondaryPosition(this);

         for (LinearLayout.ChildContainer var9 : this.children) {
            this.orientation.setSecondaryPosition(var9, var7, var2);
         }

         switch (this.orientation) {
            case HORIZONTAL:
               this.height = var2;
               break;
            case VERTICAL:
               this.width = var2;
         }
      }
   }

   @Override
   public void visitChildren(Consumer<LayoutElement> param0) {
      this.children.forEach(l_param1 -> param0.accept(l_param1.child));
   }

   public LayoutSettings newChildLayoutSettings() {
      return this.defaultChildLayoutSettings.copy();
   }

   public LayoutSettings defaultChildLayoutSetting() {
      return this.defaultChildLayoutSettings;
   }

   public <T extends LayoutElement> T addChild(T param0) {
      return this.addChild(param0, this.newChildLayoutSettings());
   }

   public <T extends LayoutElement> T addChild(T param0, LayoutSettings param1) {
      this.children.add(new LinearLayout.ChildContainer(param0, param1));
      return param0;
   }

   static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
      protected ChildContainer(LayoutElement param0, LayoutSettings param1) {
         super(param0, param1);
      }
   }

   public enum Orientation {
      HORIZONTAL,
      VERTICAL;

      int getPrimaryLength(LayoutElement param0) {
         return switch (this) {
            case HORIZONTAL -> param0.getWidth();
            case VERTICAL -> param0.getHeight();
         };
      }

      int getPrimaryLength(LinearLayout.ChildContainer param0) {
         return switch (this) {
            case HORIZONTAL -> param0.getWidth();
            case VERTICAL -> param0.getHeight();
         };
      }

      int getSecondaryLength(LayoutElement param0) {
         return switch (this) {
            case HORIZONTAL -> param0.getHeight();
            case VERTICAL -> param0.getWidth();
         };
      }

      int getSecondaryLength(LinearLayout.ChildContainer param0) {
         return switch (this) {
            case HORIZONTAL -> param0.getHeight();
            case VERTICAL -> param0.getWidth();
         };
      }

      void setPrimaryPosition(LinearLayout.ChildContainer param0, int param1) {
         switch (this) {
            case HORIZONTAL:
               param0.setX(param1, param0.getWidth());
               break;
            case VERTICAL:
               param0.setY(param1, param0.getHeight());
         }
      }

      void setSecondaryPosition(LinearLayout.ChildContainer param0, int param1, int param2) {
         switch (this) {
            case HORIZONTAL:
               param0.setY(param1, param2);
               break;
            case VERTICAL:
               param0.setX(param1, param2);
         }
      }

      int getPrimaryPosition(LayoutElement param0) {
         return switch (this) {
            case HORIZONTAL -> param0.getX();
            case VERTICAL -> param0.getY();
         };
      }

      int getSecondaryPosition(LayoutElement param0) {
         return switch (this) {
            case HORIZONTAL -> param0.getY();
            case VERTICAL -> param0.getX();
         };
      }
   }
}

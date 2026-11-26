package net.minecraft.client.gui.layouts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;

public class FrameLayout extends AbstractLayout {
   private final List<FrameLayout.ChildContainer> children = new ArrayList<>();
   private int minWidth;
   private int minHeight;
   private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults().align(0.5F, 0.5F);

   public FrameLayout() {
      this(0, 0, 0, 0);
   }

   public FrameLayout(int param0, int param1) {
      this(0, 0, param0, param1);
   }

   public FrameLayout(int param0, int param1, int param2, int param3) {
      super(param0, param1, param2, param3);
      this.setMinDimensions(param2, param3);
   }

   public FrameLayout setMinDimensions(int param0, int param1) {
      return this.setMinWidth(param0).setMinHeight(param1);
   }

   public FrameLayout setMinHeight(int param0) {
      this.minHeight = param0;
      return this;
   }

   public FrameLayout setMinWidth(int param0) {
      this.minWidth = param0;
      return this;
   }

   public LayoutSettings newChildLayoutSettings() {
      return this.defaultChildLayoutSettings.copy();
   }

   public LayoutSettings defaultChildLayoutSetting() {
      return this.defaultChildLayoutSettings;
   }

   @Override
   public void arrangeElements() {
      super.arrangeElements();
      int var1 = this.minWidth;
      int var2 = this.minHeight;

      for (FrameLayout.ChildContainer var4 : this.children) {
         var1 = Math.max(var1, var4.getWidth());
         var2 = Math.max(var2, var4.getHeight());
      }

      for (FrameLayout.ChildContainer var4 : this.children) {
         var4.setX(this.getX(), var1);
         var4.setY(this.getY(), var2);
      }

      this.width = var1;
      this.height = var2;
   }

   public <T extends LayoutElement> T addChild(T param0) {
      return this.addChild(param0, this.newChildLayoutSettings());
   }

   public <T extends LayoutElement> T addChild(T param0, LayoutSettings param1) {
      this.children.add(new FrameLayout.ChildContainer(param0, param1));
      return param0;
   }

   @Override
   public void visitChildren(Consumer<LayoutElement> param0) {
      this.children.forEach(l_param1 -> param0.accept(l_param1.child));
   }

   public static void centerInRectangle(LayoutElement param0, int param1, int param2, int param3, int param4) {
      alignInRectangle(param0, param1, param2, param3, param4, 0.5F, 0.5F);
   }

   public static void centerInRectangle(LayoutElement param0, ScreenRectangle param1) {
      centerInRectangle(param0, param1.position().x(), param1.position().y(), param1.width(), param1.height());
   }

   public static void alignInRectangle(LayoutElement param0, ScreenRectangle param1, float param2, float param3) {
      alignInRectangle(param0, param1.left(), param1.top(), param1.width(), param1.height(), param2, param3);
   }

   public static void alignInRectangle(LayoutElement param0, int param1, int param2, int param3, int param4, float param5, float param6) {
      alignInDimension(param1, param3, param0.getWidth(), param0::setX, param5);
      alignInDimension(param2, param4, param0.getHeight(), param0::setY, param6);
   }

   public static void alignInDimension(int param0, int param1, int param2, Consumer<Integer> param3, float param4) {
      int var5 = (int)Mth.lerp(param4, 0.0F, param1 - param2);
      param3.accept(param0 + var5);
   }

   static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
      protected ChildContainer(LayoutElement param0, LayoutSettings param1) {
         super(param0, param1);
      }
   }
}

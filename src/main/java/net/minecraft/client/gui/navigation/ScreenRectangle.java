package net.minecraft.client.gui.navigation;

import javax.annotation.Nullable;

public record ScreenRectangle(ScreenPosition position, int width, int height) {
   private static final ScreenRectangle EMPTY = new ScreenRectangle(0, 0, 0, 0);

   public ScreenRectangle(int param0, int param1, int param2, int param3) {
      this(new ScreenPosition(param0, param1), param2, param3);
   }

   public static ScreenRectangle empty() {
      return EMPTY;
   }

   public static ScreenRectangle of(ScreenAxis param0, int param1, int param2, int param3, int param4) {
      return switch (param0) {
         case HORIZONTAL -> new ScreenRectangle(param1, param2, param3, param4);
         case VERTICAL -> new ScreenRectangle(param2, param1, param4, param3);
      };
   }

   public ScreenRectangle step(ScreenDirection param0) {
      return new ScreenRectangle(this.position.step(param0), this.width, this.height);
   }

   public int getLength(ScreenAxis param0) {
      return switch (param0) {
         case HORIZONTAL -> this.width;
         case VERTICAL -> this.height;
      };
   }

   public int getBoundInDirection(ScreenDirection param0) {
      ScreenAxis var2 = param0.getAxis();
      return param0.isPositive() ? this.position.getCoordinate(var2) + this.getLength(var2) - 1 : this.position.getCoordinate(var2);
   }

   public ScreenRectangle getBorder(ScreenDirection param0) {
      int var2 = this.getBoundInDirection(param0);
      ScreenAxis var3 = param0.getAxis().orthogonal();
      int var4 = this.getBoundInDirection(var3.getNegative());
      int var5 = this.getLength(var3);
      return of(param0.getAxis(), var2, var4, 1, var5).step(param0);
   }

   public boolean overlaps(ScreenRectangle param0) {
      return this.overlapsInAxis(param0, ScreenAxis.HORIZONTAL) && this.overlapsInAxis(param0, ScreenAxis.VERTICAL);
   }

   public boolean overlapsInAxis(ScreenRectangle param0, ScreenAxis param1) {
      int var3 = this.getBoundInDirection(param1.getNegative());
      int var4 = param0.getBoundInDirection(param1.getNegative());
      int var5 = this.getBoundInDirection(param1.getPositive());
      int var6 = param0.getBoundInDirection(param1.getPositive());
      return Math.max(var3, var4) <= Math.min(var5, var6);
   }

   public int getCenterInAxis(ScreenAxis param0) {
      return (this.getBoundInDirection(param0.getPositive()) + this.getBoundInDirection(param0.getNegative())) / 2;
   }

   @Nullable
   public ScreenRectangle intersection(ScreenRectangle param0) {
      int var2 = Math.max(this.left(), param0.left());
      int var3 = Math.max(this.top(), param0.top());
      int var4 = Math.min(this.right(), param0.right());
      int var5 = Math.min(this.bottom(), param0.bottom());
      return var2 < var4 && var3 < var5 ? new ScreenRectangle(var2, var3, var4 - var2, var5 - var3) : null;
   }

   public int top() {
      return this.position.y();
   }

   public int bottom() {
      return this.position.y() + this.height;
   }

   public int left() {
      return this.position.x();
   }

   public int right() {
      return this.position.x() + this.width;
   }
}

package net.minecraft.client.gui.navigation;

import it.unimi.dsi.fastutil.ints.IntComparator;

public enum ScreenDirection {
   UP,
   DOWN,
   LEFT,
   RIGHT;

   private final IntComparator coordinateValueComparator = (l_param0, l_param1) -> l_param0 == l_param1 ? 0 : (this.isBefore(l_param0, l_param1) ? -1 : 1);

   public ScreenAxis getAxis() {
      return switch (this) {
         case UP, DOWN -> ScreenAxis.VERTICAL;
         case LEFT, RIGHT -> ScreenAxis.HORIZONTAL;
      };
   }

   public ScreenDirection getOpposite() {
      return switch (this) {
         case UP -> DOWN;
         case DOWN -> UP;
         case LEFT -> RIGHT;
         case RIGHT -> LEFT;
      };
   }

   public boolean isPositive() {
      return switch (this) {
         case UP, LEFT -> false;
         case DOWN, RIGHT -> true;
      };
   }

   public boolean isAfter(int param0, int param1) {
      return this.isPositive() ? param0 > param1 : param1 > param0;
   }

   public boolean isBefore(int param0, int param1) {
      return this.isPositive() ? param0 < param1 : param1 < param0;
   }

   public IntComparator coordinateValueComparator() {
      return this.coordinateValueComparator;
   }
}

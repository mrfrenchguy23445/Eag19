package net.minecraft.client.gui.components.events;

import javax.annotation.Nullable;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public interface GuiEventListener extends TabOrderedElement {
   long DOUBLE_CLICK_THRESHOLD_MS = 250L;

   default void mouseMoved(double param0, double param2) {
   }

   default boolean mouseClicked(double param0, double param2, int param4) {
      return false;
   }

   default boolean mouseReleased(double param0, double param2, int param4) {
      return false;
   }

   default boolean mouseDragged(double param0, double param2, int param4, double param5, double param7) {
      return false;
   }

   default boolean mouseScrolled(double param0, double param2, double param4) {
      return false;
   }

   default boolean keyPressed(int param0, int param1, int param2) {
      return false;
   }

   default boolean keyReleased(int param0, int param1, int param2) {
      return false;
   }

   default boolean charTyped(char param0, int param1) {
      return false;
   }

   @Nullable
   default ComponentPath nextFocusPath(FocusNavigationEvent param0) {
      return null;
   }

   default boolean isMouseOver(double param0, double param2) {
      return false;
   }

   void setFocused(boolean var1);

   boolean isFocused();

   @Nullable
   default ComponentPath getCurrentFocusPath() {
      return this.isFocused() ? ComponentPath.leaf(this) : null;
   }

   default ScreenRectangle getRectangle() {
      return ScreenRectangle.empty();
   }
}

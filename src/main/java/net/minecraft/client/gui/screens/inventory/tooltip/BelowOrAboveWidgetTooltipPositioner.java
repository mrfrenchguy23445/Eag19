package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Vector2i;
import org.joml.Vector2ic;

public class BelowOrAboveWidgetTooltipPositioner implements ClientTooltipPositioner {
   private final AbstractWidget widget;

   public BelowOrAboveWidgetTooltipPositioner(AbstractWidget param0) {
      this.widget = param0;
   }

   @Override
   public Vector2ic positionTooltip(Screen param0, int param1, int param2, int param3, int param4) {
      Vector2i var6 = new Vector2i();
      var6.x = this.widget.getX() + 3;
      var6.y = this.widget.getY() + this.widget.getHeight() + 3 + 1;
      if (var6.y + param4 + 3 > param0.height) {
         var6.y = this.widget.getY() - param4 - 3 - 1;
      }

      if (var6.x + param3 > param0.width) {
         var6.x = Math.max(this.widget.getX() + this.widget.getWidth() - param3 - 3, 4);
      }

      return var6;
   }
}

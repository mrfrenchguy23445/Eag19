package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.screens.Screen;
import org.joml.Vector2i;
import org.joml.Vector2ic;

public class DefaultTooltipPositioner implements ClientTooltipPositioner {
   public static final ClientTooltipPositioner INSTANCE = new DefaultTooltipPositioner();

   private DefaultTooltipPositioner() {
   }

   @Override
   public Vector2ic positionTooltip(Screen param0, int param1, int param2, int param3, int param4) {
      Vector2i var6 = new Vector2i(param1, param2).add(12, -12);
      this.positionTooltip(param0, var6, param3, param4);
      return var6;
   }

   private void positionTooltip(Screen param0, Vector2i param1, int param2, int param3) {
      if (param1.x + param2 > param0.width) {
         param1.x = Math.max(param1.x - 24 - param2, 4);
      }

      int var5 = param3 + 3;
      if (param1.y + var5 > param0.height) {
         param1.y = param0.height - var5;
      }
   }
}

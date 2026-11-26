package net.minecraft.client.gui.screens.advancements;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

enum AdvancementTabType {
   ABOVE(0, 0, 28, 32, 8),
   BELOW(84, 0, 28, 32, 8),
   LEFT(0, 64, 32, 28, 5),
   RIGHT(96, 64, 32, 28, 5);

   private final int textureX;
   private final int textureY;
   private final int width;
   private final int height;
   private final int max;

   AdvancementTabType(int param2, int param3, int param4, int param5, int param6) {
      this.textureX = param2;
      this.textureY = param3;
      this.width = param4;
      this.height = param5;
      this.max = param6;
   }

   public int getMax() {
      return this.max;
   }

   public void draw(PoseStack param0, int param1, int param2, boolean param3, int param4) {
      int var6 = this.textureX;
      if (param4 > 0) {
         var6 += this.width;
      }

      if (param4 == this.max - 1) {
         var6 += this.width;
      }

      int var7 = param3 ? this.textureY + this.height : this.textureY;
      GuiComponent.blit(param0, param1 + this.getX(param4), param2 + this.getY(param4), var6, var7, this.width, this.height);
   }

   public void drawIcon(PoseStack param0, int param1, int param2, int param3, ItemRenderer param4, ItemStack param5) {
      int var7 = param1 + this.getX(param3);
      int var8 = param2 + this.getY(param3);
      switch (this) {
         case ABOVE:
            var7 += 6;
            var8 += 9;
            break;
         case BELOW:
            var7 += 6;
            var8 += 6;
            break;
         case LEFT:
            var7 += 10;
            var8 += 5;
            break;
         case RIGHT:
            var7 += 6;
            var8 += 5;
      }

      param4.renderAndDecorateFakeItem(param0, param5, var7, var8);
   }

   public int getX(int param0) {
      switch (this) {
         case ABOVE:
            return (this.width + 4) * param0;
         case BELOW:
            return (this.width + 4) * param0;
         case LEFT:
            return -this.width + 4;
         case RIGHT:
            return 248;
         default:
            throw new UnsupportedOperationException("Don't know what this tab type is!" + this);
      }
   }

   public int getY(int param0) {
      switch (this) {
         case ABOVE:
            return -this.height + 4;
         case BELOW:
            return 136;
         case LEFT:
            return this.height * param0;
         case RIGHT:
            return this.height * param0;
         default:
            throw new UnsupportedOperationException("Don't know what this tab type is!" + this);
      }
   }

   public boolean isMouseOver(int param0, int param1, int param2, double param3, double param5) {
      int var8 = param0 + this.getX(param2);
      int var9 = param1 + this.getY(param2);
      return param3 > var8 && param3 < var8 + this.width && param5 > var9 && param5 < var9 + this.height;
   }
}

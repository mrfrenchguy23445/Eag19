package com.mojang.realmsclient.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.realms.RealmsObjectSelectionList;

public abstract class RowButton {
   public final int width;
   public final int height;
   public final int xOffset;
   public final int yOffset;

   public RowButton(int param0, int param1, int param2, int param3) {
      this.width = param0;
      this.height = param1;
      this.xOffset = param2;
      this.yOffset = param3;
   }

   public void drawForRowAt(PoseStack param0, int param1, int param2, int param3, int param4) {
      int var6 = param1 + this.xOffset;
      int var7 = param2 + this.yOffset;
      boolean var8 = param3 >= var6 && param3 <= var6 + this.width && param4 >= var7 && param4 <= var7 + this.height;
      this.draw(param0, var6, var7, var8);
   }

   protected abstract void draw(PoseStack var1, int var2, int var3, boolean var4);

   public int getRight() {
      return this.xOffset + this.width;
   }

   public int getBottom() {
      return this.yOffset + this.height;
   }

   public abstract void onClick(int var1);

   public static void drawButtonsInRow(
      PoseStack param0, List<RowButton> param1, RealmsObjectSelectionList<?> param2, int param3, int param4, int param5, int param6
   ) {
      for (RowButton var8 : param1) {
         if (param2.getRowWidth() > var8.getRight()) {
            var8.drawForRowAt(param0, param3, param4, param5, param6);
         }
      }
   }

   public static void rowButtonMouseClicked(
      RealmsObjectSelectionList<?> param0, ObjectSelectionList.Entry<?> param1, List<RowButton> param2, int param3, double param4, double param6
   ) {
      if (param3 == 0) {
         int var8 = param0.children().indexOf(param1);
         if (var8 > -1) {
            param0.selectItem(var8);
            int var9 = param0.getRowLeft();
            int var10 = param0.getRowTop(var8);
            int var11 = (int)(param4 - var9);
            int var12 = (int)(param6 - var10);

            for (RowButton var14 : param2) {
               if (var11 >= var14.xOffset && var11 <= var14.getRight() && var12 >= var14.yOffset && var12 <= var14.getBottom()) {
                  var14.onClick(var8);
               }
            }
         }
      }
   }
}

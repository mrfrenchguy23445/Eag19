package net.minecraft.recipebook;

import java.util.Iterator;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

public interface PlaceRecipe<T> {
   default void placeRecipe(int param0, int param1, int param2, Recipe<?> param3, Iterator<T> param4, int param5) {
      int var7 = param0;
      int var8 = param1;
      if (param3 instanceof ShapedRecipe var9) {
         var7 = var9.getWidth();
         var8 = var9.getHeight();
      }

      int var9 = 0;

      for (int var10 = 0; var10 < param1; var10++) {
         if (var9 == param2) {
            var9++;
         }

         boolean var11 = var8 < param1 / 2.0F;
         int var12 = Mth.floor(param1 / 2.0F - var8 / 2.0F);
         if (var11 && var12 > var10) {
            var9 += param0;
            var10++;
         }

         for (int var13 = 0; var13 < param0; var13++) {
            if (!param4.hasNext()) {
               return;
            }

            var11 = var7 < param0 / 2.0F;
            var12 = Mth.floor(param0 / 2.0F - var7 / 2.0F);
            int var14 = var7;
            boolean var15 = var13 < var7;
            if (var11) {
               var14 = var12 + var7;
               var15 = var12 <= var13 && var13 < var12 + var7;
            }

            if (var15) {
               this.addItemToSlot(param4, var9, param5, var10, var13);
            } else if (var14 == var13) {
               var9 += param0 - var13;
               break;
            }

            var9++;
         }
      }
   }

   void addItemToSlot(Iterator<T> var1, int var2, int var3, int var4, int var5);
}

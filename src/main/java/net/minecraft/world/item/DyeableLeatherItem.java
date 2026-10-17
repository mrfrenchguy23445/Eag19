package net.minecraft.world.item;

import java.util.List;
import net.minecraft.nbt.CompoundTag;

public interface DyeableLeatherItem {
   String TAG_COLOR = "color";
   String TAG_DISPLAY = "display";
   int DEFAULT_LEATHER_COLOR = 10511680;

   default boolean hasCustomColor(ItemStack param0) {
      CompoundTag var2 = param0.getTagElement("display");
      return var2 != null && var2.contains("color", 99);
   }

   default int getColor(ItemStack param0) {
      CompoundTag var2 = param0.getTagElement("display");
      return var2 != null && var2.contains("color", 99) ? var2.getInt("color") : 10511680;
   }

   default void clearColor(ItemStack param0) {
      CompoundTag var2 = param0.getTagElement("display");
      if (var2 != null && var2.contains("color")) {
         var2.remove("color");
      }
   }

   default void setColor(ItemStack param0, int param1) {
      param0.getOrCreateTagElement("display").putInt("color", param1);
   }

   static ItemStack dyeArmor(ItemStack param0, List<DyeItem> param1) {
      ItemStack var2 = ItemStack.EMPTY;
      int[] var3 = new int[3];
      int var4 = 0;
      int var5 = 0;
      DyeableLeatherItem var6 = null;
      Item var7 = param0.getItem();
      if (var7 instanceof DyeableLeatherItem) {
         var6 = (DyeableLeatherItem)var7;
         var2 = param0.copy();
         var2.setCount(1);
         if (var6.hasCustomColor(param0)) {
            int var8 = var6.getColor(var2);
            float var9 = (var8 >> 16 & 0xFF) / 255.0F;
            float var10 = (var8 >> 8 & 0xFF) / 255.0F;
            float var11 = (var8 & 0xFF) / 255.0F;
            var4 += (int)(Math.max(var9, Math.max(var10, var11)) * 255.0F);
            var3[0] += (int)(var9 * 255.0F);
            var3[1] += (int)(var10 * 255.0F);
            var3[2] += (int)(var11 * 255.0F);
            var5++;
         }

         for (DyeItem var9 : param1) {
            float[] var10 = var9.getDyeColor().getTextureDiffuseColors();
            int var11 = (int)(var10[0] * 255.0F);
            int var12 = (int)(var10[1] * 255.0F);
            int var13 = (int)(var10[2] * 255.0F);
            var4 += Math.max(var11, Math.max(var12, var13));
            var3[0] += var11;
            var3[1] += var12;
            var3[2] += var13;
            var5++;
         }
      }

      if (var6 == null) {
         return ItemStack.EMPTY;
      }

      int var8 = var3[0] / var5;
      int var9 = var3[1] / var5;
      int var10 = var3[2] / var5;
      float var11 = (float)var4 / var5;
      float var12 = Math.max(var8, Math.max(var9, var10));
      var8 = (int)(var8 * var11 / var12);
      var9 = (int)(var9 * var11 / var12);
      var10 = (int)(var10 * var11 / var12);
      int var13 = var8;
      var13 = (var13 << 8) + var9;
      var13 = (var13 << 8) + var10;
      var6.setColor(var2, var13);
      return var2;
   }
}

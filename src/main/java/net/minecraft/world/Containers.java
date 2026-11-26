package net.minecraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Containers {
   public static void dropContents(Level param0, BlockPos param1, Container param2) {
      dropContents(param0, param1.getX(), param1.getY(), param1.getZ(), param2);
   }

   public static void dropContents(Level param0, Entity param1, Container param2) {
      dropContents(param0, param1.getX(), param1.getY(), param1.getZ(), param2);
   }

   private static void dropContents(Level param0, double param1, double param3, double param5, Container param7) {
      for (int var8 = 0; var8 < param7.getContainerSize(); var8++) {
         dropItemStack(param0, param1, param3, param5, param7.getItem(var8));
      }
   }

   public static void dropContents(Level param0, BlockPos param1, NonNullList<ItemStack> param2) {
      param2.forEach(l_param2 -> dropItemStack(param0, param1.getX(), param1.getY(), param1.getZ(), l_param2));
   }

   public static void dropItemStack(Level param0, double param1, double param3, double param5, ItemStack param7) {
      double var8 = EntityType.ITEM.getWidth();
      double var10 = 1.0 - var8;
      double var12 = var8 / 2.0;
      double var14 = Math.floor(param1) + param0.random.nextDouble() * var10 + var12;
      double var16 = Math.floor(param3) + param0.random.nextDouble() * var10;
      double var18 = Math.floor(param5) + param0.random.nextDouble() * var10 + var12;

      while (!param7.isEmpty()) {
         ItemEntity var20 = new ItemEntity(param0, var14, var16, var18, param7.split(param0.random.nextInt(21) + 10));
         float var21 = 0.05F;
         var20.setDeltaMovement(
            param0.random.triangle(0.0, 0.11485000171139836),
            param0.random.triangle(0.2, 0.11485000171139836),
            param0.random.triangle(0.0, 0.11485000171139836)
         );
         param0.addFreshEntity(var20);
      }
   }
}

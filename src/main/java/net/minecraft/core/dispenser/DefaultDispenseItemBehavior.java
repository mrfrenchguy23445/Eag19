package net.minecraft.core.dispenser;

import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class DefaultDispenseItemBehavior implements DispenseItemBehavior {
   @Override
   public final ItemStack dispense(BlockSource param0, ItemStack param1) {
      ItemStack var3 = this.execute(param0, param1);
      this.playSound(param0);
      this.playAnimation(param0, param0.getBlockState().getValue(DispenserBlock.FACING));
      return var3;
   }

   protected ItemStack execute(BlockSource param0, ItemStack param1) {
      Direction var3 = param0.getBlockState().getValue(DispenserBlock.FACING);
      Position var4 = DispenserBlock.getDispensePosition(param0);
      ItemStack var5 = param1.split(1);
      spawnItem(param0.getLevel(), var5, 6, var3, var4);
      return param1;
   }

   public static void spawnItem(Level param0, ItemStack param1, int param2, Direction param3, Position param4) {
      double var5 = param4.x();
      double var7 = param4.y();
      double var9 = param4.z();
      if (param3.getAxis() == Direction.Axis.Y) {
         var7 -= 0.125;
      } else {
         var7 -= 0.15625;
      }

      ItemEntity var11 = new ItemEntity(param0, var5, var7, var9, param1);
      double var12 = param0.random.nextDouble() * 0.1 + 0.2;
      var11.setDeltaMovement(
         param0.random.triangle(param3.getStepX() * var12, 0.0172275 * param2),
         param0.random.triangle(0.2, 0.0172275 * param2),
         param0.random.triangle(param3.getStepZ() * var12, 0.0172275 * param2)
      );
      param0.addFreshEntity(var11);
   }

   protected void playSound(BlockSource param0) {
      param0.getLevel().levelEvent(1000, param0.getPos(), 0);
   }

   protected void playAnimation(BlockSource param0, Direction param1) {
      param0.getLevel().levelEvent(2000, param0.getPos(), param1.get3DDataValue());
   }
}

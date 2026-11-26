package net.minecraft.core.dispenser;

import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public abstract class AbstractProjectileDispenseBehavior extends DefaultDispenseItemBehavior {
   @Override
   public ItemStack execute(BlockSource param0, ItemStack param1) {
      Level var3 = param0.getLevel();
      Position var4 = DispenserBlock.getDispensePosition(param0);
      Direction var5 = param0.getBlockState().getValue(DispenserBlock.FACING);
      Projectile var6 = this.getProjectile(var3, var4, param1);
      var6.shoot(var5.getStepX(), var5.getStepY() + 0.1F, var5.getStepZ(), this.getPower(), this.getUncertainty());
      var3.addFreshEntity(var6);
      param1.shrink(1);
      return param1;
   }

   @Override
   protected void playSound(BlockSource param0) {
      param0.getLevel().levelEvent(1002, param0.getPos(), 0);
   }

   protected abstract Projectile getProjectile(Level var1, Position var2, ItemStack var3);

   protected float getUncertainty() {
      return 6.0F;
   }

   protected float getPower() {
      return 1.1F;
   }
}

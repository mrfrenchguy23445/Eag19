package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class BoatDispenseItemBehavior extends DefaultDispenseItemBehavior {
   private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();
   private final Boat.Type type;
   private final boolean isChestBoat;

   public BoatDispenseItemBehavior(Boat.Type param0) {
      this(param0, false);
   }

   public BoatDispenseItemBehavior(Boat.Type param0, boolean param1) {
      this.type = param0;
      this.isChestBoat = param1;
   }

   @Override
   public ItemStack execute(BlockSource param0, ItemStack param1) {
      Direction var3 = param0.getBlockState().getValue(DispenserBlock.FACING);
      Level var4 = param0.getLevel();
      double var5 = param0.x() + var3.getStepX() * 1.125F;
      double var7 = param0.y() + var3.getStepY() * 1.125F;
      double var9 = param0.z() + var3.getStepZ() * 1.125F;
      BlockPos var11 = param0.getPos().relative(var3);
      double var12;
      if (var4.getFluidState(var11).is(FluidTags.WATER)) {
         var12 = 1.0;
      } else {
         if (!var4.getBlockState(var11).isAir() || !var4.getFluidState(var11.below()).is(FluidTags.WATER)) {
            return this.defaultDispenseItemBehavior.dispense(param0, param1);
         }

         var12 = 0.0;
      }

      Boat var14 = this.isChestBoat ? new ChestBoat(var4, var5, var7 + var12, var9) : new Boat(var4, var5, var7 + var12, var9);
      var14.setVariant(this.type);
      var14.setYRot(var3.toYRot());
      var4.addFreshEntity(var14);
      param1.shrink(1);
      return param1;
   }

   @Override
   protected void playSound(BlockSource param0) {
      param0.getLevel().levelEvent(1000, param0.getPos(), 0);
   }
}

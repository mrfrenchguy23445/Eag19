package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSourceImpl;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class DropperBlock extends DispenserBlock {
   private static final DispenseItemBehavior DISPENSE_BEHAVIOUR = new DefaultDispenseItemBehavior();

   public DropperBlock(BlockBehaviour.Properties param0) {
      super(param0);
   }

   @Override
   protected DispenseItemBehavior getDispenseMethod(ItemStack param0) {
      return DISPENSE_BEHAVIOUR;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos param0, BlockState param1) {
      return new DropperBlockEntity(param0, param1);
   }

   @Override
   protected void dispenseFrom(ServerLevel param0, BlockPos param1) {
      BlockSourceImpl var3 = new BlockSourceImpl(param0, param1);
      DispenserBlockEntity var4 = var3.getEntity();
      int var5 = var4.getRandomSlot(param0.random);
      if (var5 < 0) {
         param0.levelEvent(1001, param1, 0);
      } else {
         ItemStack var6 = var4.getItem(var5);
         if (!var6.isEmpty()) {
            Direction var7 = param0.getBlockState(param1).getValue(FACING);
            Container var8 = HopperBlockEntity.getContainerAt(param0, param1.relative(var7));
            ItemStack var9;
            if (var8 == null) {
               var9 = DISPENSE_BEHAVIOUR.dispense(var3, var6);
            } else {
               var9 = HopperBlockEntity.addItem(var4, var8, var6.copy().split(1), var7.getOpposite());
               if (var9.isEmpty()) {
                  var9 = var6.copy();
                  var9.shrink(1);
               } else {
                  var9 = var6.copy();
               }
            }

            var4.setItem(var5, var9);
         }
      }
   }
}

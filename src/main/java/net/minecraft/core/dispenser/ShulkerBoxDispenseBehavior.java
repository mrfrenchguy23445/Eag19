package net.minecraft.core.dispenser;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.DispenserBlock;
import org.slf4j.Logger;

public class ShulkerBoxDispenseBehavior extends OptionalDispenseItemBehavior {
   private static final Logger LOGGER = LogUtils.getLogger();

   @Override
   protected ItemStack execute(BlockSource param0, ItemStack param1) {
      this.setSuccess(false);
      Item var3 = param1.getItem();
      if (var3 instanceof BlockItem) {
         Direction var4 = param0.getBlockState().getValue(DispenserBlock.FACING);
         BlockPos var5 = param0.getPos().relative(var4);
         Direction var6 = param0.getLevel().isEmptyBlock(var5.below()) ? var4 : Direction.UP;

         try {
            this.setSuccess(((BlockItem)var3).place(new DirectionalPlaceContext(param0.getLevel(), var5, var4, param1, var6)).consumesAction());
         } catch (Exception var7) {
            LOGGER.error("Error trying to place shulker box at {}", var5, var7);
         }
      }

      return param1;
   }
}

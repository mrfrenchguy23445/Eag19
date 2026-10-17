package net.minecraft.world.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class FoodOnAStickItem<T extends Entity & ItemSteerable> extends Item {
   private final EntityType<T> canInteractWith;
   private final int consumeItemDamage;

   public FoodOnAStickItem(Item.Properties param0, EntityType<T> param1, int param2) {
      super(param0);
      this.canInteractWith = param1;
      this.consumeItemDamage = param2;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      ItemStack var4 = param1.getItemInHand(param2);
      if (param0.isClientSide) {
         return InteractionResultHolder.pass(var4);
      }

      Entity var5 = param1.getControlledVehicle();
      if (param1.isPassenger() && var5 instanceof ItemSteerable var6 && var5.getType() == this.canInteractWith && var6.boost()) {
         var4.hurtAndBreak(this.consumeItemDamage, param1, l_param1 -> l_param1.broadcastBreakEvent(param2));
         if (var4.isEmpty()) {
            ItemStack var7 = new ItemStack(Items.FISHING_ROD);
            var7.setTag(var4.getTag());
            return InteractionResultHolder.success(var7);
         } else {
            return InteractionResultHolder.success(var4);
         }
      } else {
         param1.awardStat(Stats.ITEM_USED.get(this));
         return InteractionResultHolder.pass(var4);
      }
   }
}

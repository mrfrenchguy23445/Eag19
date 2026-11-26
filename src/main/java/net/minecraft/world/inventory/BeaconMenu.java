package net.minecraft.world.inventory;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class BeaconMenu extends AbstractContainerMenu {
   private static final int PAYMENT_SLOT = 0;
   private static final int SLOT_COUNT = 1;
   private static final int DATA_COUNT = 3;
   private static final int INV_SLOT_START = 1;
   private static final int INV_SLOT_END = 28;
   private static final int USE_ROW_SLOT_START = 28;
   private static final int USE_ROW_SLOT_END = 37;
   private final Container beacon = new SimpleContainer(1) {
      @Override
      public boolean canPlaceItem(int param0, ItemStack param1) {
         return param1.is(ItemTags.BEACON_PAYMENT_ITEMS);
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }
   };
   private final BeaconMenu.PaymentSlot paymentSlot;
   private final ContainerLevelAccess access;
   private final ContainerData beaconData;

   public BeaconMenu(int param0, Container param1) {
      this(param0, param1, new SimpleContainerData(3), ContainerLevelAccess.NULL);
   }

   public BeaconMenu(int param0, Container param1, ContainerData param2, ContainerLevelAccess param3) {
      super(MenuType.BEACON, param0);
      checkContainerDataCount(param2, 3);
      this.beaconData = param2;
      this.access = param3;
      this.paymentSlot = new BeaconMenu.PaymentSlot(this.beacon, 0, 136, 110);
      this.addSlot(this.paymentSlot);
      this.addDataSlots(param2);
      int var5 = 36;
      int var6 = 137;

      for (int var7 = 0; var7 < 3; var7++) {
         for (int var8 = 0; var8 < 9; var8++) {
            this.addSlot(new Slot(param1, var8 + var7 * 9 + 9, 36 + var8 * 18, 137 + var7 * 18));
         }
      }

      for (int var7 = 0; var7 < 9; var7++) {
         this.addSlot(new Slot(param1, var7, 36 + var7 * 18, 195));
      }
   }

   @Override
   public void removed(Player param0) {
      super.removed(param0);
      if (!param0.level.isClientSide) {
         ItemStack var2 = this.paymentSlot.remove(this.paymentSlot.getMaxStackSize());
         if (!var2.isEmpty()) {
            param0.drop(var2, false);
         }
      }
   }

   @Override
   public boolean stillValid(Player param0) {
      return stillValid(this.access, param0, Blocks.BEACON);
   }

   @Override
   public void setData(int param0, int param1) {
      super.setData(param0, param1);
      this.broadcastChanges();
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         var3 = var5.copy();
         if (param1 == 0) {
            if (!this.moveItemStackTo(var5, 1, 37, true)) {
               return ItemStack.EMPTY;
            }

            var4.onQuickCraft(var5, var3);
         } else if (!this.paymentSlot.hasItem() && this.paymentSlot.mayPlace(var5) && var5.getCount() == 1) {
            if (!this.moveItemStackTo(var5, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (param1 >= 1 && param1 < 28) {
            if (!this.moveItemStackTo(var5, 28, 37, false)) {
               return ItemStack.EMPTY;
            }
         } else if (param1 >= 28 && param1 < 37) {
            if (!this.moveItemStackTo(var5, 1, 28, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(var5, 1, 37, false)) {
            return ItemStack.EMPTY;
         }

         if (var5.isEmpty()) {
            var4.setByPlayer(ItemStack.EMPTY);
         } else {
            var4.setChanged();
         }

         if (var5.getCount() == var3.getCount()) {
            return ItemStack.EMPTY;
         }

         var4.onTake(param0, var5);
      }

      return var3;
   }

   public int getLevels() {
      return this.beaconData.get(0);
   }

   @Nullable
   public MobEffect getPrimaryEffect() {
      return MobEffect.byId(this.beaconData.get(1));
   }

   @Nullable
   public MobEffect getSecondaryEffect() {
      return MobEffect.byId(this.beaconData.get(2));
   }

   public void updateEffects(Optional<MobEffect> param0, Optional<MobEffect> param1) {
      if (this.paymentSlot.hasItem()) {
         this.beaconData.set(1, param0.map(MobEffect::getId).orElse(-1));
         this.beaconData.set(2, param1.map(MobEffect::getId).orElse(-1));
         this.paymentSlot.remove(1);
         this.access.execute(Level::blockEntityChanged);
      }
   }

   public boolean hasPayment() {
      return !this.beacon.getItem(0).isEmpty();
   }

   class PaymentSlot extends Slot {
      public PaymentSlot(Container param1, int param2, int param3, int param4) {
         super(param1, param2, param3, param4);
      }

      @Override
      public boolean mayPlace(ItemStack param0) {
         return param0.is(ItemTags.BEACON_PAYMENT_ITEMS);
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }
   }
}

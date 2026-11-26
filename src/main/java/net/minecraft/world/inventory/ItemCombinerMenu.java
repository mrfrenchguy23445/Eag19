package net.minecraft.world.inventory;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ItemCombinerMenu extends AbstractContainerMenu {
   private static final int INVENTORY_SLOTS_PER_ROW = 9;
   private static final int INVENTORY_SLOTS_PER_COLUMN = 3;
   protected final ContainerLevelAccess access;
   protected final Player player;
   protected final Container inputSlots;
   private final List<Integer> inputSlotIndexes;
   protected final ResultContainer resultSlots = new ResultContainer();
   private final int resultSlotIndex;

   protected abstract boolean mayPickup(Player var1, boolean var2);

   protected abstract void onTake(Player var1, ItemStack var2);

   protected abstract boolean isValidBlock(BlockState var1);

   public ItemCombinerMenu(@Nullable MenuType<?> param0, int param1, Inventory param2, ContainerLevelAccess param3) {
      super(param0, param1);
      this.access = param3;
      this.player = param2.player;
      ItemCombinerMenuSlotDefinition var5 = this.createInputSlotDefinitions();
      this.inputSlots = this.createContainer(var5.getNumOfInputSlots());
      this.inputSlotIndexes = var5.getInputSlotIndexes();
      this.resultSlotIndex = var5.getResultSlotIndex();
      this.createInputSlots(var5);
      this.createResultSlot(var5);
      this.createInventorySlots(param2);
   }

   private void createInputSlots(ItemCombinerMenuSlotDefinition param0) {
      for (final ItemCombinerMenuSlotDefinition.SlotDefinition var3 : param0.getSlots()) {
         this.addSlot(new Slot(this.inputSlots, var3.slotIndex(), var3.x(), var3.y()) {
            @Override
            public boolean mayPlace(ItemStack l_param0) {
               return var3.mayPlace().test(l_param0);
            }
         });
      }
   }

   private void createResultSlot(ItemCombinerMenuSlotDefinition param0) {
      this.addSlot(new Slot(this.resultSlots, param0.getResultSlot().slotIndex(), param0.getResultSlot().x(), param0.getResultSlot().y()) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return false;
         }

         @Override
         public boolean mayPickup(Player l_param0) {
            return ItemCombinerMenu.this.mayPickup(l_param0, this.hasItem());
         }

         @Override
         public void onTake(Player l_param0, ItemStack l_param1) {
            ItemCombinerMenu.this.onTake(l_param0, l_param1);
         }
      });
   }

   private void createInventorySlots(Inventory param0) {
      for (int var2 = 0; var2 < 3; var2++) {
         for (int var3 = 0; var3 < 9; var3++) {
            this.addSlot(new Slot(param0, var3 + var2 * 9 + 9, 8 + var3 * 18, 84 + var2 * 18));
         }
      }

      for (int var2 = 0; var2 < 9; var2++) {
         this.addSlot(new Slot(param0, var2, 8 + var2 * 18, 142));
      }
   }

   public abstract void createResult();

   protected abstract ItemCombinerMenuSlotDefinition createInputSlotDefinitions();

   private SimpleContainer createContainer(int param0) {
      return new SimpleContainer(param0) {
         @Override
         public void setChanged() {
            super.setChanged();
            ItemCombinerMenu.this.slotsChanged(this);
         }
      };
   }

   @Override
   public void slotsChanged(Container param0) {
      super.slotsChanged(param0);
      if (param0 == this.inputSlots) {
         this.createResult();
      }
   }

   @Override
   public void removed(Player param0) {
      super.removed(param0);
      this.access.execute((l_param1, l_param2) -> this.clearContainer(param0, this.inputSlots));
   }

   @Override
   public boolean stillValid(Player param0) {
      return this.access
         .evaluate(
            (l_param1, l_param2) -> !this.isValidBlock(l_param1.getBlockState(l_param2))
               ? false
               : param0.distanceToSqr(l_param2.getX() + 0.5, l_param2.getY() + 0.5, l_param2.getZ() + 0.5) <= 64.0,
            true
         );
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         var3 = var5.copy();
         int var6 = this.getInventorySlotStart();
         int var7 = this.getUseRowEnd();
         if (param1 == this.getResultSlot()) {
            if (!this.moveItemStackTo(var5, var6, var7, true)) {
               return ItemStack.EMPTY;
            }

            var4.onQuickCraft(var5, var3);
         } else if (this.inputSlotIndexes.contains(param1)) {
            if (!this.moveItemStackTo(var5, var6, var7, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.canMoveIntoInputSlots(var5) && param1 >= this.getInventorySlotStart() && param1 < this.getUseRowEnd()) {
            int var8 = this.getSlotToQuickMoveTo(var3);
            if (!this.moveItemStackTo(var5, var8, this.getResultSlot(), false)) {
               return ItemStack.EMPTY;
            }
         } else if (param1 >= this.getInventorySlotStart() && param1 < this.getInventorySlotEnd()) {
            if (!this.moveItemStackTo(var5, this.getUseRowStart(), this.getUseRowEnd(), false)) {
               return ItemStack.EMPTY;
            }
         } else if (param1 >= this.getUseRowStart()
            && param1 < this.getUseRowEnd()
            && !this.moveItemStackTo(var5, this.getInventorySlotStart(), this.getInventorySlotEnd(), false)) {
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

   protected boolean canMoveIntoInputSlots(ItemStack param0) {
      return true;
   }

   public int getSlotToQuickMoveTo(ItemStack param0) {
      return this.inputSlots.isEmpty() ? 0 : this.inputSlotIndexes.get(0);
   }

   public int getResultSlot() {
      return this.resultSlotIndex;
   }

   private int getInventorySlotStart() {
      return this.getResultSlot() + 1;
   }

   private int getInventorySlotEnd() {
      return this.getInventorySlotStart() + 27;
   }

   private int getUseRowStart() {
      return this.getInventorySlotEnd();
   }

   private int getUseRowEnd() {
      return this.getUseRowStart() + 9;
   }
}

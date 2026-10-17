package net.minecraft.world.inventory;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class CartographyTableMenu extends AbstractContainerMenu {
   public static final int MAP_SLOT = 0;
   public static final int ADDITIONAL_SLOT = 1;
   public static final int RESULT_SLOT = 2;
   private static final int INV_SLOT_START = 3;
   private static final int INV_SLOT_END = 30;
   private static final int USE_ROW_SLOT_START = 30;
   private static final int USE_ROW_SLOT_END = 39;
   private final ContainerLevelAccess access;
   long lastSoundTime;
   public final Container container = new SimpleContainer(2) {
      @Override
      public void setChanged() {
         CartographyTableMenu.this.slotsChanged(this);
         super.setChanged();
      }
   };
   private final ResultContainer resultContainer = new ResultContainer() {
      @Override
      public void setChanged() {
         CartographyTableMenu.this.slotsChanged(this);
         super.setChanged();
      }
   };

   public CartographyTableMenu(int param0, Inventory param1) {
      this(param0, param1, ContainerLevelAccess.NULL);
   }

   public CartographyTableMenu(int param0, Inventory param1, final ContainerLevelAccess param2) {
      super(MenuType.CARTOGRAPHY_TABLE, param0);
      this.access = param2;
      this.addSlot(new Slot(this.container, 0, 15, 15) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return l_param0.is(Items.FILLED_MAP);
         }
      });
      this.addSlot(new Slot(this.container, 1, 15, 52) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return l_param0.is(Items.PAPER) || l_param0.is(Items.MAP) || l_param0.is(Items.GLASS_PANE);
         }
      });
      this.addSlot(new Slot(this.resultContainer, 2, 145, 39) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return false;
         }

         @Override
         public void onTake(Player l_param0, ItemStack l_param1) {
            CartographyTableMenu.this.slots.get(0).remove(1);
            CartographyTableMenu.this.slots.get(1).remove(1);
            l_param1.getItem().onCraftedBy(l_param1, l_param0.level, l_param0);
            param2.execute((l2_param0, l2_param1) -> {
               long l2_var3 = l2_param0.getGameTime();
               if (CartographyTableMenu.this.lastSoundTime != l2_var3) {
                  l2_param0.playSound((Player)null, l2_param1, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                  CartographyTableMenu.this.lastSoundTime = l2_var3;
               }
            });
            super.onTake(l_param0, l_param1);
         }
      });

      for (int var4 = 0; var4 < 3; var4++) {
         for (int var5 = 0; var5 < 9; var5++) {
            this.addSlot(new Slot(param1, var5 + var4 * 9 + 9, 8 + var5 * 18, 84 + var4 * 18));
         }
      }

      for (int var4 = 0; var4 < 9; var4++) {
         this.addSlot(new Slot(param1, var4, 8 + var4 * 18, 142));
      }
   }

   @Override
   public boolean stillValid(Player param0) {
      return stillValid(this.access, param0, Blocks.CARTOGRAPHY_TABLE);
   }

   @Override
   public void slotsChanged(Container param0) {
      ItemStack var2 = this.container.getItem(0);
      ItemStack var3 = this.container.getItem(1);
      ItemStack var4 = this.resultContainer.getItem(2);
      if (var4.isEmpty() || !var2.isEmpty() && !var3.isEmpty()) {
         if (!var2.isEmpty() && !var3.isEmpty()) {
            this.setupResultSlot(var2, var3, var4);
         }
      } else {
         this.resultContainer.removeItemNoUpdate(2);
      }
   }

   private void setupResultSlot(ItemStack param0, ItemStack param1, ItemStack param2) {
      this.access.execute((l_param3, l_param4) -> {
         MapItemSavedData l_var6 = MapItem.getSavedData(param0, l_param3);
         if (l_var6 != null) {
            ItemStack l_var7;
            if (param1.is(Items.PAPER) && !l_var6.locked && l_var6.scale < 4) {
               l_var7 = param0.copy();
               l_var7.setCount(1);
               l_var7.getOrCreateTag().putInt("map_scale_direction", 1);
               this.broadcastChanges();
            } else if (param1.is(Items.GLASS_PANE) && !l_var6.locked) {
               l_var7 = param0.copy();
               l_var7.setCount(1);
               l_var7.getOrCreateTag().putBoolean("map_to_lock", true);
               this.broadcastChanges();
            } else {
               if (!param1.is(Items.MAP)) {
                  this.resultContainer.removeItemNoUpdate(2);
                  this.broadcastChanges();
                  return;
               }

               l_var7 = param0.copy();
               l_var7.setCount(2);
               this.broadcastChanges();
            }

            if (!ItemStack.matches(l_var7, param2)) {
               this.resultContainer.setItem(2, l_var7);
               this.broadcastChanges();
            }
         }
      });
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack param0, Slot param1) {
      return param1.container != this.resultContainer && super.canTakeItemForPickAll(param0, param1);
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         var3 = var5.copy();
         if (param1 == 2) {
            var5.getItem().onCraftedBy(var5, param0.level, param0);
            if (!this.moveItemStackTo(var5, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            var4.onQuickCraft(var5, var3);
         } else if (param1 != 1 && param1 != 0) {
            if (var5.is(Items.FILLED_MAP)) {
               if (!this.moveItemStackTo(var5, 0, 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!var5.is(Items.PAPER) && !var5.is(Items.MAP) && !var5.is(Items.GLASS_PANE)) {
               if (param1 >= 3 && param1 < 30) {
                  if (!this.moveItemStackTo(var5, 30, 39, false)) {
                     return ItemStack.EMPTY;
                  }
               } else if (param1 >= 30 && param1 < 39 && !this.moveItemStackTo(var5, 3, 30, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(var5, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(var5, 3, 39, false)) {
            return ItemStack.EMPTY;
         }

         if (var5.isEmpty()) {
            var4.setByPlayer(ItemStack.EMPTY);
         }

         var4.setChanged();
         if (var5.getCount() == var3.getCount()) {
            return ItemStack.EMPTY;
         }

         var4.onTake(param0, var5);
         this.broadcastChanges();
      }

      return var3;
   }

   @Override
   public void removed(Player param0) {
      super.removed(param0);
      this.resultContainer.removeItemNoUpdate(2);
      this.access.execute((l_param1, l_param2) -> this.clearContainer(param0, this.container));
   }
}

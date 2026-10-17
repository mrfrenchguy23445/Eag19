package net.minecraft.world.inventory;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;

public class BrewingStandMenu extends AbstractContainerMenu {
   private static final int BOTTLE_SLOT_START = 0;
   private static final int BOTTLE_SLOT_END = 2;
   private static final int INGREDIENT_SLOT = 3;
   private static final int FUEL_SLOT = 4;
   private static final int SLOT_COUNT = 5;
   private static final int DATA_COUNT = 2;
   private static final int INV_SLOT_START = 5;
   private static final int INV_SLOT_END = 32;
   private static final int USE_ROW_SLOT_START = 32;
   private static final int USE_ROW_SLOT_END = 41;
   private final Container brewingStand;
   private final ContainerData brewingStandData;
   private final Slot ingredientSlot;

   public BrewingStandMenu(int param0, Inventory param1) {
      this(param0, param1, new SimpleContainer(5), new SimpleContainerData(2));
   }

   public BrewingStandMenu(int param0, Inventory param1, Container param2, ContainerData param3) {
      super(MenuType.BREWING_STAND, param0);
      checkContainerSize(param2, 5);
      checkContainerDataCount(param3, 2);
      this.brewingStand = param2;
      this.brewingStandData = param3;
      this.addSlot(new BrewingStandMenu.PotionSlot(param2, 0, 56, 51));
      this.addSlot(new BrewingStandMenu.PotionSlot(param2, 1, 79, 58));
      this.addSlot(new BrewingStandMenu.PotionSlot(param2, 2, 102, 51));
      this.ingredientSlot = this.addSlot(new BrewingStandMenu.IngredientsSlot(param2, 3, 79, 17));
      this.addSlot(new BrewingStandMenu.FuelSlot(param2, 4, 17, 17));
      this.addDataSlots(param3);

      for (int var5 = 0; var5 < 3; var5++) {
         for (int var6 = 0; var6 < 9; var6++) {
            this.addSlot(new Slot(param1, var6 + var5 * 9 + 9, 8 + var6 * 18, 84 + var5 * 18));
         }
      }

      for (int var5 = 0; var5 < 9; var5++) {
         this.addSlot(new Slot(param1, var5, 8 + var5 * 18, 142));
      }
   }

   @Override
   public boolean stillValid(Player param0) {
      return this.brewingStand.stillValid(param0);
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         var3 = var5.copy();
         if ((param1 < 0 || param1 > 2) && param1 != 3 && param1 != 4) {
            if (BrewingStandMenu.FuelSlot.mayPlaceItem(var3)) {
               if (this.moveItemStackTo(var5, 4, 5, false) || this.ingredientSlot.mayPlace(var5) && !this.moveItemStackTo(var5, 3, 4, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (this.ingredientSlot.mayPlace(var5)) {
               if (!this.moveItemStackTo(var5, 3, 4, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (BrewingStandMenu.PotionSlot.mayPlaceItem(var3) && var3.getCount() == 1) {
               if (!this.moveItemStackTo(var5, 0, 3, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (param1 >= 5 && param1 < 32) {
               if (!this.moveItemStackTo(var5, 32, 41, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (param1 >= 32 && param1 < 41) {
               if (!this.moveItemStackTo(var5, 5, 32, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(var5, 5, 41, false)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (!this.moveItemStackTo(var5, 5, 41, true)) {
               return ItemStack.EMPTY;
            }

            var4.onQuickCraft(var5, var3);
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

   public int getFuel() {
      return this.brewingStandData.get(1);
   }

   public int getBrewingTicks() {
      return this.brewingStandData.get(0);
   }

   static class FuelSlot extends Slot {
      public FuelSlot(Container param0, int param1, int param2, int param3) {
         super(param0, param1, param2, param3);
      }

      @Override
      public boolean mayPlace(ItemStack param0) {
         return mayPlaceItem(param0);
      }

      public static boolean mayPlaceItem(ItemStack param0) {
         return param0.is(Items.BLAZE_POWDER);
      }

      @Override
      public int getMaxStackSize() {
         return 64;
      }
   }

   static class IngredientsSlot extends Slot {
      public IngredientsSlot(Container param0, int param1, int param2, int param3) {
         super(param0, param1, param2, param3);
      }

      @Override
      public boolean mayPlace(ItemStack param0) {
         return PotionBrewing.isIngredient(param0);
      }

      @Override
      public int getMaxStackSize() {
         return 64;
      }
   }

   static class PotionSlot extends Slot {
      public PotionSlot(Container param0, int param1, int param2, int param3) {
         super(param0, param1, param2, param3);
      }

      @Override
      public boolean mayPlace(ItemStack param0) {
         return mayPlaceItem(param0);
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }

      @Override
      public void onTake(Player param0, ItemStack param1) {
         Potion var3 = PotionUtils.getPotion(param1);
         if (param0 instanceof ServerPlayer) {
            CriteriaTriggers.BREWED_POTION.trigger((ServerPlayer)param0, var3);
         }

         super.onTake(param0, param1);
      }

      public static boolean mayPlaceItem(ItemStack param0) {
         return param0.is(Items.POTION) || param0.is(Items.SPLASH_POTION) || param0.is(Items.LINGERING_POTION) || param0.is(Items.GLASS_BOTTLE);
      }
   }
}

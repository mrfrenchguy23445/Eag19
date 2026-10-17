package net.minecraft.recipebook;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.slf4j.Logger;

public class ServerPlaceRecipe<C extends Container> implements PlaceRecipe<Integer> {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final StackedContents stackedContents = new StackedContents();
   protected Inventory inventory;
   protected RecipeBookMenu<C> menu;

   public ServerPlaceRecipe(RecipeBookMenu<C> param0) {
      this.menu = param0;
   }

   public void recipeClicked(ServerPlayer param0, @Nullable Recipe<C> param1, boolean param2) {
      if (param1 != null && param0.getRecipeBook().contains(param1)) {
         this.inventory = param0.getInventory();
         if (this.testClearGrid() || param0.isCreative()) {
            this.stackedContents.clear();
            param0.getInventory().fillStackedContents(this.stackedContents);
            this.menu.fillCraftSlotsStackedContents(this.stackedContents);
            if (this.stackedContents.canCraft(param1, null)) {
               this.handleRecipeClicked(param1, param2);
            } else {
               this.clearGrid();
               param0.connection.send(new ClientboundPlaceGhostRecipePacket(param0.containerMenu.containerId, param1));
            }

            param0.getInventory().setChanged();
         }
      }
   }

   protected void clearGrid() {
      for (int var1 = 0; var1 < this.menu.getSize(); var1++) {
         if (this.menu.shouldMoveToInventory(var1)) {
            ItemStack var2 = this.menu.getSlot(var1).getItem().copy();
            this.inventory.placeItemBackInInventory(var2, false);
            this.menu.getSlot(var1).set(var2);
         }
      }

      this.menu.clearCraftingContent();
   }

   protected void handleRecipeClicked(Recipe<C> param0, boolean param1) {
      boolean var3 = this.menu.recipeMatches(param0);
      int var4 = this.stackedContents.getBiggestCraftableStack(param0, null);
      if (var3) {
         for (int var5 = 0; var5 < this.menu.getGridHeight() * this.menu.getGridWidth() + 1; var5++) {
            if (var5 != this.menu.getResultSlotIndex()) {
               ItemStack var6 = this.menu.getSlot(var5).getItem();
               if (!var6.isEmpty() && Math.min(var4, var6.getMaxStackSize()) < var6.getCount() + 1) {
                  return;
               }
            }
         }
      }

      int var5 = this.getStackSize(param1, var4, var3);
      IntList var6 = new IntArrayList();
      if (this.stackedContents.canCraft(param0, var6, var5)) {
         int var7 = var5;

         for (int var9 : var6) {
            int var10 = StackedContents.fromStackingIndex(var9).getMaxStackSize();
            if (var10 < var7) {
               var7 = var10;
            }
         }

         var5 = var7;
         if (this.stackedContents.canCraft(param0, var6, var5)) {
            this.clearGrid();
            this.placeRecipe(this.menu.getGridWidth(), this.menu.getGridHeight(), this.menu.getResultSlotIndex(), param0, var6.iterator(), var5);
         }
      }
   }

   @Override
   public void addItemToSlot(Iterator<Integer> param0, int param1, int param2, int param3, int param4) {
      Slot var6 = this.menu.getSlot(param1);
      ItemStack var7 = StackedContents.fromStackingIndex(param0.next());
      if (!var7.isEmpty()) {
         for (int var8 = 0; var8 < param2; var8++) {
            this.moveItemToGrid(var6, var7);
         }
      }
   }

   protected int getStackSize(boolean param0, int param1, boolean param2) {
      int var4 = 1;
      if (param0) {
         var4 = param1;
      } else if (param2) {
         var4 = 64;

         for (int var5 = 0; var5 < this.menu.getGridWidth() * this.menu.getGridHeight() + 1; var5++) {
            if (var5 != this.menu.getResultSlotIndex()) {
               ItemStack var6 = this.menu.getSlot(var5).getItem();
               if (!var6.isEmpty() && var4 > var6.getCount()) {
                  var4 = var6.getCount();
               }
            }
         }

         if (var4 < 64) {
            var4++;
         }
      }

      return var4;
   }

   protected void moveItemToGrid(Slot param0, ItemStack param1) {
      int var3 = this.inventory.findSlotMatchingUnusedItem(param1);
      if (var3 != -1) {
         ItemStack var4 = this.inventory.getItem(var3).copy();
         if (!var4.isEmpty()) {
            if (var4.getCount() > 1) {
               this.inventory.removeItem(var3, 1);
            } else {
               this.inventory.removeItemNoUpdate(var3);
            }

            var4.setCount(1);
            if (param0.getItem().isEmpty()) {
               param0.set(var4);
            } else {
               param0.getItem().grow(1);
            }
         }
      }
   }

   private boolean testClearGrid() {
      List<ItemStack> var1 = Lists.newArrayList();
      int var2 = this.getAmountOfFreeSlotsInInventory();

      for (int var3 = 0; var3 < this.menu.getGridWidth() * this.menu.getGridHeight() + 1; var3++) {
         if (var3 != this.menu.getResultSlotIndex()) {
            ItemStack var4 = this.menu.getSlot(var3).getItem().copy();
            if (!var4.isEmpty()) {
               int var5 = this.inventory.getSlotWithRemainingSpace(var4);
               if (var5 == -1 && var1.size() <= var2) {
                  for (ItemStack var7 : var1) {
                     if (var7.sameItem(var4) && var7.getCount() != var7.getMaxStackSize() && var7.getCount() + var4.getCount() <= var7.getMaxStackSize()) {
                        var7.grow(var4.getCount());
                        var4.setCount(0);
                        break;
                     }
                  }

                  if (!var4.isEmpty()) {
                     if (var1.size() >= var2) {
                        return false;
                     }

                     var1.add(var4);
                  }
               } else if (var5 == -1) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private int getAmountOfFreeSlotsInInventory() {
      int var1 = 0;

      for (ItemStack var3 : this.inventory.items) {
         if (var3.isEmpty()) {
            var1++;
         }
      }

      return var1;
   }
}

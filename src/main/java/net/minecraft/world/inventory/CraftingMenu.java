package net.minecraft.world.inventory;

import java.util.Optional;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class CraftingMenu extends RecipeBookMenu<CraftingContainer> {
   public static final int RESULT_SLOT = 0;
   private static final int CRAFT_SLOT_START = 1;
   private static final int CRAFT_SLOT_END = 10;
   private static final int INV_SLOT_START = 10;
   private static final int INV_SLOT_END = 37;
   private static final int USE_ROW_SLOT_START = 37;
   private static final int USE_ROW_SLOT_END = 46;
   private final CraftingContainer craftSlots = new CraftingContainer(this, 3, 3);
   private final ResultContainer resultSlots = new ResultContainer();
   private final ContainerLevelAccess access;
   private final Player player;

   public CraftingMenu(int param0, Inventory param1) {
      this(param0, param1, ContainerLevelAccess.NULL);
   }

   public CraftingMenu(int param0, Inventory param1, ContainerLevelAccess param2) {
      super(MenuType.CRAFTING, param0);
      this.access = param2;
      this.player = param1.player;
      this.addSlot(new ResultSlot(param1.player, this.craftSlots, this.resultSlots, 0, 124, 35));

      for (int var4 = 0; var4 < 3; var4++) {
         for (int var5 = 0; var5 < 3; var5++) {
            this.addSlot(new Slot(this.craftSlots, var5 + var4 * 3, 30 + var5 * 18, 17 + var4 * 18));
         }
      }

      for (int var4 = 0; var4 < 3; var4++) {
         for (int var5 = 0; var5 < 9; var5++) {
            this.addSlot(new Slot(param1, var5 + var4 * 9 + 9, 8 + var5 * 18, 84 + var4 * 18));
         }
      }

      for (int var4 = 0; var4 < 9; var4++) {
         this.addSlot(new Slot(param1, var4, 8 + var4 * 18, 142));
      }
   }

   protected static void slotChangedCraftingGrid(AbstractContainerMenu param0, Level param1, Player param2, CraftingContainer param3, ResultContainer param4) {
      if (!param1.isClientSide) {
         ServerPlayer var5 = (ServerPlayer)param2;
         ItemStack var6 = ItemStack.EMPTY;
         Optional<CraftingRecipe> var7 = param1.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, param3, param1);
         if (var7.isPresent()) {
            CraftingRecipe var8 = var7.get();
            if (param4.setRecipeUsed(param1, var5, var8)) {
               ItemStack var9 = var8.assemble(param3, param1.registryAccess());
               if (var9.isItemEnabled(param1.enabledFeatures())) {
                  var6 = var9;
               }
            }
         }

         param4.setItem(0, var6);
         param0.setRemoteSlot(0, var6);
         var5.connection.send(new ClientboundContainerSetSlotPacket(param0.containerId, param0.incrementStateId(), 0, var6));
      }
   }

   @Override
   public void slotsChanged(Container param0) {
      this.access.execute((l_param0, l_param1) -> slotChangedCraftingGrid(this, l_param0, this.player, this.craftSlots, this.resultSlots));
   }

   @Override
   public void fillCraftSlotsStackedContents(StackedContents param0) {
      this.craftSlots.fillStackedContents(param0);
   }

   @Override
   public void clearCraftingContent() {
      this.craftSlots.clearContent();
      this.resultSlots.clearContent();
   }

   @Override
   public boolean recipeMatches(Recipe<? super CraftingContainer> param0) {
      return param0.matches(this.craftSlots, this.player.level);
   }

   @Override
   public void removed(Player param0) {
      super.removed(param0);
      this.access.execute((l_param1, l_param2) -> this.clearContainer(param0, this.craftSlots));
   }

   @Override
   public boolean stillValid(Player param0) {
      return stillValid(this.access, param0, Blocks.CRAFTING_TABLE);
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         var3 = var5.copy();
         if (param1 == 0) {
            this.access.execute((l_param2, l_param3) -> var5.getItem().onCraftedBy(var5, l_param2, param0));
            if (!this.moveItemStackTo(var5, 10, 46, true)) {
               return ItemStack.EMPTY;
            }

            var4.onQuickCraft(var5, var3);
         } else if (param1 >= 10 && param1 < 46) {
            if (!this.moveItemStackTo(var5, 1, 10, false)) {
               if (param1 < 37) {
                  if (!this.moveItemStackTo(var5, 37, 46, false)) {
                     return ItemStack.EMPTY;
                  }
               } else if (!this.moveItemStackTo(var5, 10, 37, false)) {
                  return ItemStack.EMPTY;
               }
            }
         } else if (!this.moveItemStackTo(var5, 10, 46, false)) {
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
         if (param1 == 0) {
            param0.drop(var5, false);
         }
      }

      return var3;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack param0, Slot param1) {
      return param1.container != this.resultSlots && super.canTakeItemForPickAll(param0, param1);
   }

   @Override
   public int getResultSlotIndex() {
      return 0;
   }

   @Override
   public int getGridWidth() {
      return this.craftSlots.getWidth();
   }

   @Override
   public int getGridHeight() {
      return this.craftSlots.getHeight();
   }

   @Override
   public int getSize() {
      return 10;
   }

   @Override
   public RecipeBookType getRecipeBookType() {
      return RecipeBookType.CRAFTING;
   }

   @Override
   public boolean shouldMoveToInventory(int param0) {
      return param0 != this.getResultSlotIndex();
   }
}
